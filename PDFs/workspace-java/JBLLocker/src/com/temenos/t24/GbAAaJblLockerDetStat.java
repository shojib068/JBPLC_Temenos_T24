package com.temenos.t24;

import java.util.List;

import com.temenos.api.TStructure;
import com.temenos.t24.api.complex.eb.servicehook.TransactionData;
import com.temenos.t24.api.complex.eb.templatehook.TransactionContext;
import com.temenos.t24.api.hook.system.RecordLifecycle;
import com.temenos.t24.api.records.aajbllockeracct.AaJblLockerAcctRecord;
import com.temenos.t24.api.records.aajbllockerdetails.AaJblLockerDetailsRecord;
import com.temenos.t24.api.tables.aajbllockerdetails.AaJblLockerDetailsTable;
/**
 * TODO: Document me!
 *
 * @author kawsar
 *
 */
public class GbAAaJblLockerDetStat extends RecordLifecycle{

    @Override
    public void postUpdateRequest(String application, String currentRecordId, TStructure currentRecord,
            List<TransactionData> transactionData, List<TStructure> currentRecords,
            TransactionContext transactionContext) {
        AaJblLockerAcctRecord currentAccRec = null;
        AaJblLockerDetailsTable detailsTable = null;
        AaJblLockerDetailsRecord detailsRec = null;
        String lockerId = "";
        String newStatus = ""; 
        
        currentAccRec = new AaJblLockerAcctRecord(currentRecord);
        lockerId  = currentAccRec.getLockerId().getValue();
        newStatus = currentAccRec.getStatus().getValue();
        
        if (lockerId == null || newStatus == null) {
            return;
        }
        try {
            /* ---------------------------------------------------------
             * Read Locker Details record
             * --------------------------------------------------------- */
            detailsTable = new AaJblLockerDetailsTable(this);

            detailsRec = detailsTable.read(lockerId);

            /* ---------------------------------------------------------
             * CASE 1 : Account CLOSED → Release the locker
             * --------------------------------------------------------- */
            if ("CLOSED".equalsIgnoreCase(newStatus)) {

                detailsRec.setStatus("Free");        // Locker available
                detailsRec.setLockerAcct("");        // Remove account link

                detailsTable.write(lockerId, detailsRec);
                return;
            }
            
            /* ---------------------------------------------------------
             * CASE 2 : Account ACTIVE / FREEZE / DECEASED / UNCLAIMED
             *          → Mark locker as USED
             * --------------------------------------------------------- */
            if ("FREE".equalsIgnoreCase(detailsRec.getStatus().getValue())
                && detailsRec.getLockerAcct().getValue().isEmpty()
                && (newStatus.equalsIgnoreCase("Active")
                 || newStatus.equalsIgnoreCase("Freeze")
                 || newStatus.equalsIgnoreCase("Deceased")
                 || newStatus.equalsIgnoreCase("Unclaimed"))) {

                detailsRec.setStatus("Used");                 // Locker occupied
                detailsRec.setLockerAcct(currentRecordId);    // Link account

                detailsTable.write(lockerId, detailsRec);
            }

        } catch (Exception e) {
          
        }
            
        }
    }
