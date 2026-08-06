package com.temenos.t24;

import java.util.List;

import com.temenos.api.TStructure;
import com.temenos.api.TValidationResponse;
import com.temenos.t24.api.complex.eb.templatehook.TransactionContext;
import com.temenos.t24.api.hook.system.RecordLifecycle;
import com.temenos.t24.api.records.ebaajbllockeracct.EbAaJblLockerAcctRecord;
import com.temenos.t24.api.records.ebaajbllockerdetails.EbAaJblLockerDetailsRecord;
import com.temenos.t24.api.system.DataAccess;

/**
 * ATTACHED TO: EB.AA.JBL.LOCKER.ACCT,INPUT
 * FUNCTIONALITY:
 * 1. Locker.ID cannot be empty
 * 2. Must be a valid authorized locker.id matching EB.AA.JBL.LOCKER.DETAILS,INPUT
 * 3. If a locker.id is assigned to someone and unauth, that locker cannot be assigned to anyone
 * 4. If a locker.id is already in used status, that cannot be assigned 
 *
 *
 *TYPE: INPUT.RTN
 * @author kawsar
 *
 */
public class GbEbAaJblLockerAcctLockId extends RecordLifecycle {

    @Override
    public TValidationResponse validateRecord(
            String application,
            String currentRecordId,
            TStructure currentRecord,
            TStructure unauthorisedRecord,
            TStructure liveRecord,
            TransactionContext transactionContext) {

        DataAccess da = new DataAccess(this);

        EbAaJblLockerAcctRecord lockerAccRec =
                new EbAaJblLockerAcctRecord(currentRecord);

        String lockerId = lockerAccRec.getLockerId().getValue();

        /* -------------------------------------------------
         * Locker ID cannot be empty
         * ------------------------------------------------- */
        if (lockerId == null || lockerId.trim().isEmpty()) {
            lockerAccRec.getLockerId()
                    .setError("Locker ID cannot be empty");
            return lockerAccRec.getValidationResponse();
        }

        /* -------------------------------------------------
         * Locker ID must exist in LOCKER DETAILS
         * ------------------------------------------------- */
        EbAaJblLockerDetailsRecord locDetRecord = null;
        try {
            locDetRecord = new EbAaJblLockerDetailsRecord(
                    da.getRecord("EB.AA.JBL.LOCKER.DETAILS", lockerId));
        } catch (Exception e) {
            lockerAccRec.getLockerId().setError(
                    "Invalid Locker ID"
            );
            return lockerAccRec.getValidationResponse();
        }

        /* -------------------------------------------------
         * Check UNAUTH file (NAU)
         * ------------------------------------------------- */
        try {
            List<String> nauIds = da.selectRecords(
                    "",
                    "EB.AA.JBL.LOCKER.ACCT$NAU",
                    "",
                    "WITH LOCKER.ID EQ " + lockerId
            );

           
            if (nauIds != null) {
                for (String id : nauIds) {
                    if (!id.equals(currentRecordId)) {
                        lockerAccRec.getLockerId().setError(
                                "This locker is already taken and unauth"
                        );
                        return lockerAccRec.getValidationResponse();
                    }
                }
            }

        } catch (Exception e) {
           
        }

        /* -------------------------------------------------
         * Check LIVE locker usage
         * ------------------------------------------------- */
        String lockerStatus = locDetRecord.getStatus().getValue();
        String lockerAcc    = locDetRecord.getLockerAcct().getValue();

        if ("USED".equalsIgnoreCase(lockerStatus)
                && lockerAcc != null
                && !lockerAcc.equals(currentRecordId)) {

            lockerAccRec.getLockerId().setError(
                    "Cannot allocate locker. Locker " + lockerId + " is already used."
            );
            return lockerAccRec.getValidationResponse();
        }

return null;
    }
}

