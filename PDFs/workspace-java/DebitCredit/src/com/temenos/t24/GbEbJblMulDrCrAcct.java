package com.temenos.t24;

import com.temenos.api.TStructure;
import com.temenos.t24.api.complex.eb.templatehook.TransactionContext;
import com.temenos.t24.api.hook.system.RecordLifecycle;
import com.temenos.t24.api.records.account.AccountRecord;
import com.temenos.t24.api.records.ebjblmultipledrcr.EbJblMultipleDrCrRecord;
import com.temenos.t24.api.system.DataAccess;

/**
 * TODO: Document me!
 *
 * @author kawsar
 *
 */
public class GbEbJblMulDrCrAcct extends RecordLifecycle{

    @Override
    public void defaultFieldValues(String application, String currentRecordId, TStructure currentRecord,
            TStructure unauthorisedRecord, TStructure liveRecord, TransactionContext transactionContext) {
        
        String debitAcc = null;
        String accTitle = null;
        String workingBalance = null;
        DataAccess da = new DataAccess(this);
        
        EbJblMultipleDrCrRecord mulDrCrRec = new EbJblMultipleDrCrRecord(currentRecord);
       
        debitAcc = mulDrCrRec.getDebitAccount(0).getDebitAccount().getValue();
        
        AccountRecord accRec = new AccountRecord(da.getRecord("ACCOUNT", debitAcc));
        accTitle = accRec.getShortTitle(0).getValue();
        workingBalance = accRec.getWorkingBalance().getValue();
        
        
        
    }
    
    

}
