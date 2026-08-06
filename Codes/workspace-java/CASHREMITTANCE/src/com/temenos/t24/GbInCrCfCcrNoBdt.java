package com.temenos.t24;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import com.ibm.icu.impl.duration.DateFormatter;
import com.temenos.api.TStructure;
import com.temenos.api.TValidationResponse;
import com.temenos.t24.api.complex.eb.templatehook.TransactionContext;
import com.temenos.t24.api.hook.system.RecordLifecycle;
import com.temenos.t24.api.records.ebjblcashfeeding.EbJblCashFeedingRecord;

/**
 * TODO: 
 *
 * @author kawsar
 *DATE: 0.1 08 JUL 2026
 *      0.2 16 JUL 2026
 */
public class GbInCrCfCcrNoBdt extends RecordLifecycle{

    @Override
    public TValidationResponse validateRecord(String application, String currentRecordId, TStructure currentRecord,
            TStructure unauthorisedRecord, TStructure liveRecord, TransactionContext transactionContext) {
        EbJblCashFeedingRecord cashFeedRec = null;
        try{
            cashFeedRec = new EbJblCashFeedingRecord(currentRecord);
        }catch(Exception e){}
        String ccy = "";
        String reqDate = "";
        if(cashFeedRec!=null){
            try{
                ccy = cashFeedRec.getCurrency().getValue();
            }catch(Exception e){}
            try{
                reqDate = cashFeedRec.getRequestDate().getValue();
            }catch(Exception e){}
        }
        if(!ccy.isEmpty() && "BDT".equalsIgnoreCase(ccy)){
            cashFeedRec.getCurrency().setError("Invalid Currency");
        }
        if(!reqDate.isEmpty()){
            DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("yyyyMMdd");
            LocalDate txnDate = LocalDate.parse(reqDate, dateFormat);
            LocalDate today = LocalDate.now();
            if(txnDate.isAfter(today.plusDays(7))){
                cashFeedRec.getRequestDate().setError("Request Date cannot be more than 7 day");
            }
        }        
        return cashFeedRec.getValidationResponse();
    }
}
