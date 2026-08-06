package com.temenos.t24;

import java.util.ArrayList;
import java.util.List;

import com.temenos.api.TField;
import com.temenos.api.TStructure;
import com.temenos.api.TValidationResponse;
import com.temenos.api.exceptions.T24CoreException;
import com.temenos.t24.api.complex.eb.templatehook.TransactionContext;
import com.temenos.t24.api.hook.system.RecordLifecycle;
import com.temenos.t24.api.records.aajbllockeraccess.AaJblLockerAccessRecord;
import com.temenos.t24.api.records.aajbllockeracct.AaJblLockerAcctRecord;
import com.temenos.t24.api.records.account.AccountRecord;
import com.temenos.t24.api.system.DataAccess;

/**
 * Routine: Input.Routine
 * Vesrion Attached to: 
 * Business Logic:
 * 
 * This hook validates locker access requests based on the status of the locker.
 * 
 * Steps:
 * 1. Retrieve the Locker ID from the current locker access record.
 * 2. Fetch the corresponding locker account record (AA.JBL.LOCKER.ACCT).
 * 3. Check the status of the locker.
 * 4. If the locker status is not "ACTIVE", an override is triggered.
 *    - The user will be prompted with a message:
 *      "Locker is in <STATUS>. Override needed"
 * 5. If the locker is ACTIVE, the transaction proceeds normally without override.
 * 
 * Purpose:
 * - Give override message when the locker is in !ACTIVE status. 
 * 
 * @author kawsar
 */
public class GbInAaJblLockerAccessOverride extends RecordLifecycle {

    @Override
    public TValidationResponse validateRecord(String application, String currentRecordId, TStructure currentRecord,
            TStructure unauthorisedRecord, TStructure liveRecord, TransactionContext transactionContext) {

        AaJblLockerAccessRecord locAccessRec = new AaJblLockerAccessRecord(currentRecord);
        AaJblLockerAcctRecord locAcctRec = null;
        DataAccess da = new DataAccess(this);
        AccountRecord accRec = null;
        String lockerId = "";
        String status = "";
        List<TField> prList = new ArrayList<>();
//        --------------------------------------------------------------------------------------------------------
        if(locAccessRec!= null){
            try{
                lockerId = locAccessRec.getLockerId().getValue();
            }catch(Exception e){}
        }
        if(!lockerId.isEmpty()){
            try{
                locAcctRec = new AaJblLockerAcctRecord(da.getRecord("AA.JBL.LOCKER.ACCT", lockerId));
            }catch(Exception e){}
            String acctId = lockerId.split("\\.")[0];
            try{
                accRec = new AccountRecord(da.getRecord("ACCOUNT", acctId));
            }catch(Exception e){}
        }
        if(locAcctRec!=null){
            try{
                status = locAcctRec.getStatus().getValue();
            }catch(Exception e){}
        }
        if(!status.isEmpty()){
            if(!"ACTIVE".equalsIgnoreCase(status)){
                locAccessRec.getLockerId().setOverride("Locker is in "+status+ ". Override needed");
            }
        }
//        --------------------------------------------------------------------------------------------------------------
        if(accRec != null){
            try{
                prList = accRec.getPostingRestrict();
            }catch(Exception e){}
        }
        if(prList != null ){
            for(int i = 0; i< prList.size(); i++){
                String pr = prList.get(i).getValue();
                if(pr.equals("12")){
                    throw new T24CoreException("","AA-LOCKER-ACCOUNT-DECEASED");               
                }
                if(pr.equals("15")){
                    throw new T24CoreException("","AA-LOCKER-ACCOUNT-FROZEN");
                }
            }
        }
        
        return locAccessRec.getValidationResponse();
    }
}