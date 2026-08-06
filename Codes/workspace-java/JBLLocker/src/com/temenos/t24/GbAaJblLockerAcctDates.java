package com.temenos.t24;


import com.temenos.api.TStructure;
import com.temenos.api.TValidationResponse;
import com.temenos.t24.api.complex.eb.templatehook.TransactionContext;
import com.temenos.t24.api.hook.system.RecordLifecycle;
import com.temenos.t24.api.records.ebaajbllockeracct.EbAaJblLockerAcctRecord;

/**
 * TODO: CLOSING.DATE > OPENING.DATE 
 *
 * @author kawsar
 *
 */
public class GbAaJblLockerAcctDates extends RecordLifecycle{

    @Override
    public TValidationResponse validateRecord(String application, String currentRecordId, TStructure currentRecord,
            TStructure unauthorisedRecord, TStructure liveRecord, TransactionContext transactionContext) {
       
        EbAaJblLockerAcctRecord lockerAcctRec = new EbAaJblLockerAcctRecord(currentRecord);
        String openDate = lockerAcctRec.getOpeningDate().getValue();
        String closeDate = lockerAcctRec.getClosingDate().getValue();
        
        //check the opening.date field
        try{
            if(openDate.isEmpty() ){
                lockerAcctRec.getOpeningDate().setError("Opening Date cannot be empty");
                return lockerAcctRec.getValidationResponse();
            }
        }catch(Exception e){
            throw e;
         }
        
        //check the closing date field
        try{
            if(closeDate.isEmpty() ){
                lockerAcctRec.getClosingDate().setError("Closing Date cannot be empty");
                return lockerAcctRec.getValidationResponse();
            }
        }catch(Exception e ){
            throw e;
        }
        
        //check opening.date < closing.date
       
       try{
           if( !openDate.isEmpty() && !closeDate.isEmpty()){
               int openingDate = Integer.parseInt(openDate);
               int closingDate = Integer.parseInt(closeDate);
               if( closingDate < openingDate ){
                   lockerAcctRec.getClosingDate().setError("Closing Date cannot be earlier than Opening Date");
                   return lockerAcctRec.getValidationResponse();
               }
           }
           
       }catch(Exception e){
           throw e;
           
       } 
            
        return null;
    }
    

}
