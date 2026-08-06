package com.temenos.t24;

import java.util.List;

import com.temenos.api.TStructure;
import com.temenos.t24.api.complex.eb.servicehook.TransactionData;
import com.temenos.t24.api.complex.eb.templatehook.TransactionContext;
import com.temenos.t24.api.hook.system.RecordLifecycle;
import com.temenos.t24.api.records.ebaajbllockeracct.EbAaJblLockerAcctRecord;
import com.temenos.t24.api.records.ebaajbllockerdetails.EbAaJblLockerDetailsRecord;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.tables.ebaajbllockerdetails.EbAaJblLockerDetailsTable;

/**
 * ---------------------------------------------------------------------
 * Class Name : GbEbAaJblLockerDetStat
 * ATTACHED TO: EB.AA.JBL.LOCKER.ACCT,INPUT
 * ---------------------------------------------------------------------
 * FUNCTIONALITY :
 *   After authorisation of EB.AA.JBL.LOCKER.ACCT, update the corresponding
 *   EB.AA.JBL.LOCKER.DETAILS record to reflect correct locker usage.
 *
 * Business Logic :
 *   1. If account status becomes ACTIVE / FREEZE / DECEASED / UNCLAIMED
 *      and locker is FREE → mark locker as USED and attach account no.
 *
 *   2. If account status becomes CLOSED → release the locker:
 *        - DETAILS->STATUS     = FREE
 *        - DETAILS->LOCKER.ACCT = NULL
 *
 *ROUTINE TYPE: AUTH.RTN
 *
 * Author : Kawsar
 * ---------------------------------------------------------------------
 */
public class GbEbAaJblLockerDetStat extends RecordLifecycle{

    @Override
    public void postUpdateRequest(String application, String currentRecordId, TStructure currentRecord,
            List<TransactionData> transactionData, List<TStructure> currentRecords,
            TransactionContext transactionContext) {
        EbAaJblLockerAcctRecord currentAccRec = null;
        EbAaJblLockerDetailsTable detailsTable = null;
        EbAaJblLockerDetailsRecord detailsRec = null;
        DataAccess da = new DataAccess(this);
        String lockerId = "";
        String newStatus = "";

        
        currentAccRec = new EbAaJblLockerAcctRecord(currentRecord);
        lockerId  = currentAccRec.getLockerId().getValue();
        newStatus = currentAccRec.getStatus().getValue(); 
        
        if (lockerId == null || newStatus == null) {
            return;
        }
        try {
            /* ---------------------------------------------------------
             * Read Locker Details record
             * --------------------------------------------------------- */
            detailsTable = new EbAaJblLockerDetailsTable(this);

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

  