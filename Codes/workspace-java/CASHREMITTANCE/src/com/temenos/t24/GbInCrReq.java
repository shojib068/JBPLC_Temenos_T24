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
 * TODO: 
 *VERSION ATTACHED: 
 *EB.JBL.CASH.FEEDING,INPUT
 *EB.JBL.CASH.FEEDING,INPUT.FCY 
 * @author kawsar
 *DATE: 0.1 08 JUL 2026
 *      0.2 16 JUL 2026
 *      0.4 19 JUL 2026
 *@author khaleque
 *      0.3 16 JUL 2026
 *      
 */
public class GbInCrReq extends RecordLifecycle{

    @Override
    public TValidationResponse validateRecord(String application, String currentRecordId, TStructure currentRecord,
            TStructure unauthorisedRecord, TStructure liveRecord, TransactionContext transactionContext) {
        
        EbJblCashFeedingRecord cashFeedRec = null;
        String versionId = transactionContext.getCurrentVersionId();
        Date tdate = new Date(this);
        String todayS = tdate.getDates().getToday().getValue();
        try{
            cashFeedRec = new EbJblCashFeedingRecord(currentRecord);
        }catch(Exception e){
            return cashFeedRec.getValidationResponse();
        }
        
//        Request date cannot be greater than 7 days
        String reqDate= "";
        String currency = "";
        if(cashFeedRec!=null){
            try{
                reqDate = cashFeedRec.getRequestDate().getValue();
            }catch (Exception e){}
            try{
                currency = cashFeedRec.getCurrency().getValue();
            }catch(Exception e){}
        }
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
//        Currency check
        
//        1. FCY cash feeding & cash sending cannot be BDT
        
        if(",INPUT.FCY".equalsIgnoreCase(versionId) || 
                ",SENDING.FCY".equalsIgnoreCase(versionId)){
            if("BDT".equalsIgnoreCase(currency)){
                cashFeedRec.getCurrency().setError("Invalid Currency");
            }
        }  
        return cashFeedRec.getValidationResponse();
    }
}
