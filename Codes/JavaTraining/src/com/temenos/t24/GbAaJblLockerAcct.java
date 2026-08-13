package com.temenos.t24;

import com.temenos.api.TStructure;
import com.temenos.t24.api.complex.eb.templatehook.TransactionContext;
import com.temenos.t24.api.records.account.AccountRecord;
import com.temenos.t24.api.records.customer.CustomerRecord;
import com.temenos.t24.api.records.ebaajbllockeracct.EbAaJblLockerAcctRecord;
import com.temenos.t24.api.hook.system.RecordLifecycle;
import com.temenos.t24.api.system.DataAccess;

/**
 * TODO: after validation, it will auto populate the Account Number, Account Name and Customer ID field
 *
 * @kawsar ahmed shojib
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
            
            // get account number from record
           
            String[] parts = currentRecordId.split("\\.");
            String accountNo = parts[0];
            String customerId = "";
            String customerName = "";
            
            
            accRec = new AccountRecord(da.getRecord("ACCOUNT", accountNo));
            customerId = accRec.getCustomer().getValue();
            cusRec = new CustomerRecord(da.getRecord("CUSTOMER", customerId));
            customerName = cusRec.getShortName().get(0).getValue();

            
          
                lockerRec.setAcctNo(accountNo);
                lockerRec.setAcctName(customerName);
                lockerRec.setCustomer(customerId);
                currentRecord.set(lockerRec.toStructure());

        } catch (Exception e) {
            throw e;
            
           
        }
    }
}
