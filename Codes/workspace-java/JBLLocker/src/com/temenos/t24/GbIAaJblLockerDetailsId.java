package com.temenos.t24;

import java.util.List;

import com.temenos.api.exceptions.T24CoreException;
import com.temenos.t24.api.complex.eb.templatehook.TransactionContext;
import com.temenos.t24.api.hook.system.RecordLifecycle;
import com.temenos.t24.api.records.aaarrangement.AaArrangementRecord;
import com.temenos.t24.api.records.account.AccountRecord;
import com.temenos.t24.api.system.DataAccess;

/**
 * TODO: Document me!
 *
 * @author kawsar
 *
 */
public class GbIAaJblLockerDetailsId extends RecordLifecycle{

    @Override
    public String checkId(String currentRecordId, TransactionContext transactionContext) {
        // TODO Auto-generated method stub
        DataAccess da = new DataAccess(this);
        AccountRecord accRec = null;
        AaArrangementRecord arrRec = null;

        String accountNo = "";
        String newLockerAcctId = "";
        String group = "";
        String product = "";
        String arrangementId = "";
        int lastSeq = 0;
        int nextSeq = 0;

        List<String> eligibleProducts = null;
        List<String> existingLockersLive = null;
        List<String> existingLockersUnauth = null;
        List<String> existingLockersHistory = null;
        
        /* =====================================================
         * CASE 1 : FULL LOCKER ACCOUNT ID ENTERED
         * ===================================================== */
        if (currentRecordId.contains(".")) {

            try {
                //check LIVE file
                da.getRecord("AA.JBL.LOCKER.ACCT", currentRecordId);
                return currentRecordId; 
            } catch (Exception e1) {
                try {
                    //check UNAUTH file
                    da.getRecord("AA.JBL.LOCKER.ACCT$NAU", currentRecordId);
                    return currentRecordId; 
                } catch (Exception e2) {
                    try {
                        // check HISTORY file
                        da.getRecord("AA.JBL.LOCKER.ACCT$HIS", currentRecordId);
                        return currentRecordId;
                    } catch (Exception e3) {
                        throw new T24CoreException("", "Locker ID does not exist");
                    }
                }
            }
        }
        /* =====================================================
         * CASE 2 : ACCOUNT NUMBER ENTERED
         * ===================================================== */
        accountNo = currentRecordId;

        //1. Validate account number 
        try {
            accRec = new AccountRecord(da.getRecord("ACCOUNT", accountNo));
        } catch (Exception e) {
            throw new T24CoreException("", "Invalid Account Number");
        }

        //2. Read arrangement & product
        arrangementId = accRec.getArrangementId().getValue();
        try {
            arrRec = new AaArrangementRecord(
                    da.getRecord("AA.ARRANGEMENT", arrangementId));
        } catch (Exception e) {
            throw new T24CoreException("", "Arrangement not found for Account Number");
        }

        group = arrRec.getProductGroup().getValue();
        product = arrRec.getProduct(0).getProduct().getValue();
        
      //3. Check locker-eligible products
        eligibleProducts = da.selectRecords(
                "BNK",
                "AA.JBL.LOCKER.PRODUCT",
                "",
                "WITH GROUP EQ " + group + " OR PRODUCT EQ " + product
        );

        if (eligibleProducts == null || eligibleProducts.isEmpty()) {
            throw new T24CoreException("", "Account type must be SB / CD / SND");
        }
        
      //4. Check LIVE lockers
        try {
            existingLockersLive = da.selectRecords(
                    "BNK",
                    "AA.JBL.LOCKER.ACCT",
                    "",
                    "WITH @ID LIKE '" + accountNo + "...'"
            );

            for (String id : existingLockersLive) {
                int seq = Integer.parseInt(id.split("\\.")[1]);
                if (seq > lastSeq) lastSeq = seq;
            }
        } catch (Exception e) {
            // none found
        }
        
     // 5. Check UNAUTH lockers 
        try {
            existingLockersUnauth = da.selectRecords(
                    "BNK",
                    "AA.JBL.LOCKER.ACCT$NAU",
                    "",
                    "WITH @ID LIKE '" + accountNo + "...'"
            );

            for (String id : existingLockersUnauth) {
                int seq = Integer.parseInt(id.split("\\.")[1]);
                if (seq > lastSeq) lastSeq = seq;
            }
        } catch (Exception e) {
            
        }
      //6. Check HISTORY lockers 
        try {
            existingLockersHistory = da.selectRecords(
                    "BNK",
                    "AA.JBL.LOCKER.ACCT$HIS",
                    "",
                    "WITH @ID LIKE '" + accountNo + "...'"
            );

            for (String id : existingLockersHistory) {
             // Remove history version suffix (;1, ;2, etc.)
                String cleanId = id.split(";")[0];
             // Extract numeric sequence after dot
                int seq = Integer.parseInt(cleanId.split("\\.")[1]);
                if (seq > lastSeq) lastSeq = seq;
            }
        } catch (Exception e) {
           
        }
      //7. Generate next sequence 
        nextSeq = lastSeq + 1;
        String seqFormatted = String.format("%04d", nextSeq);
        newLockerAcctId = accountNo + "." + seqFormatted;

        return newLockerAcctId;   
    }
    

}
