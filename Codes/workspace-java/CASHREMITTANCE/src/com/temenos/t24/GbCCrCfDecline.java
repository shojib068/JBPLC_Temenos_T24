package com.temenos.t24;

import com.temenos.api.TStructure;
import com.temenos.api.exceptions.T24CoreException;
import com.temenos.t24.api.complex.eb.templatehook.TransactionContext;
import com.temenos.t24.api.hook.system.RecordLifecycle;
import com.temenos.t24.api.records.ebjblcashfeeding.EbJblCashFeedingRecord;
import com.temenos.t24.api.records.teller.TellerRecord;
import com.temenos.t24.api.system.DataAccess;

/**
 * TODO: Document me!
 * VERSION ATTACHED: TELLER,JBL.CASHWDL.ACPT
 *  TELLER,JBL.CASHWDL.ACPT.FCY 
 *  TELLER,JBL.CASHWDL.RECV
 *  TELLER,JBL.CASHWDL.RECV.FCY
 * @author kawsar
 *
 */
public class GbCCrCfDecline extends RecordLifecycle{
    

    @Override
    public void defaultFieldValues(String application, String currentRecordId, TStructure currentRecord,
            TStructure unauthorisedRecord, TStructure liveRecord, TransactionContext transactionContext) {
        DataAccess da = new DataAccess(this);
        TellerRecord tellerRec = null;
        EbJblCashFeedingRecord cfRec = null;
        String thRef = "";
        String status = "";
        try{
            tellerRec = new TellerRecord(currentRecord);
        }catch(Exception e){}
        if(tellerRec!=null){
            try{
                thRef = tellerRec.getTheirReference().getValue();
            }catch(Exception e){}
        }
        if(!thRef.isEmpty()){
            try{
                cfRec = new EbJblCashFeedingRecord(da.getRecord("EB.JBL.CASH.FEEDING$NAU", thRef));
            }catch(Exception e){}
        }
        if(cfRec!=null){
            try{
                status = cfRec.getStatus().getValue();
            }catch(Exception e){}
        }
        if(!status.isEmpty() && "DECLINE".equalsIgnoreCase(status)){
            throw new T24CoreException("EB.ERROR", "AA-CR-CF-DECLINE");
        }     
    }
}
