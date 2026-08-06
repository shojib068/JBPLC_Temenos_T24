package com.temenos.t24;

import com.temenos.api.TStructure;
import com.temenos.api.TValidationResponse;
import com.temenos.t24.api.complex.eb.templatehook.TransactionContext;
import com.temenos.t24.api.hook.system.RecordLifecycle;
import com.temenos.t24.api.records.drawings.DrawingsRecord;

/**
 * TODO: 
    ROUTINE.TYPE: Validation Routine
    VERSION.ATTACHED: DRAWINGS,JBL.F.EXPDOCREAL
    MANTIS.ISSUE: 004204 REQ 3
    BUSINESS.LOGIC:
    If user input anything other than (0 to 5) On drawings application field 
    ‘CUSTOMER.SPREAD’ give an error saying ‘Allowable spread range is upto 5’.
 *  CODE.FLOW:
 *                  START
                  |
                  v
        validateRecord()
                  |
                  v
     Read DRAWINGS Record
                  |
                  v
     Get CUSTOMER.SPREAD
                  |
                  v
Is |CUSTOMER.SPREAD| > 5?
            /             \
          Yes             No
           |               |
           v               |
 Display Validation Error   |
           |               |
           +-------+-------+
                   |
                   v
      Return Validation Response
                   |
                   v
                  END
 * @author kawsar
 *  0.1 -> 26 July 2026
 */
public class TfBdVDrawCustSpreadRange extends RecordLifecycle{

    @Override
    public TValidationResponse validateRecord(String application, String currentRecordId, TStructure currentRecord,
            TStructure unauthorisedRecord, TStructure liveRecord, TransactionContext transactionContext) {
        // TODO Auto-generated method stub
        DrawingsRecord drRec = new DrawingsRecord(currentRecord);
        String custSpreadStr = "";
        double custSpread = 0.0;
        if(drRec!=null){
            try{
                custSpreadStr = drRec.getCustomerSpread().getValue();
            }catch(Exception e){}
            if(!custSpreadStr.isEmpty()){
                custSpread = Double.parseDouble(custSpreadStr);
            }           
        }
        if(Math.abs(custSpread) > 5){
            drRec.getCustomerSpread().setError("Allowable spread range is upto 5");
        }    
        return drRec.getValidationResponse();
    }
}
