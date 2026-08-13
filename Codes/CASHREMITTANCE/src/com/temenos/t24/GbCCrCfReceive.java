package com.temenos.t24;

import com.temenos.api.TStructure;
import com.temenos.api.exceptions.T24CoreException;
import com.temenos.t24.api.complex.eb.templatehook.TransactionContext;
import com.temenos.t24.api.hook.system.RecordLifecycle;
import com.temenos.t24.api.records.ebjblcashfeeding.EbJblCashFeedingRecord;
import com.temenos.t24.api.system.DataAccess;

/**
 * TODO: Document me!
 * VERSION ATTACHED:
 * EB.JBL.CASH.FEEDING,FEEDING.DECLINE
 * EB.JBL.CASH.FEEDING,FEEDING.DECLINE.FCY   
 * @author kawsar
 *
 */
public class GbCCrCfReceive extends RecordLifecycle{

    @Override
    public void defaultFieldValues(String application, String currentRecordId, TStructure currentRecord,
            TStructure unauthorisedRecord, TStructure liveRecord, TransactionContext transactionContext) {
        DataAccess da = new DataAccess(this);
        EbJblCashFeedingRecord cfRec = null;
        try{
            cfRec = new EbJblCashFeedingRecord(da.getRecord("EB.JBL.CASH.FEEDING$NAU", currentRecordId));
        }catch(Exception e){}
        String status = "";
        if(cfRec!=null){
            try{
                status = cfRec.getStatus().getValue();
            }catch(Exception e){}
        }
        if(!status.isEmpty() && "DECLINE".equalsIgnoreCase(status))
            throw new T24CoreException("EB.ERROR", "AA-CR-CF-DECLINE");      
    }
    

}
