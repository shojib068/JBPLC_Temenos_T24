package com.temenos.t24;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import com.ibm.icu.text.DecimalFormat;
import com.ibm.icu.text.DecimalFormatSymbols;
import com.temenos.api.TStructure;
import com.temenos.t24.api.complex.eb.servicehook.ServiceData;
import com.temenos.t24.api.complex.eb.servicehook.SynchronousTransactionData;
import com.temenos.t24.api.complex.eb.servicehook.TransactionControl;
import com.temenos.t24.api.hook.system.ServiceLifecycle;
import com.temenos.t24.api.records.aajbllockeracct.AaJblLockerAcctRecord;
import com.temenos.t24.api.records.aajbllockerdetails.AaJblLockerDetailsRecord;
import com.temenos.t24.api.records.aajbllockerparameter.AaJblLockerParameterRecord;
import com.temenos.t24.api.records.customer.CustomerRecord;
import com.temenos.t24.api.records.ebjblsmsbook.EbJblSmsBookRecord;
import com.temenos.t24.api.records.ebjblsmsparameter.EbJblSmsParameterRecord;
import com.temenos.t24.api.records.ebjblsmsparameter.SmsEventClass;
import com.temenos.t24.api.records.ebjblsmsparameter.SmsTextClass;
import com.temenos.t24.api.records.ftcommissiontype.FtCommissionTypeRecord;
import com.temenos.t24.api.records.tax.TaxRecord;
import com.temenos.t24.api.system.DataAccess;

/**
 * EB.API: LOC.POST.SMS.DUE, LOC.POST.SMS.DUE.SELECT
 * PGM.FILE: LOC.POST.SMS.DUE
 * BATCH: BNK/LOC.POST.SMS.DUE
 * TSA.SERVICE: BNK/LOC.POST.SMS.DUE
 * Date: 6th July, 2026
 * @author kawsar
 *
 */
public class GbSAaJblLocPostTxnSmsDue extends ServiceLifecycle{

    @Override
    public List<String> getIds(ServiceData serviceData, List<String> controlList) {
        DataAccess da = new DataAccess(this);
        List<String> chargeIds = new ArrayList<>();
        List<String> resultIds = new ArrayList<>();
        LocalDate today = LocalDate.now();
        double todayYear = today.getYear();
        chargeIds = da.selectRecords("BNK", "A.JBL.LOCKER.CHARGE", "", "");
        for( String ids : chargeIds ){
            String lockerAcctNo = ids.split("\\-")[0];
            String chargeType = ids.split("\\-")[1];
            String chargeYearStr = ids.split("\\-")[2];
            double chargeYear = Double.parseDouble(chargeYearStr);
            if("RENT".equalsIgnoreCase(chargeType) && (todayYear == chargeYear)){
                resultIds.add(lockerAcctNo);
            }           
        }
        return resultIds;
    }

    @Override
    public void updateRecord(String id, ServiceData serviceData, String controlItem,
            TransactionControl transactionControl, List<SynchronousTransactionData> transactionData,
            List<TStructure> records) {
        DataAccess da = new DataAccess(this);
        AaJblLockerAcctRecord locAcctRec = null;
        String phoneNo = "";
        String lockerId = "";
        String customerId = "";
        String acctNo = "";
        String acctName = "";
        try{
            locAcctRec = new AaJblLockerAcctRecord(da.getRecord("AA.JBL.LOCKER.ACCT", id));
        }catch(Exception e){}
        if(locAcctRec!=null){
            try{
                phoneNo = locAcctRec.getMobileNumber().getValue();
            }catch(Exception e){}
            try{
                lockerId = locAcctRec.getLockerId().getValue();
            }catch(Exception e){}
            try{
                customerId = locAcctRec.getCustomer().getValue();
            }catch(Exception e){}
            try{
                acctNo = locAcctRec.getAcctNo().getValue().toString();
            }catch(Exception e){}
            try{
                acctName = locAcctRec.getAcctName().getValue().toString();
            }catch(Exception e){}
        }
//        Check Customer 
        
        CustomerRecord cusRec = null;
        String cusStatus = "";
        if(!customerId.isEmpty()){
            try{
                cusRec = new CustomerRecord(da.getRecord("CUSTOMER", customerId));
            }catch(Exception e){}
        }
        if(cusRec!= null){
            try{
                cusStatus = cusRec.getCustomerStatus().getValue();
            }catch(Exception e){}
        }
        AaJblLockerDetailsRecord locDetRec = null;
        String locType = "";
        if(!lockerId.isEmpty()){
            try{
                locDetRec = new AaJblLockerDetailsRecord(da.getRecord("AA.JBL.LOCKER.DETAILS", lockerId));
            }catch(Exception e){}
        }
        if(locDetRec!= null){
            try{
                locType = locDetRec.getLockerType().getValue();
            }catch(Exception e){}
        }
        AaJblLockerParameterRecord locParamRec = null;
        String comm = "";
        String staffComm = "";
        String resultComm = "";
        if(!locType.isEmpty()){
            try{
                locParamRec = new AaJblLockerParameterRecord(da.getRecord("AA.JBL.LOCKER.PARAMETER", locType));
            }catch(Exception e){}
        }
        if(locParamRec!=null){
            try{
                comm = locParamRec.getCommission().getValue();
            }catch(Exception e){}
            try{
                staffComm = locParamRec.getStaffCommission().getValue();
            }catch(Exception e){}
        }
//        commission setup
        resultComm = "7".equals(cusStatus) ? staffComm : comm;
        
        FtCommissionTypeRecord ftCommTypeRec = null;
        String flatAmtStr = "";
        String taxCode = "";
        if(!resultComm.isEmpty()){
            try{
                ftCommTypeRec = new FtCommissionTypeRecord(da.getRecord("FT.COMMISSION.TYPE", resultComm));
            }catch(Exception e){}
        }
        if(ftCommTypeRec!=null){
            try{
                flatAmtStr = ftCommTypeRec.getCurrency(0).getFlatAmt().getValue();
            }catch(Exception e){}
            try{
                taxCode = ftCommTypeRec.getTaxCode().getValue();
            }catch(Exception e){}
        }
//        Tax
        double taxRate = 0.0;
        try {
            List<String> taxIds = da.selectRecords(
                    "BNK", "TAX", "", "WITH @ID LIKE '" + taxCode + "...'"
            );

            if (taxIds != null && !taxIds.isEmpty()) {
                for( String taxid : taxIds ){
                    TaxRecord taxRec = new TaxRecord(da.getRecord("TAX", taxid)); 
                    String rate = taxRec.getRate().getValue();
                    if (rate != null) {
                        taxRate = Double.parseDouble(rate) / 100.0;
                    }
                }
            }

        } catch (Exception e) {}
        
//        total charge
        double flatAmt = 0.0;
        double yearlyCharge = 0.0;
        if(!flatAmtStr.isEmpty()){
            flatAmt = Double.parseDouble(flatAmtStr);
        }
        yearlyCharge = flatAmt + ( flatAmt * taxRate );

//        Formatted amount
     
     DecimalFormatSymbols symbols = new DecimalFormatSymbols();
     symbols.setGroupingSeparator(',');

     DecimalFormat df = new DecimalFormat("#,##,##0.00", symbols);

     String formattedCharge = df.format(yearlyCharge);
     
//     SMS BODY
      
     EbJblSmsParameterRecord smsParamRec = new EbJblSmsParameterRecord(da.getRecord("EB.JBL.SMS.PARAMETER", "LOCKER.TXN.SMS"));
     List<SmsEventClass> smsEvent = null;
     SmsTextClass sms1 = null;
     SmsTextClass sms2 = null;
     SmsTextClass sms3 = null;
     SmsTextClass sms4 = null;
     SmsTextClass sms5 = null;
     SmsTextClass sms6 = null;
     if(smsParamRec!=null){
         try{
             smsEvent = smsParamRec.getSmsEvent();
         }catch(Exception e){}
     }
     if(smsEvent!=null){
         for(int i=0; i< smsEvent.size(); i++){
             sms1 = smsEvent.get(i).getSmsText().get(0); //Dear
             sms2 = smsEvent.get(i).getSmsText().get(2); //, yearly locker charge of BDT
             sms3 = smsEvent.get(i).getSmsText().get(4); //for locker
             sms4 = smsEvent.get(i).getSmsText().get(10); //could not be debited.
             sms5 = smsEvent.get(i).getSmsText().get(11); //Please maintain sufficient balance/contact branch.
             sms6 = smsEvent.get(i).getSmsText().get(12); //Thanks.
         }

     }
//     Dear Customer, yearly locker charge of BDT <TOTAL> 
//     for locker <LOCKER.NO> could not be debited. 
//     Please maintain sufficient balance/contact branch.
     String smsBody = sms1 + " " +
             acctName + " " +
             sms2 + " " +
             formattedCharge + " " +
             sms3 + " " +
             lockerId + " " +
             sms4 + " " +
             sms5 + " " +
             sms6;
     DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");
     String smsId = id + "-" + LocalDateTime.now().format(formatter);
     
//     Set up EB.JBL.SMS.BOOK
     EbJblSmsBookRecord smsBookRec = null;
     try{
         smsBookRec = new EbJblSmsBookRecord(this);
         
         smsBookRec.setSmsStatus("PENDING");
         smsBookRec.setPhone(phoneNo);
         smsBookRec.setSmsBody(smsBody);
         
         SynchronousTransactionData txn = new SynchronousTransactionData();
         txn.setVersionId("EB.JBL.SMS.BOOK,LOCKER.OFS");
         txn.setFunction("INPUT");
         txn.setSourceId("LOCKER.OFS");
         txn.setUserName("INPUTT");
         txn.setTransactionId(smsId);

         transactionData.add(txn);
         records.add(smsBookRec.toStructure());
     }catch(Exception e){}
 }
 }