package com.temenos.t24;

import java.util.ArrayList;
import java.util.List;

import com.temenos.api.TField;
import com.temenos.api.TStructure;
import com.temenos.t24.api.complex.eb.servicehook.ServiceData;
import com.temenos.t24.api.complex.eb.servicehook.SynchronousTransactionData;
import com.temenos.t24.api.complex.eb.servicehook.TransactionControl;
import com.temenos.t24.api.hook.system.ServiceLifecycle;
import com.temenos.t24.api.records.aajbllockeracct.AaJblLockerAcctRecord;
import com.temenos.t24.api.records.aajbllockercharge.AaJblLockerChargeRecord;
import com.temenos.t24.api.records.acchargerequest.AcChargeRequestRecord;
import com.temenos.t24.api.records.acchargerequest.ChargeCodeClass;
import com.temenos.t24.api.records.account.AccountRecord;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.system.Session;

/**
 * EB.API:LOC.CHARGE.DEDUCT.SELECT, LOC.CHARGE.DEDUCT
 * PGM.FILE:LOC.CHARGE.DEDUCT
 * BATCH:BNK/LOC.CHARGE.DEDUCT
 * TSA.SERVICE:BNK/LOC.CHARGE.DEDUCT
 * @author kawsar
 *
 */
public class GbSAaJblLockerDeductCharge extends ServiceLifecycle{
    
    // =====================================================
    // Extract year from ID (01.01-RENT-2022/01.01-INS-2022 → 2022)
    // =====================================================
    
    @Override
    public List<String> getIds(ServiceData serviceData, List<String> controlList) {
        DataAccess da = new DataAccess(this);
        Session ss = new Session(this);

        String coCode = ss.getCompanyId();
        String selectStmt = " WITH CO.CODE EQ " + coCode+ " AND STATUS EQ 'Due'";

        List<String> chargeIds = da.selectRecords("BNK", "AA.JBL.LOCKER.CHARGE", "", selectStmt);
        return chargeIds;

    }

    
//    final ids -> 01.01-RENT-2022, 01.01-INS-2022
    @Override
    public void updateRecord(String id, ServiceData serviceData, String controlItem,
            TransactionControl transactionControl, List<SynchronousTransactionData> transactionData,
            List<TStructure> records) {
           DataAccess da = new DataAccess(this);
           String lockerId = id.split("\\-")[0];  //01.01
           String chargeType = id.split("\\-")[1]; // RENT INS
           String acctNo = lockerId.split("\\.")[0];  // 01
           
           AaJblLockerChargeRecord locCrgRec = null;
           String chargeCode = "";
           String chargeAmountStr = "";
           double chargeAmount = 0.0;
           try{
               locCrgRec = new AaJblLockerChargeRecord(da.getRecord("AA.JBL.LOCKER.CHARGE", id));
           }catch(Exception e){}
           if(locCrgRec!=null){
               try{
                   chargeCode = locCrgRec.getChargeCode().getValue();
               }catch(Exception e){}
               try{
                   chargeAmountStr = locCrgRec.getChargeAmount().getValue();
               }catch(Exception e){}
           }
           if(!chargeAmountStr.isEmpty()){
               chargeAmount = Double.parseDouble(chargeAmountStr);
           }
           // read Account record
           
           AccountRecord accRec = null;
           String workingBalanceStr = "";
           List<TField> prList = new ArrayList<>();
           double workingBalance = 0.0;
           if(!acctNo.isEmpty()){
               try{
                   accRec = new AccountRecord(da.getRecord("ACCOUNT", acctNo));
               }catch(Exception e){} 
           }
           if(accRec != null){
               try{
                   workingBalanceStr = accRec.getWorkingBalance().getValue();
               }catch(Exception e){}
               try{
                   prList= accRec.getPostingRestrict();
               }catch(Exception e){}
           }
           if(!workingBalanceStr.isEmpty()){
               workingBalance = Double.parseDouble(workingBalanceStr.replace(",", ""));
           } 
           
           boolean isFreeze = false;
           if(prList != null ){
               for(int i = 0; i< prList.size(); i++){
                   String pr = prList.get(i).getValue();
                   if(pr.equals("15")){
                       isFreeze = true;
                   }
               }
           }
           
           if(isFreeze == false){
               AcChargeRequestRecord req = null;
               AaJblLockerAcctRecord setIns = null;
               AaJblLockerChargeRecord lockerChargeRec = null;
               if(workingBalance >= chargeAmount){
                   try{
                       req = new AcChargeRequestRecord(this);
                       
                       req.setRequestType("BOOK");
                       req.setDebitAccount(acctNo);
                       req.setStatus("PAID");
                       req.setRelatedRef("LOCKER CHARGE");
                       req.setExtraDetails("LOCKER DETAILS", 0);
                       req.setChargeCcy("BDT");
                       
                       ChargeCodeClass crgCode = new ChargeCodeClass();
                       crgCode.setChargeCode(chargeCode);
                       req.addChargeCode(crgCode);
                       
                       
//                     ofs 1
                     
                     SynchronousTransactionData txn1 = new SynchronousTransactionData();                     
                     txn1.setFunction("INPUT");                    
                     txn1.setUserName("INPUTT");
                     txn1.setSourceId("LOCKER.OFS");
                     txn1.setVersionId("AC.CHARGE.REQUEST,LOCKER.OFS");

                     transactionData.add(txn1);
                     records.add(req.toStructure());
                   }catch(Exception e){}
                   
                   if("INS".equalsIgnoreCase(chargeType)){
//                     after insurance, set flag to yes
                       try{
                           setIns = new AaJblLockerAcctRecord(da.getRecord("AA.JBL.LOCKER.ACCT", lockerId));
                           setIns.setInsurance("YES");
                           
//                         ofs 2
                         SynchronousTransactionData txn2 = new SynchronousTransactionData();                     
                         txn2.setFunction("INPUT");          
                         txn2.setUserName("INPUTT");
                         txn2.setTransactionId(lockerId);
                         txn2.setSourceId("LOCKER.OFS");
                         txn2.setVersionId("AA.JBL.LOCKER.ACCT,OFS");

                         transactionData.add(txn2);
                         records.add(setIns.toStructure());
                       }catch(Exception e){}
                   }
//                 update the charge table
                   
                   try{
                       lockerChargeRec = new AaJblLockerChargeRecord(da.getRecord("AA.JBL.LOCKER.CHARGE", id));
                       
//                       ofs 3                      
                       SynchronousTransactionData txn3 = new SynchronousTransactionData();
                       txn3.setFunction("REVERSE");
                       txn3.setUserName("INPUTT");
                       txn3.setSourceId("LOCKER.OFS");
                       txn3.setTransactionId(id);
                       txn3.setVersionId("AA.JBL.LOCKER.CHARGE,OFS");
                       
                       transactionData.add(txn3);
                       records.add(lockerChargeRec.toStructure());
                   
                   }catch(Exception e){}
                   
               }
           }
    }
}
