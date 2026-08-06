package com.temenos.t24;

import com.temenos.api.TStructure;
import com.temenos.t24.api.complex.eb.templatehook.TransactionContext;
import com.temenos.t24.api.hook.system.RecordLifecycle;
import com.temenos.t24.api.records.ebjblcashfeeding.EbJblCashFeedingRecord;
import com.temenos.t24.api.records.teller.Account1Class;
import com.temenos.t24.api.records.teller.TellerRecord;
import com.temenos.t24.api.system.DataAccess;

/**
 * TODO: Document me!
 *VERSION ATTACHED: 
 *TELLER,JBL.CASHWDL.FEED.ADJ.FCY
 * @author kawsar
 *
 */
public class GbCCrCfAbjAmt extends RecordLifecycle{

    @Override
    public void defaultFieldValues(String application, String currentRecordId, TStructure currentRecord,
            TStructure unauthorisedRecord, TStructure liveRecord, TransactionContext transactionContext) {
        
        TellerRecord tellRec = new TellerRecord(currentRecord);
        EbJblCashFeedingRecord cfRec = null;
        DataAccess da = new DataAccess(this);
        String thRef = "";
        String adjAmt = "";
        if(tellRec!=null){
            try{
                thRef = tellRec.getTheirReference().getValue();
            }catch(Exception e){}
        }
        if(!thRef.isEmpty()){
            try{
                cfRec = new EbJblCashFeedingRecord(da.getRecord("EB.JBL.CASH.FEEDING", thRef));
            }catch(Exception e){}
        }
        if(cfRec!=null){
            try{
                adjAmt = cfRec.getAdjustmentAmount().getValue();
            }catch(Exception e){}
        }
        if(!adjAmt.isEmpty()){
            
            Account1Class acctClass = new Account1Class();
            acctClass.setAmountFcy1(adjAmt);
            tellRec.setAccount1(acctClass, 0);
        }
        currentRecord.set(tellRec.toStructure());
    }


}
