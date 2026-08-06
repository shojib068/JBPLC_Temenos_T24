package com.temenos.t24;

import java.util.List;

import com.temenos.api.TStructure;
import com.temenos.t24.api.complex.eb.templatehook.TransactionContext;
import com.temenos.t24.api.complex.eb.templatehook.TransactionData;
import com.temenos.t24.api.hook.system.RecordLifecycle;

/**
 * TODO: Document me!
 *
 * @author kawsar
 *
 */
public class TEST extends RecordLifecycle{

    @Override
    public void updateRecord(String application, String currentRecordId, TStructure currentRecord,
            TStructure unauthorisedRecord, TStructure liveRecord, TransactionContext transactionContext,
            List<TransactionData> transactionData, List<TStructure> currentRecords) {
        // TODO Auto-generated method stub
        super.updateRecord(application, currentRecordId, currentRecord, unauthorisedRecord, liveRecord, transactionContext,
                transactionData, currentRecords);
    }

}
