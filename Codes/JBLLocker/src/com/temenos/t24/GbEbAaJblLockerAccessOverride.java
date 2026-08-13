package com.temenos.t24;

import java.util.List;

import com.temenos.api.TStructure;
import com.temenos.t24.api.complex.eb.servicehook.TransactionData;
import com.temenos.t24.api.complex.eb.templatehook.TransactionContext;
import com.temenos.t24.api.hook.system.RecordLifecycle;
import com.temenos.t24.api.records.ebaajbllockeraccess.EbAaJblLockerAccessRecord;
import com.temenos.t24.api.records.ebaajbllockeracct.EbAaJblLockerAcctRecord;
import com.temenos.t24.api.system.DataAccess;

/**
 * TODO: Document me!
 *
 * @author kawsar
 *
 */
public class GbEbAaJblLockerAccessOverride extends RecordLifecycle{

    @Override
    public void postUpdateRequest(String application, String currentRecordId, TStructure currentRecord,
            List<TransactionData> transactionData, List<TStructure> currentRecords,
            TransactionContext transactionContext) {
        DataAccess da = new DataAccess(this);
        EbAaJblLockerAccessRecord rec =
                new EbAaJblLockerAccessRecord(currentRecord);

        String lockerId = rec.getLockerId().getValue();
        String status   = "";

        try {
            EbAaJblLockerAcctRecord accRec =
                    new EbAaJblLockerAcctRecord(
                            da.getRecord("EB.AA.JBL.LOCKER.ACCT", lockerId));

            status = accRec.getStatus().getValue();
        } catch (Exception e) {
            return;
        }

        if (status != null && !"ACTIVE".equalsIgnoreCase(status)) {
            transactionContext.setOverride(
                    "Locker access is not allowed for status "
                            + status + ". Override accepted."
            );
        }
    }
    }


