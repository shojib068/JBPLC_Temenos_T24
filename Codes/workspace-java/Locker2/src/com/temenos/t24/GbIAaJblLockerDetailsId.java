package com.temenos.t24;

import java.util.ArrayList;
import java.util.List;

import com.temenos.api.exceptions.T24CoreException;
import com.temenos.t24.api.complex.eb.templatehook.TransactionContext;
import com.temenos.t24.api.hook.system.RecordLifecycle;
import com.temenos.t24.api.records.aaarrangement.AaArrangementRecord;
import com.temenos.t24.api.records.aaarrangement.ProductClass;
import com.temenos.t24.api.records.account.AccountRecord;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.system.Session;

/**
 * Routine: ID.Routine
 * Version Attached to: AA.JBL.LOCKER.ACCT,INPUT
 * Business Logic:  
 * =====================================================
 * CASE 1: Full Locker Account ID is provided
 * =====================================================
 * 
 * - If the input ID contains a dot (.), it is treated as a complete Locker ID
 *   (e.g., 123456.0001).
 * 
 * - The system validates whether the ID exists in:
 *     1. LIVE file (AA.JBL.LOCKER.ACCT)
 *     2. UNAUTH file (AA.JBL.LOCKER.ACCT$NAU)
 *     3. HISTORY file (AA.JBL.LOCKER.ACCT$HIS)
 * 
 * - If found in any of the above:
 *     → The same ID is accepted and returned.
 * 
 * - If not found:
 *     → An error is raised: "Locker ID does not exist"
 * =====================================================
 * CASE 2: Only Account Number is provided
 * =====================================================
 * 
 * - The input is treated as an Account Number (e.g., 123456).
 * 
 * Step 1: Validate Account
 *   - Fetch ACCOUNT record using Account Number
 *   - If not found → throw error "Invalid Account Number"
 * Step 2: Fetch Arrangement Details
 *   - Retrieve Arrangement ID from ACCOUNT
 *   - Fetch AA.ARRANGEMENT record
 *   - Extract Product Group and Product
 *   - If not found → throw error "Arrangement not found for Account Number"
 * Step 3: Check Locker Eligibility
 *   - Validate if the account belongs to allowed products/groups
 *     using AA.JBL.LOCKER.PRODUCT table
 *   - If not eligible → throw error "Account type must be SB / CD / SND"
 * 
 * Step 4: Find Existing Locker Accounts (LIVE)
 *   - Search AA.JBL.LOCKER.ACCT for existing IDs matching account number
 *   - Extract numeric sequence (after dot) and track the highest value
 * 
 * Step 5: Check UNAUTH Records
 *   - Search AA.JBL.LOCKER.ACCT$NAU
 *   - Update highest sequence if found
 * 
 * Step 6: Check HISTORY Records
 *   - Search AA.JBL.LOCKER.ACCT$HIS
 *   - Remove version suffix (e.g., ;1, ;2)
 *   - Extract sequence and update highest value
 * 
 * Step 7: Generate New Locker Account ID
 *   - Next sequence = highest sequence + 1
 *   - Format sequence as 4-digit number (e.g., 0001, 0002)
 *   - Construct ID as: <AccountNo>.<Sequence>
 *     Example: 123456.0003
 * @author kawsar
 */
public class GbIAaJblLockerDetailsId extends RecordLifecycle{

    @Override
    public String checkId(String currentRecordId, TransactionContext transactionContext) {
  
        DataAccess da = new DataAccess(this);
        AccountRecord accRec = null;
        AaArrangementRecord arrRec = null;
        Session ss = new Session(this);
        
        String coCode = ss.getCompanyId();
        String accountNo = "";
        String newLockerAcctId = "";
        String group = "";
        String product = "";
        String arrangementId = "";
        int lastSeq = 0;
        int nextSeq = 0;
        
        List<ProductClass> productList = new ArrayList<>();
        List<String> arrProduct = new ArrayList<>();;
        List<String> eligibleProducts = new ArrayList<>();
        List<String> existingLockersLive = new ArrayList<>();
        List<String> existingLockersUnauth = new ArrayList<>();
        List<String> existingLockersHistory = new ArrayList<>();
        
        
        /* =====================================================
         * CASE 1 : FULL LOCKER ACCOUNT ID ENTERED
         * ===================================================== */
        
        if(currentRecordId.trim().contains(".")){
            try{
                da.getRecord("AA.JBL.LOCKER.ACCT", currentRecordId);
                return currentRecordId;
            }catch(Exception e1){
                try{
                    da.getRecord("AA.JBL.LOCKER.ACCT$NAU", currentRecordId);
                    return currentRecordId;
                }catch(Exception e2){
                    try{
                        da.getRecord("AA.JBL.LOCKER.ACCT$HIS", currentRecordId);
                        return currentRecordId;
                    }catch(Exception e3){
                        throw new T24CoreException("", "AA-LOCKER-ID");
                    }
                }

            }
        }
        
        /* =====================================================
         * CASE 2 : ACCOUNT NUMBER ENTERED
         * ===================================================== */
        accountNo = currentRecordId.trim();
        
        try{
            accRec = new AccountRecord(
                    da.getRecord("ACCOUNT", accountNo));
        }catch(Exception e){}
        
        if(accRec == null){
            throw new T24CoreException("", "AA-LOCKER-ID");
        }else{
            try{
                arrangementId = accRec.getArrangementId().getValue();
            }catch(Exception e){}
        }
        
        if(arrangementId!= null && !arrangementId.isEmpty()){
            try{
                arrRec = new AaArrangementRecord(
                        da.getRecord("AA.ARRANGEMENT", arrangementId));
            }catch(Exception e){}
        }
        
        if(arrRec == null){
            throw new T24CoreException("", "AA-ACCOUNT.NOT.AN.ARR");
        }else{
            try{
                group = arrRec.getProductGroup().getValue();
            }catch(Exception e){}
            try{
                productList = arrRec.getProduct();
            }catch(Exception e){}
        }
        if( productList != null ){
            for(int i=0; i<productList.size(); i++){
                try{
                    product = productList.get(i).getProduct().getValue(); 
                }catch(Exception e){} 
                
                if(product!= null && !product.isEmpty())
                    arrProduct.add(product);
            }
        }
        product = String.join(" ", arrProduct);
        
      //3. Check locker-eligible products

        String selectStmt = " WITH CO.CODE EQ "+coCode+" AND ( GROUP EQ "+group+
                " OR PRODUCT EQ "+product+" )";
        eligibleProducts = da.selectRecords("BNK", "AA.JBL.LOCKER.PRODUCT", "",selectStmt);
        
        
        if( eligibleProducts == null || eligibleProducts.isEmpty()){
            throw new T24CoreException("", "AA-LOCKER-ID-PRODUCT-TYPE");
        }
        
        
//        4. Generate id sequence
        
//        Check LIVE lockers
        try {
            existingLockersLive = da.selectRecords(
                    "BNK",
                    "AA.JBL.LOCKER.ACCT",
                    "",
                    "WITH @ID LIKE '" + accountNo + "...'"
            );
        }catch(Exception e){}
        if(existingLockersLive!= null){
            for (String id : existingLockersLive) {
                int seq = Integer.parseInt(id.split("\\.")[1]);
                if (seq > lastSeq) lastSeq = seq;
            }

        }
        
//        Check UNAUTH lockers
        try {
            existingLockersUnauth = da.selectRecords(
                    "BNK",
                    "AA.JBL.LOCKER.ACCT$NAU",
                    "",
                    "WITH @ID LIKE '" + accountNo + "...'"
            );
        }catch(Exception e){}
        if(existingLockersUnauth != null){
            for (String id : existingLockersUnauth) {
                int seq = Integer.parseInt(id.split("\\.")[1]);
                if (seq > lastSeq) lastSeq = seq;
            }

        }
        
//        Check HISTORY lockers 
        try {
            existingLockersHistory = da.selectRecords(
                    "BNK",
                    "AA.JBL.LOCKER.ACCT$HIS",
                    "",
                    "WITH @ID LIKE '" + accountNo + "...'"
            );
        }catch (Exception e) {}

        if(existingLockersHistory!= null){
            for (String id : existingLockersHistory) {
                String cleanId = id.split(";")[0];
                int seq = Integer.parseInt(cleanId.split("\\.")[1]);
                if (seq > lastSeq) lastSeq = seq;
            }   
        }

        nextSeq = lastSeq + 1;
        String seqFormatted = String.format("%04d", nextSeq);
        newLockerAcctId = accountNo + "." + seqFormatted;

        return newLockerAcctId;
    }
}
