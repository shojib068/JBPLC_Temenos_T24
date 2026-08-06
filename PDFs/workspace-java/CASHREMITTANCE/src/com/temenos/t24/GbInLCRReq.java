package com.temenos.t24;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import com.temenos.api.TStructure;
import com.temenos.api.TValidationResponse;
import com.temenos.t24.api.complex.eb.templatehook.TransactionContext;
import com.temenos.t24.api.hook.system.RecordLifecycle;
import com.temenos.t24.api.records.ebjblcashfeeding.EbJblCashFeedingRecord;
import com.temenos.t24.api.system.Date;

/**
 * TODO: Document me!
 *Date: 0.1 -> 19/07/2026
 * @author kawsar
 *
 */
public class GbInLCRReq extends RecordLifecycle{

    @Override
    public TValidationResponse validateRecord(String application, String currentRecordId, TStructure currentRecord,
            TStructure unauthorisedRecord, TStructure liveRecord, TransactionContext transactionContext) {
        EbJblCashFeedingRecord cashFeedRec = null;
        try{
            cashFeedRec = new EbJblCashFeedingRecord(currentRecord);
        }catch(Exception e){
            return cashFeedRec.getValidationResponse();
        }
        Date tdate = new Date(this);
        String todayS = tdate.getDates().getToday().getValue();
        String reqDate= "";
        try{
            reqDate = cashFeedRec.getRequestDate().getValue();
        }catch (Exception e){}
        if ((currentRecordId.startsWith("CR-") || currentRecordId.startsWith("SR-")) && !reqDate.isEmpty()) {
            try {
                DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("yyyyMMdd");
                
                LocalDate txnDate = LocalDate.parse(reqDate, dateFormat);
                LocalDate today = LocalDate.parse(todayS, dateFormat); 
                
                if (txnDate.isAfter(today.plusDays(7))) {
                    cashFeedRec.getRequestDate().setError("Request Date cannot be more than 7 days");
                }
            } catch (Exception e) {
                cashFeedRec.getRequestDate().setError("Invalid Date Format");
            }
        }
        
        return cashFeedRec.getValidationResponse();
    }

}
