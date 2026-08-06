package com.temenos.t24;

import java.util.ArrayList;
import java.util.List;

import com.temenos.api.TField;
import com.temenos.api.TStructure;
import com.temenos.api.exceptions.T24CoreException;
import com.temenos.t24.api.complex.eb.templatehook.TransactionContext;
import com.temenos.t24.api.hook.system.RecordLifecycle;
import com.temenos.t24.api.records.aajbllockeraccess.AaJblLockerAccessRecord;
import com.temenos.t24.api.records.account.AccountRecord;
import com.temenos.t24.api.system.DataAccess;

/**
 * TODO: Document me!
 *
 * @author kawsar
 *
 */
public class GbCAaJblLockerAccessPR extends RecordLifecycle{

    @Override
    public void defaultFieldValues(String application, String currentRecordId, TStructure currentRecord,
            TStructure unauthorisedRecord, TStructure liveRecord, TransactionContext transactionContext) {
        AaJblLockerAccessRecord locAccessRec = new AaJblLockerAccessRecord(currentRecord);
        AccountRecord accRec = null;
        DataAccess da = new DataAccess(this);
        List<TField> prList = new ArrayList<>();
        
        String lockerAcct = "";
        
        if(locAccessRec != null){
            try{
                lockerAcct = locAccessRec.getLockerId().getValue();
            }catch(Exception e){}
        }
        if(lockerAcct != null && !lockerAcct.isEmpty()){
            String acctId = lockerAcct.split("\\.")[0];
            try{
                accRec = new AccountRecord(da.getRecord("ACCOUNT", acctId)); 
            }catch(Exception e){}
        }
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
    }

}
