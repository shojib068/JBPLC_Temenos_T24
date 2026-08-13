package com.temenos.t24;

import com.temenos.api.TStructure;
import com.temenos.t24.api.complex.eb.templatehook.TransactionContext;
import com.temenos.t24.api.records.account.AccountRecord;
import com.temenos.t24.api.records.customer.CustomerRecord;
import com.temenos.t24.api.records.ebaajbllockeracct.EbAaJblLockerAcctRecord;
import com.temenos.t24.api.hook.system.RecordLifecycle;
import com.temenos.t24.api.system.DataAccess;

public class GbAaJblLockerAcct extends RecordLifecycle {

    @Override
    public void defaultFieldValues(String application, String currentRecordId, TStructure currentRecord,
                                   TStructure unauthorisedRecord, TStructure liveRecord,
                                   TransactionContext transactionContext) {

        try {

            EbAaJblLockerAcctRecord lockerRec = new EbAaJblLockerAcctRecord(currentRecord);
            DataAccess da = new DataAccess(this);

            // get account number from record
           
            String[] parts = currentRecordId.split("\\.");
            String accountNo = parts[0];
            

            AccountRecord accRec = new AccountRecord(da.getRecord("ACCOUNT", accountNo));
            String customerId = accRec.getCustomer().getValue();
            CustomerRecord cusRec = new CustomerRecord(da.getRecord("CUSTOMER", customerId));
            String customerName = cusRec.getShortName().get(0).getValue();

  
            // fill the field
            
            lockerRec.getAcctNo().setValue(accountNo);
            lockerRec.getAcctName().setValue(customerName);
            lockerRec.getCustomer().setValue(customerId);
 
            
            currentRecord.set(lockerRec.toStructure());

        } catch (Exception e) {
            throw e;
            
           
        }
    }
}
