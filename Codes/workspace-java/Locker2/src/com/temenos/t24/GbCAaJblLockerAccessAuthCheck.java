package com.temenos.t24;

import java.util.List;

import com.temenos.api.TStructure;
import com.temenos.api.exceptions.T24CoreException;
import com.temenos.t24.api.complex.eb.templatehook.TransactionContext;
import com.temenos.t24.api.hook.system.RecordLifecycle;
import com.temenos.t24.api.records.aajbllockeraccess.AaJblLockerAccessRecord;
import com.temenos.t24.api.system.Session;

/**
 * TODO: Document me!
 *
 * @author kawsar
 *
 */
public class GbCAaJblLockerAccessAuthCheck extends RecordLifecycle {

    @Override
    public void defaultFieldValues(String application, String currentRecordId, TStructure currentRecord,
            TStructure unauthorisedRecord, TStructure liveRecord, TransactionContext transactionContext) {

        AaJblLockerAccessRecord lockerAccessRec = new AaJblLockerAccessRecord(currentRecord);
        Session ss = new Session(this);

        String inputterUserId = ss.getUserId();
        List<String> curRecInputterList = lockerAccessRec.getInputter();

        if (curRecInputterList != null && !curRecInputterList.isEmpty()) {
            String lastInputter = curRecInputterList.get(curRecInputterList.size() - 1);
            String inputter = lastInputter.split("\\_")[1];
            
            if (curRecInputterList != null &&
                    inputter.equalsIgnoreCase(inputterUserId)) {
                throw new T24CoreException("EB.ERROR","AA-LOCKER-SAME-INPUTT");
            }
        }
    }
}