package com.temenos.t24;

import com.temenos.api.TStructure;
import com.temenos.api.TValidationResponse;
import com.temenos.t24.api.complex.eb.templatehook.TransactionContext;
import com.temenos.t24.api.hook.system.RecordLifecycle;
import com.temenos.t24.api.records.ebaajbllockerdetails.EbAaJblLockerDetailsRecord;

/**
 * TODO: if the status = free -> locker.acct will be empty
 * if the status is used -> cannot modify the locker account
 *
 * @author kawsar
 *
 */
public class GbEbAaJblLockerDetAcct extends RecordLifecycle{

    @Override
    public TValidationResponse validateRecord(String application, String currentRecordId, TStructure currentRecord,
            TStructure unauthorisedRecord, TStructure liveRecord, TransactionContext transactionContext) {
        
        EbAaJblLockerDetailsRecord locDetRec= new EbAaJblLockerDetailsRecord(currentRecord);
        String status = locDetRec.getStatus().getValue();
        String locAcct = locDetRec.getLockerAcct().getValue();
        //if status is free, cannot assign any locker account manually
        if (status.equals("Free") && locAcct != null && !locAcct.isEmpty()) {
            locDetRec.getLockerAcct()
                     .setError("Locker Account must be empty when Status is Free");
            return locDetRec.getValidationResponse();
        }
        // if status is used -> cannot change the locker account and locker type manually
        
        if(liveRecord != null){
            EbAaJblLockerDetailsRecord liveRec = new EbAaJblLockerDetailsRecord(liveRecord);
            String liveLocAcct = liveRec.getLockerAcct().getValue(); 
            
            if(liveLocAcct != null && !liveLocAcct.isEmpty() && !liveLocAcct.equals(locAcct)){
                locDetRec.getLockerAcct().setError("Locker Account Cannot be modified manually");
                return locDetRec.getValidationResponse();
                
            }
            String currLockerType = locDetRec.getLockerType().getValue();
            String liveLockerType = liveRec.getLockerType().getValue();

            if (liveLockerType != null && !liveLockerType.isEmpty()
                    && !liveLockerType.equals(currLockerType)) {

                locDetRec.getLockerType().setError("Locker Type cannot be modified manually");
                return locDetRec.getValidationResponse();
            }
        }
        
        
        return null;
    }

}
