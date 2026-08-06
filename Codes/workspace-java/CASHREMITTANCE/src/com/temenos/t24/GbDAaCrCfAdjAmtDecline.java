package com.temenos.t24;

import com.temenos.api.TStructure;
import com.temenos.t24.api.complex.eb.templatehook.TransactionContext;
import com.temenos.t24.api.hook.system.RecordLifecycle;
import com.temenos.t24.api.records.ebjblcashfeeding.EbJblCashFeedingRecord;
/**
 * TODO: Document me!
 * VERSION ATTACHED: 
 * @author kawsar
 *
 */
public class GbDAaCrCfAdjAmtDecline extends RecordLifecycle{

    @Override
    public void defaultFieldValues(String application, String currentRecordId, TStructure currentRecord,
            TStructure unauthorisedRecord, TStructure liveRecord, TransactionContext transactionContext) {
        EbJblCashFeedingRecord cfRec = new EbJblCashFeedingRecord(currentRecord);
        if(cfRec!=null){
          String approvedAmt =  cfRec.getApprovedAmount().getValue();
          cfRec.setAdjustmentAmount(approvedAmt);
        }
        currentRecord.set(cfRec.toStructure());       
    }

}
