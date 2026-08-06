package com.temenos.t24;

import com.temenos.api.TStructure;
import com.temenos.t24.api.complex.eb.templatehook.TransactionContext;
import com.temenos.t24.api.hook.system.RecordLifecycle;
import com.temenos.t24.api.records.account.AccountRecord;
import com.temenos.t24.api.records.customer.CustomerRecord;
import com.temenos.t24.api.records.ebaajbllockeracct.EbAaJblLockerAcctRecord;
import com.temenos.t24.api.system.DataAccess;



/**
 * TODO:
 * ATTACHED TO: EB.AA.JBL.LOCKER.ACCT,INPUT -> locker opening 
 * 
 * FUNCTIONALITY:
 After validation, the system automatically:
 *      • Extracts the Account Number from the record ID
 *      • Fetches the corresponding Account record
 *      • Retrieves the linked Customer ID from the Account
 *      • Fetches the Customer record
 *      • Retrieves the Customer Short Name
 *      • Auto-populates the following non-input fields:
 *          - Account Number
 *          - Account Name (Customer Short Name)
 *          - Customer ID
 *          
 * TYPE: DEFAULT.ROUTINE
 * @kawsar
 * 
 * 
 *
 */

public class GbAaJblLockerAcct extends RecordLifecycle {

    @Override
    public void defaultFieldValues(String application, String currentRecordId, TStructure currentRecord,
                                   TStructure unauthorisedRecord, TStructure liveRecord,
                                   TransactionContext transactionContext) {

        try {

            EbAaJblLockerAcctRecord lockerRec = new EbAaJblLockerAcctRecord(currentRecord);
            DataAccess da = new DataAccess(this);
            AccountRecord accRec = null;
            CustomerRecord cusRec = null;
            
            // ----------------------------------------------------
            // Step 1: Extract Account Number from Record ID
            // Example Record ID: 123456.0001
            // Account Number   : 123456
            // ----------------------------------------------------
           
            String[] parts = currentRecordId.split("\\.");
            String accountNo = parts[0];
            String customerId = "";
            String customerName = "";
            
            // ----------------------------------------------------
            // Step 2: Read Account record using Account Number
            // ----------------------------------------------------
            accRec = new AccountRecord(da.getRecord("ACCOUNT", accountNo));
            customerId = accRec.getCustomer().getValue();
            
            // ----------------------------------------------------
            // Step 3: Read Customer record using Customer ID
            // ----------------------------------------------------
            
            cusRec = new CustomerRecord(da.getRecord("CUSTOMER", customerId));
            customerName = cusRec.getShortName().get(0).getValue();

            
            // ----------------------------------------------------
            // Step 4: Auto-populate non-input fields
            // ----------------------------------------------------
            
                lockerRec.setAcctNo(accountNo); // Account Number
                lockerRec.setAcctName(customerName); // Account Name
                lockerRec.setCustomer(customerId); // Customer ID
                
                
                // Write updated values back to the current record
                
                currentRecord.set(lockerRec.toStructure());

        } catch (Exception e) {
            throw e;
            
           
        }
    }
}
