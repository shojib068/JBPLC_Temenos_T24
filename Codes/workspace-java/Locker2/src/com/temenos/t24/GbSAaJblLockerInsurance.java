package com.temenos.t24;

import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import com.temenos.api.TField;
import com.temenos.api.TStructure;
import com.temenos.t24.api.complex.eb.servicehook.ServiceData;
import com.temenos.t24.api.complex.eb.servicehook.SynchronousTransactionData;
import com.temenos.t24.api.complex.eb.servicehook.TransactionControl;
import com.temenos.t24.api.hook.system.ServiceLifecycle;
import com.temenos.t24.api.records.aagblockerparam.AaGbLockerParamRecord;
import com.temenos.t24.api.records.aajbllockeracct.AaJblLockerAcctRecord;
import com.temenos.t24.api.records.aajbllockercharge.AaJblLockerChargeRecord;
import com.temenos.t24.api.records.aajbllockerdetails.AaJblLockerDetailsRecord;
import com.temenos.t24.api.records.aajbllockerparameter.AaJblLockerParameterRecord;
import com.temenos.t24.api.records.acchargerequest.AcChargeRequestRecord;
import com.temenos.t24.api.records.acchargerequest.ChargeCodeClass;
import com.temenos.t24.api.records.account.AccountRecord;
import com.temenos.t24.api.records.ftcommissiontype.CurrencyClass;
import com.temenos.t24.api.records.ftcommissiontype.FtCommissionTypeRecord;
import com.temenos.t24.api.records.tax.TaxRecord;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.system.Session;

/**
 * TODO: Document me!
 *
 * @author kawsar
 *
 */
public class GbSAaJblLockerInsurance extends ServiceLifecycle{
    private static final DateTimeFormatter T24_DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMdd");

    @Override
    public List<String> getIds(ServiceData serviceData, List<String> controlList) {
        DataAccess da = new DataAccess(this);
        Session ss = new Session(this);        
        String coCode = ss.getCompanyId();
        List<String> locAcctIds = new ArrayList<>();
        
        locAcctIds = da.selectRecords("BNK", "AA.JBL.LOCKER.ACCT", "", " WITH CO.CODE EQ "+coCode);
        return locAcctIds;
    }
//    locAcctIds -> 01.01, 01.02 ...
    
    @Override
    public void updateRecord(String id, ServiceData serviceData, String controlItem,
            TransactionControl transactionControl, List<SynchronousTransactionData> transactionData,
            List<TStructure> records) {
        DataAccess da = new DataAccess(this);
        String lockerAcctNo = id; //01.01
        String acctNo = lockerAcctNo.split("\\.")[0]; //01
        LocalDate today = LocalDate.now();
        String currentYearStr = String.valueOf(today.getYear());
        double currentYear = Double.parseDouble(currentYearStr);
        
        AaJblLockerAcctRecord locAcctRec = null;
        String locInsYearStr = "";
        String lockerId = "";
        double locInsYear = 0.0;
        try{
            locAcctRec = new AaJblLockerAcctRecord(da.getRecord("AA.JBL.LOCKER.ACCT", id));
        }catch(Exception e){}
        if(locAcctRec != null ){
            try{
                lockerId = locAcctRec.getLockerId().getValue();
            }catch(Exception e){}
            try{
                locInsYearStr = locAcctRec.getLastDueYear().getValue();
            }catch(Exception e){}
        }
        if(!locInsYearStr.isEmpty()){
            try{
                locInsYear = Double.parseDouble(locInsYearStr);
            }catch(Exception e){}
        }
        
//      read gb.parameter record for insurance year
        AaGbLockerParamRecord gbLocParamRec = null;
        String insParamYearStr = "";
        double insParamYear = 0.0;
        try{
            gbLocParamRec = new AaGbLockerParamRecord(da.getRecord("AA.GB.LOCKER.PARAM", "SYSTEM"));
        }catch(Exception e){}
        if(gbLocParamRec!=null){
            try{
                insParamYearStr =  gbLocParamRec.getInsuranceYear().getValue();
            }catch(Exception e){}
        }
        if(!insParamYearStr.isEmpty()){
            try{
                insParamYear = Double.parseDouble(insParamYearStr);
            }catch(Exception e){}
        }
//      read locker details record
        AaJblLockerDetailsRecord locDetRec = null;
        String lockerType = "";
      if(!lockerId.isEmpty()){
          try{
              locDetRec = new AaJblLockerDetailsRecord(da.getRecord("AA.JBL.LOCKER.DETAILS", lockerId));
          }catch(Exception e){}
      }
      
      if(locDetRec != null){
          try{
              lockerType = locDetRec.getLockerType().getValue();
          }catch(Exception e){}
      }
//      read locker parameter
      AaJblLockerParameterRecord locParamRec = null;
      String insuranceComm = "";
      if(!lockerType.isEmpty() ){
          try{
              locParamRec = new AaJblLockerParameterRecord(da.getRecord("AA.JBL.LOCKER.PARAMETER", lockerType));
          }catch(Exception e){}     
      }
      if(locParamRec != null){
          try{
              insuranceComm=  locParamRec.getInsuranceCommission().getValue();
          }catch(Exception e){}
      }
      // =====================================================
      // Working balance
      // =====================================================
      AccountRecord accRec = null;
      String workingBalanceStr = "";
      double workingBalance = 0.0;
      List<TField> prList = new ArrayList<>();
      try{
          accRec = new AccountRecord(da.getRecord("ACCOUNT", acctNo));
      }catch(Exception e){}
      if(accRec != null){
          try{
              workingBalanceStr = accRec.getWorkingBalance().getValue();
          }catch(Exception e){}
          try{
              prList = accRec.getPostingRestrict();
          }catch(Exception e){}
      }
      
      if (!workingBalanceStr.isEmpty()) {
          workingBalance = Double.parseDouble(workingBalanceStr.replace(",", ""));
      }
      
      // =====================================================
      // Commission
      // =====================================================
      FtCommissionTypeRecord ftCommTypeRec = null;
      String taxCode = "";
      double taxRate = 0.0;
      double commissionAmount = 0.0;
      try {
          ftCommTypeRec = new FtCommissionTypeRecord(
                  da.getRecord("FT.COMMISSION.TYPE", insuranceComm));

          taxCode = ftCommTypeRec.getTaxCode().getValue();

          List<CurrencyClass> list = ftCommTypeRec.getCurrency();
          if (list != null) {
                  String flat = list.get(0).getFlatAmt().getValue();
                      commissionAmount = Double.parseDouble(flat);
          }

      } catch (Exception e) {}
      
      // =====================================================
      // Tax
      // =====================================================
      List<String> taxIds = new ArrayList<>();
      try {
          taxIds = da.selectRecords( "BNK", "TAX", "", "WITH @ID LIKE '" + taxCode + "...'");
          }catch(Exception e){}
      if (taxIds != null && !taxIds.isEmpty()) {
          for(String ids : taxIds){
              TaxRecord taxRec = new TaxRecord(da.getRecord("TAX", ids));
              String rate = taxRec.getRate().getValue();
              if (rate != null) {
                  taxRate = Double.parseDouble(rate) / 100.0;
              }
          }       
      }
      double yearlyCharge = commissionAmount + (commissionAmount * taxRate);
      DecimalFormat df = new DecimalFormat("#.##");
      String yrlyCrg = df.format(yearlyCharge);
//      
      boolean isFreeze = false;
      if(prList != null ){
          for(int i = 0; i< prList.size(); i++){
              String pr = prList.get(i).getValue();
              if(pr.equals("15")){
                  isFreeze = true;
              }
          }
      }

          AcChargeRequestRecord req = null;
          AaJblLockerChargeRecord due = null;
          AaJblLockerAcctRecord setIns = null;
          
//        create insurance charge
//        2026 - 2022 >= 3
          if((currentYear - locInsYear) >= insParamYear){  
             
              if(( workingBalance >= yearlyCharge) && (isFreeze == false)){
//                  deduct charge
                  
                  try {
                      req = new AcChargeRequestRecord(this);
          
                      req.setRequestType("BOOK");
                      req.setDebitAccount(acctNo);
                      req.setStatus("PAID");
                      req.setRelatedRef("LKR INS CRG");
                      req.setChargeCcy("BDT");         
                      ChargeCodeClass comm = new ChargeCodeClass();
                      comm.setChargeCode(insuranceComm);
                      req.setChargeCode(comm, 0);         
                      req.setExtraDetails("LOCKER INSURANCE CHARGE", 0);
          
//          ofs 1

                      SynchronousTransactionData txn1 = new SynchronousTransactionData();
          
                      txn1.setFunction("INPUT");            
                      txn1.setUserName("INPUTT");
                      txn1.setSourceId("LOCKER.OFS");
                      txn1.setVersionId("AC.CHARGE.REQUEST,LOCKER.OFS");

                      transactionData.add(txn1);
                      records.add(req.toStructure());

                  } catch (Exception e) {}
      
                  try{
//          set insurance flag
          
                      setIns = new AaJblLockerAcctRecord(this);
                      setIns.setInsurance("YES");
                      
//          ofs 2
                      SynchronousTransactionData txn2 = new SynchronousTransactionData();
                      txn2.setVersionId("AA.JBL.LOCKER.ACCT,OFS");
                      txn2.setFunction("INPUT");
                      txn2.setSourceId("LOCKER.OFS");
                      txn2.setUserName("INPUTT");
                      txn2.setTransactionId(lockerAcctNo);

                      transactionData.add(txn2);
                      records.add(setIns.toStructure());
          
                  }catch(Exception e){}           
              }
              
//              create due record
              
              if(( workingBalance < yearlyCharge) || (isFreeze == true)){
                  try {
                      due = new AaJblLockerChargeRecord(this);
                          due.setLockerAcctId(id);
                          due.setDueDate(today.format(T24_DATE_FORMAT));
                          due.setStatus("Due");
                          due.setChargeAmount(yrlyCrg);
                          due.setChargeCode(insuranceComm);
          
//          ofs 3
                          SynchronousTransactionData txn3 = new SynchronousTransactionData();
                          txn3.setVersionId("AA.JBL.LOCKER.CHARGE,OFS");
                          txn3.setFunction("INPUT");
                          txn3.setSourceId("LOCKER.OFS");
                          txn3.setUserName("INPUTT");
                          txn3.setTransactionId(id + "-INS-" + currentYearStr);

                          transactionData.add(txn3);
                          records.add(due.toStructure());

                  } catch (Exception e) {} 
  }
          
  }
      }

}