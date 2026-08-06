package com.temenos.t24;

import java.util.ArrayList;
import java.util.List;

import com.temenos.api.TField;
import com.temenos.api.TStructure;
import com.temenos.t24.api.complex.eb.templatehook.TransactionContext;
import com.temenos.t24.api.hook.system.RecordLifecycle;
import com.temenos.t24.api.records.drawings.DrawingsRecord;
import com.temenos.t24.api.records.letterofcredit.LetterOfCreditRecord;
import com.temenos.t24.api.system.DataAccess;

/**
 * ROUTINE.TYPE: Default Routine
 * VERSION.ATTACHED: DRAWINGS,JBL.BTBRD, DRAWINGS,JBL.RD, DRAWINGS,JBL.BTBLODGE
 * MANTIS.ISSUE: 004204 Req 2
 * BUSINESS.LOGIC:
 * if PRESENTOR.CUST & PRESENTOR == null
 *  1. go to LC application
 *  2. get ADVISING.BK.CUSTNO
 *      2.1 if ADVISING.BK.CUSTNO != NULL
 *          3.1 PRESENTOR.CUST = ADVISING.BK.CUSTNO
 *      2.2 if ADVISING.BK.CUSTNO == NULL
 *          3.2 get ADVISING.BK
 *          3.3 if ADVISING.BK!=null
 *              4.1 PRESENTOR = ADVISING.BK
Code FLow:
                START
                  |
                  v
      defaultFieldValues()
                  |
                  v
      Read DRAWINGS Record
                  |
                  v
Are PRESENTOR.CUST and PRESENTOR empty?
            /              \
          No                Yes
          |                  |
          |                  v
          |       Read LETTER.OF.CREDIT
          |                  |
          |                  v
          |   Is ADVISING.BK.CUSTNO available?
          |          /               \
          |        Yes               No
          |         |                 |
          |         v                 v
          | Set PRESENTOR.CUST   Copy ADVISING.BK
          |                      to PRESENTOR(s)
          |             \          /
          |              \        /
          |               v      v
          +----------- Save Record
                  |
                  v
                 END
 *
 * @author kawsar
 *  VERSION 0.1 -> 25 JUL 2026
 */
public class TfBdDDrawPrsntrCustPrsntr extends RecordLifecycle{

    @Override
    public void defaultFieldValues(String application, String currentRecordId, TStructure currentRecord,
            TStructure unauthorisedRecord, TStructure liveRecord, TransactionContext transactionContext) {
        DrawingsRecord drawRec = new DrawingsRecord(currentRecord);
        DataAccess da = new DataAccess(this);
        LetterOfCreditRecord lcRec = null;
        String presentorCust = "";
        String presentor = "";
        List<TField> advisingBkList = new ArrayList<>();
        if(drawRec!=null){
            try{
                presentorCust = drawRec.getPresentorCust().getValue();
            }catch(Exception e){}
            try{
                presentor = drawRec.getPresentor(0).getValue();
            }catch(Exception e){}
        }
        String drawId = "";
        if(presentorCust.isEmpty() && presentor.isEmpty()){
            if(drawRec!=null){
                drawId = currentRecordId.substring(0, currentRecordId.length()-2);
                try{
                    lcRec = new LetterOfCreditRecord(da.getRecord("LETTER.OF.CREDIT", drawId));
                }catch(Exception e){}           
            }
            if(lcRec!=null){
                String advisingBkCustNo = "";
                try{
                    advisingBkCustNo = lcRec.getAdvisingBkCustno().getValue();
                }catch(Exception e){}
                if(!advisingBkCustNo.isEmpty()){
                    drawRec.setPresentorCust(advisingBkCustNo);
                }else{
                    try{
                        advisingBkList = lcRec.getAdvisingBk();
                    }catch(Exception e){}
                    if(!advisingBkList.isEmpty() || advisingBkList!=null){
                        for(int i=0; i<advisingBkList.size(); i++){
                            String x = advisingBkList.get(i).getValue();
                            if(!x.isEmpty()){
                                drawRec.setPresentor(x, i);
                            }                            
                        }                       
                    }
                }
            }
        }
        currentRecord.set(drawRec.toStructure());
    }
}
