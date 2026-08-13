package com.temenos.t24;

import com.temenos.api.exceptions.T24CoreException;
import com.temenos.t24.api.complex.eb.templatehook.TransactionContext;
import com.temenos.t24.api.hook.system.RecordLifecycle;
import com.temenos.t24.api.system.DataAccess;

/**
 * VERSION: EB.AA.JBL.LOCKER.ACCT,CLOSE -> to close the locker account
 * the input must be valid locker.acct from EB.AA.JBL.LOCK.ACCT
 *
 * @author kawsar
 *
 */
public class GbEbAaJblLockerAcctClose extends RecordLifecycle {

    @Override
    public String checkId(String currentRecordId, TransactionContext transactionContext) {

        DataAccess da = new DataAccess(this);


        if (!currentRecordId.contains(".")) {
            throw new T24CoreException("", "Invalid Locker Account ID format");
        }

        try {
            // check if the locker id is in EB.AA.JBL.LOCKER.ACCT
            da.getRecord("EB.AA.JBL.LOCKER.ACCT", currentRecordId);

            return currentRecordId;

        } catch (Exception e) {
            // Record does not exist
            throw new T24CoreException("", "Locker account does not exist");
        }
    }
}
