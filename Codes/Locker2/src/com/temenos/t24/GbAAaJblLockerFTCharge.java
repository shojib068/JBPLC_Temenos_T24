package com.temenos.t24;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import com.temenos.api.LocalRefGroup;
import com.temenos.api.LocalRefList;
import com.temenos.api.TStructure;
import com.temenos.t24.api.complex.eb.servicehook.TransactionData;
import com.temenos.t24.api.complex.eb.templatehook.TransactionContext;
import com.temenos.t24.api.hook.system.RecordLifecycle;
import com.temenos.t24.api.records.aaarrangement.AaArrangementRecord;
import com.temenos.t24.api.records.aagblockerparam.AaGbLockerParamRecord;
import com.temenos.t24.api.records.aajbllockeracct.AaJblLockerAcctRecord;
import com.temenos.t24.api.records.aajbllockercharge.AaJblLockerChargeRecord;
import com.temenos.t24.api.records.aajbllockerdetails.AaJblLockerDetailsRecord;
import com.temenos.t24.api.records.aajbllockerparameter.AaJblLockerParameterRecord;
import com.temenos.t24.api.records.customer.CustomerRecord;
import com.temenos.t24.api.records.fundstransfer.CommissionTypeClass;
import com.temenos.t24.api.records.fundstransfer.FundsTransferRecord;
import com.temenos.t24.api.system.DataAccess;

/**
 * TODO: Document me!
 *
 * @author kawsar
 *
 */
public class GbAAaJblLockerFTCharge extends RecordLifecycle{

    private static final DateTimeFormatter T24_DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMdd");

    @Override
    public void postUpdateRequest(String application, String currentRecordId,
            TStructure currentRecord,
            List<TransactionData> transactionData,
            List<TStructure> currentRecords,
            TransactionContext transactionContext) {

        AaJblLockerAcctRecord lockerAccRec = new AaJblLockerAcctRecord(currentRecord);
        DataAccess da = new DataAccess(this);

        AaJblLockerChargeRecord lockerChargeRec = null;
        AaJblLockerDetailsRecord locDetRec = null;
        AaJblLockerParameterRecord locParamRec = null;
        AaArrangementRecord aaArrangementRec = null;
        FundsTransferRecord ftRecord = null;
        CustomerRecord cusRec = null;
        
        LocalDate openingDate = null;
//        String id = "";
        String acctNo = "";
        String customerNo = "";
        String openingDateStr = "";
        String lockerId = "";
        String customerStatus = "";
        String lockerType = "";
        String commission = "";
        String staffCommission = "";
        String resultComm = "";
        String productGroup = "";
        String commissionAmountStr = "";
        String lockerAcctId = currentRecordId;
        String payDeString = "";
        
        if (lockerAccRec != null) {
            

            try {
                acctNo = lockerAccRec.getAcctNo().getValue();
            } catch (Exception e) {}

            try {
                customerNo = lockerAccRec.getCustomer().getValue();
            } catch (Exception e) {}

            try {
                openingDateStr = lockerAccRec.getOpeningDate().getValue();
                openingDate = LocalDate.parse(openingDateStr, T24_DATE_FORMAT);
            } catch (Exception e) {}

            try {
                lockerId = lockerAccRec.getLockerId().getValue();
            } catch (Exception e) {}
            
//          duplicate id check
            
            try {
                String txnId = lockerAcctId + "-" + openingDate.getYear();
                da.getRecord("AA.JBL.LOCKER.CHARGE", txnId);
                return;
            } catch (Exception e) {}          
            



            // CUSTOMER
            try {
                cusRec = new CustomerRecord(da.getRecord("CUSTOMER", customerNo));
                if (cusRec != null) {
                    customerStatus = cusRec.getCustomerStatus().getValue();
                }
            } catch (Exception e) {}

            // LOCKER DETAILS
            try {
                locDetRec = new AaJblLockerDetailsRecord(
                        da.getRecord("AA.JBL.LOCKER.DETAILS", lockerId));

                if (locDetRec != null) {
                    lockerType = locDetRec.getLockerType().getValue();
                }
            } catch (Exception e) {}

            // LOCKER PARAMETER
            try {
                locParamRec = new AaJblLockerParameterRecord(
                        da.getRecord("AA.JBL.LOCKER.PARAMETER", lockerType));

                if (locParamRec != null) {
                    commission = locParamRec.getCommission().getValue();
                    staffCommission = locParamRec.getStaffCommission().getValue();
                }
            } catch (Exception e) {}

            // ARRANGEMENT
            try {
                List<String> aaArrangementId = da.selectRecords("BNK", "AA.ARRANGEMENT", "", 
                        " WITH LINKED.APPL.ID EQ "+acctNo);

                if (aaArrangementId != null) {
                    for(String ids : aaArrangementId){
                        aaArrangementRec = new AaArrangementRecord(da.getRecord("AA.ARRANGEMENT", ids));
                        if(aaArrangementRec!=null){
                            try{
                                productGroup = aaArrangementRec.getProductGroup().getValue();
                            }catch(Exception e){}
                        }
                            
                    }
                    
                }
            } catch (Exception e) {}

            // COMMISSION DECISION
            if ("7".equals(customerStatus)) {
                resultComm = staffCommission;
            } else {
                resultComm = commission;
            }
            
            
//            find Flat Amount for Locker Key Charge
            AaGbLockerParamRecord gbLocParamRec = null;
            try{
                gbLocParamRec = new AaGbLockerParamRecord(da.getRecord("AA.GB.LOCKER.PARAM", "SYSTEM"));
            }catch(Exception e){}
            commissionAmountStr = gbLocParamRec.getLockerKeyCharge().getValue();

            // =========================
            // FUNDS.TRANSFER,LOCKER.OPENING.CRG.OFS
            // =========================
            try {
                ftRecord = new FundsTransferRecord(this);
                ftRecord.setTransactionType("ACLK");
                ftRecord.setDebitAcctNo(acctNo);
                ftRecord.setDebitAmount(commissionAmountStr);//locker key charge
                ftRecord.setDebitCurrency("BDT");
                
                ftRecord.setCreditAcctNo("BDT17608");
                ftRecord.setCreditCurrency("BDT");
                ftRecord.setChargeCode("DEBIT PLUS CHARGES");
                
                if ("JBL.NKSP.GRP.RD".equals(productGroup) 
                        || "YES".equalsIgnoreCase(lockerAccRec.getChargeWaive().getValue())) {                   
//                  charge waive or nari kollan not 
                    CommissionTypeClass comm = new CommissionTypeClass();
                    comm.setCommissionType("CHARGEWAIVE");
                    ftRecord.setCommissionType(comm, 0);
                }else{ 

                    CommissionTypeClass comm = new CommissionTypeClass();
                    comm.setCommissionType(resultComm);
                    ftRecord.setCommissionType(comm, 0);
                }
                ftRecord.getLocalRefField("LT.OLD.PO.NO").setValue(lockerAcctId);
                ftRecord.setOrderingBank("JBL", 0);
                ftRecord.setPaymentDetails("LOCKER OPENING CHARGE", 0);
            } catch (Exception e) {}
            
            // OFS 1
            try {
                
                TransactionData   txn1 = new TransactionData();
                
                txn1.setFunction("INPUT");
                txn1.setUserName("INPUTT");
                txn1.setNumberOfAuthoriser("0");
                txn1.setSourceId("GCS");                
                txn1.setVersionId("FUNDS.TRANSFER,LOCKER.OPENING.CRG.OFS");
                
                transactionData.add(txn1);
                currentRecords.add(ftRecord.toStructure());
            } catch (Exception e) {}
            
            // LOCKER ACCT KEY CHARGE RECCORD
            AaJblLockerAcctRecord keyCrgRec = null;
            try{
                keyCrgRec = new AaJblLockerAcctRecord(this);
                
                keyCrgRec.setKeyCharge(commissionAmountStr);
            }catch(Exception e){}
            
            //OFS 3
            try{

                    TransactionData txn3 = new TransactionData();
                    txn3.setFunction("INPUT");
                    txn3.setTransactionId(lockerAcctId);
                    txn3.setUserName("INPUTT");
                    txn3.setNumberOfAuthoriser("0");
                    txn3.setSourceId("LOCKER.OFS");
                    txn3.setVersionId("AA.JBL.LOCKER.ACCT,OFS");
                     
                     transactionData.add(txn3);
                     currentRecords.add(keyCrgRec.toStructure());
            }catch(Exception e){}

            
        }
    }
}

