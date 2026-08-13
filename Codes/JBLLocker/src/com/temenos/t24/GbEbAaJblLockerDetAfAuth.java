package com.temenos.t24;

import java.util.List;

import com.temenos.api.TStructure;
import com.temenos.t24.api.complex.eb.servicehook.TransactionData;
import com.temenos.t24.api.complex.eb.templatehook.TransactionContext;
import com.temenos.t24.api.hook.system.RecordLifecycle;
import com.temenos.t24.api.records.ebaajbllockerdetails.EbAaJblLockerDetailsRecord;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.tables.ebaajbllockerdetails.EbAaJblLockerDetailsTable;

/**
 * TODO: Document me!
 *
 * @author kawsar
 *
 */
public class GbEbAaJblLockerDetAfAuth extends RecordLifecycle{

    @Override
    public void postUpdateRequest(String application, String currentRecordId, TStructure currentRecord,
            List<TransactionData> transactionData, List<TStructure> currentRecords,
            TransactionContext transactionContext) {
        DataAccess da = new DataAccess(this);
        String locId = currentRecordId;
        EbAaJblLockerDetailsRecord locDetRec = new EbAaJblLockerDetailsRecord(currentRecord);
        EbAaJblLockerDetailsTable locDetTable = new EbAaJblLockerDetailsTable(locDetTable.de);
       
        
        
    }

}
