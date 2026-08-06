package com.temenos.t24;

import com.temenos.api.exceptions.T24CoreException;
import com.temenos.t24.api.complex.eb.templatehook.TransactionContext;
import com.temenos.t24.api.hook.system.RecordLifecycle;
import com.temenos.t24.api.system.DataAccess;

/**
 * Routine: ID.RTN
 * Version Attached to: AA.JBL.LOCKER.DETAILS,ADMIN
 *
 * Business Logic:
 * =====================================================
 * CASE: Validate Locker Detail ID Format
 * =====================================================
 *
 * - The input ID is expected in the format:
 *     <CoCode>.<CabinetNo>.<LockerSize>.<Serial>
 *     Example: 1234.101.L.0001
 *
 * =====================================================
 * Step 1: Check Empty Input
 * =====================================================
 *
 * - If the input ID is null or empty:
 *     → Raise error: "ID cannot be empty"
 *
 * =====================================================
 * Step 2: Split ID into Components
 * =====================================================
 *
 * - Split the ID using dot (.)
 * - Expected number of parts = 4
 *
 * - If parts count is not 4:
 *     → Raise error: "ID must be in format xxxx.yyy.z.aaaa"
 *
 * =====================================================
 * Step 3: Extract Components
 * =====================================================
 *
 * - coCode     → First part  (Branch Code - last 4 digits)
 * - cabinetNo  → Second part (Cabinet Number)
 * - lockerSize → Third part  (Locker Size)
 * - serial     → Fourth part (Locker Serial Number)
 *
 * =====================================================
 * Step 4: Validate Company Code (coCode)
 * =====================================================
 *
 * - Must be exactly 4 characters
 *
 * - If length is not 4:
 *     → Raise error: "Please Enter the last four Digit of the Branch Code"
 *
 * =====================================================
 * Step 5: Validate Cabinet Number
 * =====================================================
 *
 * - Must contain only numeric digits
 *
 * - If not numeric:
 *     → Raise error: "Cabinet Number must be numeric"
 *
 * =====================================================
 * Step 6: Validate Locker Size
 * =====================================================
 *
 * - Allowed values:
 *     → L (Large)
 *     → M (Medium)
 *     → S1 (Small Type 1)
 *     → S2 (Small Type 2)
 *
 * - If value is not one of the above:
 *     → Raise error: "Locker size must be L, M, S1, or S2"
 *
 * =====================================================
 * Step 7: Validate Serial Number
 * =====================================================
 *
 * - Must contain only numeric digits
 *
 * - If not numeric:
 *     → Raise error: "Locker serial must be numeric"
 *
 * =====================================================
 * Step 8: Return Valid ID
 * =====================================================
 *
 * - If all validations pass:
 *     → Return the same ID
 *
 * =====================================================
 * NOTE (Commented Logic)
 * =====================================================
 *
 * - There is a provision (currently commented out) to:
 *     → Fetch user's company ID using Session
 *     → Extract last 4 digits
 *     → Restrict operation to Head Office (e.g., 9999)
 *
 * - This logic is not active in current implementation
 *
 * @author Kawsar
 */
public class GbIAaJblLockerDetIdCheck extends RecordLifecycle {

    @Override
    public String checkId(String currentRecordId, TransactionContext transactionContext) {

        DataAccess da = new DataAccess(this);

        if (currentRecordId == null || currentRecordId.trim().isEmpty()) {
            throw new T24CoreException("", "AA-LOCKER-ID");
        }

        String id = currentRecordId.trim();
        String[] parts = id.split("\\.");

        // Format check
        if (parts.length != 4) {
            throw new T24CoreException("", "AA-LOCKER-DET-ID");
        }

        String coCode = parts[0];
        String cabinetNo = parts[1];
        String lockerSize = parts[2];
        String serial = parts[3];

        // Field validations
        if (coCode.length() != 4 ||
            cabinetNo.length() != 3 ||
            serial.length() != 4 ||
            !serial.matches("\\d+")) {

            throw new T24CoreException("", "AA-LOCKER-ID");
        }

        if (!lockerSize.equals("L")
                && !lockerSize.equals("M")
                && !lockerSize.equals("S1")
                && !lockerSize.equals("S2")) {

            throw new T24CoreException("", "AA-LOCKER-DET-ID-SIZE");
        }

        // Record exists?
        if (recordExists(da, "AA.JBL.LOCKER.DETAILS", id)
                || recordExists(da, "AA.JBL.LOCKER.DETAILS$NAU", id)
                || recordExists(da, "AA.JBL.LOCKER.DETAILS$HIS", id)) {

            return id;
        }

        return id;
    }

    private boolean recordExists(DataAccess da, String app, String id) {
        try {
            da.getRecord(app, id);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}