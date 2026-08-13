package com.temenos.t24;

import java.util.ArrayList;
import java.util.List;

import com.temenos.api.TField;
import com.temenos.api.TStructure;
import com.temenos.api.exceptions.T24CoreException;
import com.temenos.t24.api.complex.eb.templatehook.TransactionContext;
import com.temenos.t24.api.hook.system.RecordLifecycle;
import com.temenos.t24.api.records.aajbllockeracct.AaJblLockerAcctRecord;
import com.temenos.t24.api.records.account.AccountRecord;
import com.temenos.t24.api.records.postingrestrict.PostingRestrictRecord;
import com.temenos.t24.api.system.DataAccess;

/**
 * TODO: Document me!
 *
 * @author kawsar
 *
 */
public class GbCAaJblLockerAcctPR extends RecordLifecycle{

    @Override
    public void defaultFieldValues(String application, String currentRecordId, TStructure currentRecord,
            TStructure unauthorisedRecord, TStructure liveRecord, TransactionContext transactionContext) {
        AaJblLockerAcctRecord locAcctRec = new AaJblLockerAcctRecord(currentRecord);
        DataAccess da = new DataAccess(this);
        AccountRecord accRec = null;
        String acctId = currentRecordId.split("\\.")[0];
        List<TField> prList = new ArrayList<>();
        if(locAcctRec != null ){
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
