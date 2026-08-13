package com.temenos.t24;

import java.util.ArrayList;
import java.util.List;

import com.temenos.api.TStructure;
import com.temenos.t24.api.complex.eb.servicehook.TransactionData;
import com.temenos.t24.api.complex.eb.templatehook.TransactionContext;
import com.temenos.t24.api.hook.system.RecordLifecycle;
import com.temenos.t24.api.records.ebjblcashfeeding.EbJblCashFeedingRecord;
import com.temenos.t24.api.records.teller.TellerRecord;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.tables.ebjblcashfeeding.EbJblCashFeedingTable;

/**
 * TODO: Document me!
 *
 * @author kawsar
 * VERSION ATTACHED: 
 * TELLER,JBL.CASHWDL.ACPT
 * TELLER,JBL.CASHWDL.ACPT.FCY
 * 
 * 0.1-> 16 JUL 2026
 *
 */
public class GbACrCfAdjAmt extends RecordLifecycle{

    @Override
    public void postUpdateRequest(String application, String currentRecordId, TStructure currentRecord,
            List<TransactionData> transactionData, List<TStructure> currentRecords,
            TransactionContext transactionContext) {
       TellerRecord tellerRec = null;
       DataAccess da = new DataAccess(this);
       String versionId = transactionContext.getCurrentVersionId();
       EbJblCashFeedingRecord cfRec = null;
       EbJblCashFeedingTable cfTable = null;
       String tellerId = currentRecordId;
       String approvedAmtStr = "";
       String rcvAmtStr = "";
       String adjAmtStr = "";
       String thRef = "";
       double approvedAmt = 0.0;
       double rcvAmt = 0.0;
       double adjAmt = 0.0;
       try{
           tellerRec = new TellerRecord(currentRecord);
       }catch(Exception e){}
       if(tellerRec!= null){
           try{
               approvedAmtStr = tellerRec.getLocalRefField("LT.APR.AMOUNT").getValue();
               approvedAmt = Double.parseDouble(approvedAmtStr);
           }catch(Exception e){}
//           FOr local
           if(",JBL.CASHWDL.ACPT".equalsIgnoreCase(versionId)){
               try{
                   rcvAmtStr = tellerRec.getAccount1(0).getAmountLocal1().getValue();
                   rcvAmt = Double.parseDouble(rcvAmtStr);
               }catch(Exception e){}
               adjAmt = approvedAmt - rcvAmt;
               adjAmtStr = String.valueOf(adjAmt);
           }
//           For foreign
           if(",JBL.CASHWDL.ACPT.FCY".equalsIgnoreCase(versionId)){
               try{
                   rcvAmtStr = tellerRec.getAccount1(0).getAmountFcy1().getValue();
                   rcvAmt = Double.parseDouble(rcvAmtStr);
               }catch(Exception e){}
               adjAmt = approvedAmt - rcvAmt;
               adjAmtStr = String.valueOf(adjAmt);
           }
//        -----------------------------------------------------   
           try {
               thRef = tellerRec.getTheirReference().getValue();
           } catch (Exception e) {}   
       }
       try {
           cfRec = new EbJblCashFeedingRecord(da.getRecord("EB.JBL.CASH.FEEDING", thRef));
       } catch (Exception e) {}
               if(cfRec!=null){
                   cfRec.setAdjustmentAmount(adjAmtStr);
                   cfTable = new EbJblCashFeedingTable(this);
                   try{
                       cfTable.write(thRef, cfRec);
                   }catch(Exception e){}
               }
           }
}
