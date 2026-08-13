package com.temenos.t24;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.temenos.api.TField;
import com.temenos.api.TStructure;
import com.temenos.t24.api.complex.eb.servicehook.ServiceData;
import com.temenos.t24.api.complex.eb.servicehook.SynchronousTransactionData;
import com.temenos.t24.api.complex.eb.servicehook.TransactionControl;
import com.temenos.t24.api.hook.system.ServiceLifecycle;
import com.temenos.t24.api.records.aajbllockeracct.AaJblLockerAcctRecord;
import com.temenos.t24.api.records.aajbllockercharge.AaJblLockerChargeRecord;
import com.temenos.t24.api.records.aajbllockerdetails.AaJblLockerDetailsRecord;
import com.temenos.t24.api.records.aajbllockerparameter.AaJblLockerParameterRecord;
import com.temenos.t24.api.records.acchargerequest.AcChargeRequestRecord;
import com.temenos.t24.api.records.acchargerequest.ChargeCodeClass;
import com.temenos.t24.api.records.account.AccountRecord;
import com.temenos.t24.api.records.customer.CustomerRecord;
import com.temenos.t24.api.records.ftcommissiontype.CurrencyClass;
import com.temenos.t24.api.records.ftcommissiontype.FtCommissionTypeRecord;
import com.temenos.t24.api.records.tax.TaxRecord;
import com.temenos.t24.api.system.DataAccess;

/**
 * EB.API: LOC.CHARGE.POP.SELECT, LOC.CHARGE.POP
 * PGM.FILE: LOC.CHARGE.POP
 * BATCH: BNK/LOC.CHARGE.POP
 * TSA: BNK/LOC.CHARGE.POP
 * ETD: AA.JBL.LOCKER.CHARGE
 * 
 *@author kawsar
 */
public class GbSAaJblLockerChargePopulate extends ServiceLifecycle {

    private static final DateTimeFormatter T24_DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMdd");

    // =====================================================
    // Extract year from ID (01.01-RENT-2022 → 2022)
    // =====================================================
    private int extractYear(String chargeId) {
        try {
            if (chargeId != null && chargeId.contains("-")) {
                return Integer.parseInt(chargeId.split("-")[2]);
            }
        } catch (Exception e) {}
        return 0;
    }

    @Override
    public List<String> getIds(ServiceData serviceData, List<String> controlList) {

        List<String> recIds = new ArrayList<>();
        List<String> nauLocIds = new ArrayList<>();
        DataAccess da = new DataAccess(this);

        String selectStmt = "WITH STATUS EQ 'Active'";

        try {
            recIds = da.selectRecords("BNK", "AA.JBL.LOCKER.ACCT", "", selectStmt);
        } catch (Exception e) {}

        try {
            nauLocIds = da.selectRecords("BNK", "AA.JBL.LOCKER.ACCT$NAU", "", "");
        } catch (Exception e) {}

        if (recIds != null && nauLocIds != null) {
            recIds.removeAll(nauLocIds);
        }

        return recIds;
    }

    @Override
    public void updateRecord(String id,
                             ServiceData serviceData,
                             String controlItem,
                             TransactionControl transactionControl,
                             List<SynchronousTransactionData> transactionData,
                             List<TStructure> records) {
        
        DataAccess da = new DataAccess(this); 
        
        

        LocalDate today = LocalDate.now();
        String currentYear = String.valueOf(today.getYear());

        
        
        
     // =====================================================
        // Read locker account
        // =====================================================
        AaJblLockerAcctRecord lockerAcctRec = null;
        String lockerAcctNo = "";
        String customer = "";
        String lockerId = "";
        String chargeWaive = "";
        
        try {
            lockerAcctRec = new AaJblLockerAcctRecord(
                    da.getRecord("AA.JBL.LOCKER.ACCT", id));
        } catch (Exception e) {}

        if(lockerAcctRec!= null){
            try { 
                lockerAcctNo = lockerAcctRec.getAcctNo().getValue(); 
                } catch (Exception e) {}
            try { 
                customer = lockerAcctRec.getCustomer().getValue(); 
                } catch (Exception e) {}
            try { 
                lockerId = lockerAcctRec.getLockerId().getValue(); 
                } catch (Exception e) {}
            try{
                chargeWaive = lockerAcctRec.getChargeWaive().getValue();
            }catch(Exception e){}

        }        
        // =====================================================
        // Working balance
        // =====================================================
        AccountRecord accRec = null;
        String workingBalanceStr = "";
        List<TField> prList = new ArrayList<>();
        double workingBalance = 0.0;
        
        if(!lockerAcctNo.isEmpty()){
            try{
                accRec = new AccountRecord(da.getRecord("ACCOUNT", lockerAcctNo));
            }catch(Exception e){}
        }
        if(accRec!=null){
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
        // Customer
        // =====================================================
        CustomerRecord cusRec = null;
        String customerStatus = "";
        
        if(!customer.isEmpty()){
            try{
                cusRec = new CustomerRecord(da.getRecord("CUSTOMER", customer));
            }catch(Exception e){}
        }
        if(cusRec!=null){
            try {           
                customerStatus = cusRec.getCustomerStatus().getValue();
            } catch (Exception e) {}   
        }       
        // =====================================================
        // Locker details
        // =====================================================
        AaJblLockerDetailsRecord locDetRec = null;
        String lockerType = "";
        if(!lockerId.isEmpty()){
            try{
                locDetRec = new AaJblLockerDetailsRecord(
                        da.getRecord("AA.JBL.LOCKER.DETAILS", lockerId));
            }catch(Exception e){}
        }
        if(locDetRec!=null){
            try{
                lockerType = locDetRec.getLockerType().getValue();
            }catch(Exception e){}
        }        
        // =====================================================
        // Commission setup
        // =====================================================
        AaJblLockerParameterRecord locParamRec = null;
        String commission = "";
        String staffCommission = "";
        String resultComm = "";
        if(!lockerType.isEmpty()){
            try{
                locParamRec = new AaJblLockerParameterRecord(
                        da.getRecord("AA.JBL.LOCKER.PARAMETER", lockerType));
            }catch(Exception e){}
        }
        if(locParamRec!=null){
            try{
                commission = locParamRec.getCommission().getValue();
            }catch(Exception e){}
            try{
                staffCommission = locParamRec.getStaffCommission().getValue();
            }catch(Exception e){}
        }
        if(!commission.isEmpty() && !staffCommission.isEmpty()){
            resultComm = "7".equals(customerStatus) ? staffCommission : commission;
        }       
        // =====================================================
        // Commission
        // =====================================================
        FtCommissionTypeRecord ftCommTypeRec = null;
        String taxCode = "";
        double commissionAmount = 0.0;
        List<CurrencyClass> list = new ArrayList<>();
        if(!resultComm.isEmpty()){
            try{
                ftCommTypeRec = new FtCommissionTypeRecord(
                        da.getRecord("FT.COMMISSION.TYPE", resultComm));
            }catch(Exception e){}
        }
        if(ftCommTypeRec!=null){
            try{
                taxCode = ftCommTypeRec.getTaxCode().getValue();
            }catch(Exception e){}
            try{
                list = ftCommTypeRec.getCurrency();
            }catch(Exception e){}
        }
        if (list != null) {
            try{
                String flat = list.get(0).getFlatAmt().getValue();
                commissionAmount = Double.parseDouble(flat);
            }catch(Exception e){}

        }       
        // =====================================================
        // Tax
        // =====================================================
        List<String> taxIds = new ArrayList<>();
        double taxRate = 0.0;
        
        if(!taxCode.isEmpty()){
            try{
                taxIds = da.selectRecords("BNK", "TAX", "", "WITH @ID LIKE '" + taxCode + "...'");
            }catch(Exception e){}
        }        
        if(taxIds != null && !taxIds.isEmpty()){
            for( String taxid : taxIds ){
                TaxRecord taxRec = new TaxRecord(da.getRecord("TAX", taxid)); 
                String rate = taxRec.getRate().getValue();
                if (rate != null) {
                    taxRate = Double.parseDouble(rate) / 100.0;
                }
            }
        }
//        calculate yearly charge for this year without charge.waive = yes
        double yearlyCharge = 0.0;
        if("YES".equalsIgnoreCase(chargeWaive)){
            yearlyCharge = 0.0;
            resultComm = "CHARGEWAIVE";
        }else{
            yearlyCharge = commissionAmount + (commissionAmount * taxRate);
        } 
        DecimalFormat df = new DecimalFormat("#.##");
        String yearlyChargeStr = df.format(yearlyCharge);//5.123 -> 5.12, 5.125->5.13
        // =====================================================
        // Get charge records
        // =====================================================
        List<String> lockerChargeRecIds = new ArrayList<>();
        String selectStmt = "WITH LOCKER.ACCT.ID EQ " +id+ " AND @ID LIKE '...-RENT-...'";        
        try{
            lockerChargeRecIds = da.selectRecords("BNK", "AA.JBL.LOCKER.CHARGE", "", selectStmt);
        }catch(Exception e){}        
        // =====================================================        
       //FREEZE/DECEASED account check
      // =====================================================
        
//        if the account is deceased, create a new charge record
        
        AaJblLockerChargeRecord due = null;
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
        if(isFreeze == true){
            try {
                due = new AaJblLockerChargeRecord(this);
                due.setLockerAcctId(id);
                due.setDueDate(today.format(T24_DATE_FORMAT));
                due.setStatus("Due");
                due.setChargeAmount(yearlyChargeStr);
                due.setChargeCode(resultComm);

                SynchronousTransactionData txn = new SynchronousTransactionData();
                txn.setVersionId("AA.JBL.LOCKER.CHARGE,OFS");
                txn.setFunction("INPUT");
                txn.setSourceId("LOCKER.OFS");
                txn.setUserName("INPUTT");
                txn.setTransactionId(id + "-RENT-" + currentYear);

                transactionData.add(txn);
                records.add(due.toStructure());

            } catch (Exception e) {}
        }else{
            // =====================================================
            // CASE 1: NO CHARGE RECORD
            // =====================================================
            
            if (lockerChargeRecIds == null || lockerChargeRecIds.isEmpty()){
                if (workingBalance >= yearlyCharge){
//                    deduct charge
                    try{
                        req = new AcChargeRequestRecord(this);
                        req.setRequestType("BOOK");
                        req.setDebitAccount(lockerAcctNo);
                        req.setStatus("PAID");
                        req.setRelatedRef("LOCKER CHARGE");
                        req.setExtraDetails("LOCKER DETAILS", 0);
                        req.setChargeCcy("BDT");
                        
                        ChargeCodeClass comm = new ChargeCodeClass();
                        comm.setChargeCode(resultComm);
                        req.setChargeCode(comm, 0);

                        SynchronousTransactionData txn = new SynchronousTransactionData();
                        txn.setVersionId("AC.CHARGE.REQUEST,LOCKER.OFS");
                        txn.setFunction("INPUT");
                        txn.setSourceId("LOCKER.OFS");
                        txn.setUserName("INPUTT");
                        
                        transactionData.add(txn);
                        records.add(req.toStructure());
                    }catch(Exception e){}
                    
                }else{
//                    create a due record for this year
                    try{
                        due = new AaJblLockerChargeRecord(this);
                        due.setLockerAcctId(id);
                        due.setDueDate(today.format(T24_DATE_FORMAT));
                        due.setStatus("Due");
                        due.setChargeAmount(yearlyChargeStr);
                        due.setChargeCode(resultComm);

                        SynchronousTransactionData txn = new SynchronousTransactionData();
                        txn.setVersionId("AA.JBL.LOCKER.CHARGE,OFS");
                        txn.setFunction("INPUT");
                        txn.setSourceId("LOCKER.OFS");
                        txn.setUserName("INPUTT");
                        txn.setTransactionId(id + "-RENT-" + currentYear);

                        transactionData.add(txn);
                        records.add(due.toStructure());
                    }catch(Exception e){}
                }
            }else{
                // =====================================================
                // CASE 2: EXISTING CHARGES (FIFO)
                // =====================================================
                
//              first create a due record for that year
                if(!lockerChargeRecIds.contains(id+"-RENT-"+currentYear)){
                    try {
                        due = new AaJblLockerChargeRecord(this);
                        due.setLockerAcctId(id);
                        due.setDueDate(today.format(T24_DATE_FORMAT));
                        due.setStatus("Due");
                        due.setChargeAmount(yearlyChargeStr);
                        due.setChargeCode(resultComm);

                        SynchronousTransactionData txn = new SynchronousTransactionData();
                        txn.setVersionId("AA.JBL.LOCKER.CHARGE,OFS");
                        txn.setFunction("INPUT");
                        txn.setSourceId("LOCKER.OFS");
                        txn.setUserName("INPUTT");
                        txn.setTransactionId(id + "-RENT-" + currentYear);

                        transactionData.add(txn);
                        records.add(due.toStructure());

                    } catch (Exception e) {}
            }
//              add current year record in chargeIds
                if (!lockerChargeRecIds.contains(id + "-RENT-" + currentYear)) {
                    lockerChargeRecIds.add(id + "-RENT-" + currentYear);
                } 
                
                // SORT by YEAR
                Collections.sort(lockerChargeRecIds,
                        (a, b) -> Integer.compare(extractYear(a), extractYear(b))); 
                for (String chargeId : lockerChargeRecIds) {
                    AaJblLockerChargeRecord locCrgRec = null;
                    try{
                        locCrgRec = new AaJblLockerChargeRecord(da.getRecord("A.JBL.LOCKER.CHARGE", chargeId));
                    }catch(Exception e){}
                    String chargeAmountStr = "";
                    String chrgeCode = "";
                    double chargeAmount = 0.0;
                    if(locCrgRec!=null){
                        try{
                            chargeAmountStr = locCrgRec.getChargeAmount().getValue();
                            chargeAmount = Double.parseDouble(chargeAmountStr);
                        }catch(Exception e){}
                        try{
                            chrgeCode = locCrgRec.getChargeCode().getValue();
                        }catch(Exception e){}
                    }
                    
                    if(workingBalance > chargeAmount){
                        try {
                            req = new AcChargeRequestRecord(this);
                            req.setRequestType("BOOK");
                            req.setDebitAccount(lockerAcctNo);
                            req.setStatus("PAID");
                            req.setRelatedRef("LOCKER CHARGE");
                            req.setExtraDetails("LOCKER DETAILS", 0);
                            
                            ChargeCodeClass comm = new ChargeCodeClass();
                            comm.setChargeCode(chrgeCode);
                            req.setChargeCode(comm, 0);

                            SynchronousTransactionData txn = new SynchronousTransactionData();
                            txn.setVersionId("AC.CHARGE.REQUEST,LOCKER.OFS");
                            txn.setFunction("INPUT");
                            txn.setSourceId("LOCKER.OFS");
                            txn.setUserName("INPUTT");

                            transactionData.add(txn);
                            records.add(req.toStructure());

                        } catch (Exception e) {}
                        workingBalance -= chargeAmount;
                        // DELETE after charge request
                        try {
                            AaJblLockerChargeRecord chargeRec =
                                    new AaJblLockerChargeRecord(this);

                            SynchronousTransactionData delTxn = new SynchronousTransactionData();
                            delTxn.setVersionId("AA.JBL.LOCKER.CHARGE,OFS");
                            delTxn.setFunction("REVERSE");
                            delTxn.setSourceId("LOCKER.OFS");
                            delTxn.setUserName("INPUTT");
                            delTxn.setTransactionId(chargeId);

                            transactionData.add(delTxn);
                            records.add(chargeRec.toStructure());

                        } catch (Exception e) {}
                        
                    }
                }
        }

    }
}
}