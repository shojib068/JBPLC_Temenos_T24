package com.temenos.t24;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import com.temenos.api.TField;
import com.temenos.api.TStructure;
import com.temenos.t24.api.complex.eb.servicehook.TransactionData;
import com.temenos.t24.api.complex.eb.templatehook.TransactionContext;
import com.temenos.t24.api.hook.system.RecordLifecycle;
import com.temenos.t24.api.records.aaarrangement.AaArrangementRecord;
import com.temenos.t24.api.records.aajbllockeracct.AaJblLockerAcctRecord;
import com.temenos.t24.api.records.aajbllockercharge.AaJblLockerChargeRecord;
import com.temenos.t24.api.records.aajbllockerdetails.AaJblLockerDetailsRecord;
import com.temenos.t24.api.records.aajbllockerparameter.AaJblLockerParameterRecord;
import com.temenos.t24.api.records.acchargerequest.AcChargeRequestRecord;
import com.temenos.t24.api.records.acchargerequest.ChargeCodeClass;
import com.temenos.t24.api.records.customer.CustomerRecord;
import com.temenos.t24.api.records.ofsrequestdetail.OfsRequestDetailRecord;
import com.temenos.t24.api.system.DataAccess;

/**
 * Routine Type: Auth.Routine
 * Routine Attached: AA.JBL.LOCKER.ACCT,INPUT
 *  Business Logic:
 * =====================================================
 * CASE: Generate Locker Charge After Account Update
 * =====================================================
 *
 * - This routine automatically creates locker charge records
 *   and AC.CHARGE.REQUEST entries after locker account update.
 *
 * =====================================================
 * Step 1: Extract Locker Account Information
 * =====================================================
 *
 * - Retrieve from AA.JBL.LOCKER.ACCT:
 *     → Account Number
 *     → Customer Number
 *     → Opening Date
 *     → Locker ID
 *
 * =====================================================
 * Step 2: Prevent Duplicate Charge Creation
 * =====================================================
 *
 * - Build transaction ID:
 *     → LOC-CRG-PAID-<LockerAcctId>-<Year>
 *
 * - If AA.JBL.LOCKER.CHARGE record already exists:
 *     → Exit routine (no duplicate charge created)
 *
 * =====================================================
 * Step 3: Fetch Customer Details
 * =====================================================
 *
 * - Retrieve CUSTOMER record using customer number
 * - Extract:
 *     → Customer Status
 *
 * =====================================================
 * Step 4: Fetch Locker Details
 * =====================================================
 *
 * - Retrieve AA.JBL.LOCKER.DETAILS using Locker ID
 * - Extract:
 *     → Locker Type
 *
 * =====================================================
 * Step 5: Fetch Charge Parameters
 * =====================================================
 *
 * - Retrieve AA.JBL.LOCKER.PARAMETER using Locker Type
 * - Extract:
 *     → Commission
 *     → Staff Commission
 *
 * =====================================================
 * Step 6: Fetch Arrangement Details
 * =====================================================
 *
 * - Retrieve AA.ARRANGEMENT using Locker Account ID
 * - Extract:
 *     → Product Group
 *
 * =====================================================
 * Step 7: Decide Commission Type
 * =====================================================
 *
 * - If Customer Status = "7":
 *     → Use Staff Commission
 * - Else:
 *     → Use Normal Commission
 *
 * =====================================================
 * Step 8: Build AC.CHARGE.REQUEST
 * =====================================================
 *
 * - Create charge request with:
 *     → Request Type = BOOK
 *     → Debit Account = Account Number
 *     → Status = PAID
 *     → Related Reference = LOCKER CHARGE
 *
 * - Charge Code Rules:
 *
 *   CASE A:
 *   - If Product Group = JBL.NKSP.GRP.RD
 *     OR Charge Waiver = YES
 *       → Add only LOCKEYCRG
 *
 *   CASE B:
 *   - Otherwise:
 *       → Add LOCKEYCRG
 *       → Add Commission Charge (resultComm)
 *
 * =====================================================
 * Step 9: Generate Charge Transaction ID
 * =====================================================
 *
 * - Format:
 *     CHG/<Last5DigitsOfAccount>/<Year>
 *
 * =====================================================
 * Step 10: Create OFS Transaction for Charge Request
 * =====================================================
 *
 * - Version: AC.CHARGE.REQUEST, LOCKER.OFS
 * - Function: INPUT
 * - Source: GCS
 * - User: INPUTT
 *
 * - Attach AC.CHARGE.REQUEST record to transaction list
 *
 * =====================================================
 * Step 11: Create Locker Charge Record
 * =====================================================
 *
 * - Populate AA.JBL.LOCKER.CHARGE:
 *     → Locker Account ID
 *     → Due Date
 *     → Payment Date
 *     → Status = Paid
 *     → Transaction Reference
 *
 * =====================================================
 * Step 12: Create OFS Transaction for Locker Charge
 * =====================================================
 *
 * - Version: AA.JBL.LOCKER.CHARGE, OFS
 * - Function: INPUT
 * - Source: BULK.OFS
 * - User: INPUTT
 *
 * - Attach Locker Charge record to transaction list
 *
 * =====================================================
 * NOTE
 * =====================================================
 *
 * - Multiple try-catch blocks suppress exceptions (risk of silent failure)
 * - Duplicate charge prevention is based on yearly transaction ID
 * - Commission logic depends on customer status and product group
 * - Two OFS transactions are generated:
 *     1. AC.CHARGE.REQUEST
 *     2. AA.JBL.LOCKER.CHARGE
 *
 * @author Kawsar
 *
 * @author kawsar
 *
 */
public class GbAFAaJblLockerChargePop extends RecordLifecycle {

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
        AcChargeRequestRecord acChargeReqRec = null;
        CustomerRecord cusRec = null;
    //    TransactionData txn1 = null;
        LocalDate openingDate = null;
        String id = "";
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
        String lockerAcctId = currentRecordId;

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
                aaArrangementRec = new AaArrangementRecord(
                        da.getRecord("AA.ARRANGEMENT", lockerAcctId));

                if (aaArrangementRec != null) {
                    productGroup = aaArrangementRec.getProductGroup().getValue();
                }
            } catch (Exception e) {}

            // COMMISSION DECISION
            if ("7".equals(customerStatus)) {
                resultComm = staffCommission;
            } else {
                resultComm = commission;
            }
            


            // =========================
            // AC.CHARGE.REQUEST
            // =========================
            try {
                acChargeReqRec = new AcChargeRequestRecord();

                acChargeReqRec.setRequestType("BOOK");
                acChargeReqRec.setDebitAccount(acctNo);
                acChargeReqRec.setStatus("PAID");
                acChargeReqRec.setRelatedRef("LOCKER CHARGE");
                acChargeReqRec.setExtraDetails("LOCKER DETAILS", 0);

                int idx = 0;

                if ("JBL.NKSP.GRP.RD".equals(productGroup) 
                        || "YES".equalsIgnoreCase(lockerAccRec.getChargeWaive().getValue())) {

                    // Only LOCKEYCRG
                    ChargeCodeClass key = new ChargeCodeClass();
                    key.setChargeCode("LOCKEYCRG");
                    acChargeReqRec.setChargeCode(key, idx++);

                } else {

                    // LOCKEYCRG
                    ChargeCodeClass key = new ChargeCodeClass();
                    key.setChargeCode("LOCKEYCRG");
                    acChargeReqRec.setChargeCode(key, idx++);

                    // + resultComm
                    if (resultComm != null && !resultComm.trim().isEmpty()) {
                        ChargeCodeClass comm = new ChargeCodeClass();
                        comm.setChargeCode(resultComm);
                        acChargeReqRec.setChargeCode(comm, idx++);
                    }
                }  

            } catch (Exception e) {}
            
//            id = "CHG" + acctNo.substring(acctNo.length() - 5) + String.format("%05d", openingDate.getYear());
            
            // OFS 1
            try {
                
                TransactionData   txn1 = new TransactionData();
                
                txn1.setFunction("INPUT");
                txn1.setTransactionId(id);
                txn1.setUserName("INPUTT");
                txn1.setNumberOfAuthoriser("0");
                txn1.setSourceId("GCS");                
                txn1.setVersionId("AC.CHARGE.REQUEST,LOCKER.OFS");
                
                transactionData.add(txn1);
                transactionData.get(0).getSourceId();
//                id = txn1.getTransactionId();
                currentRecords.add(acChargeReqRec.toStructure());
                
//                OfsRequestDetailRecord ofs = new OfsRequestDetailRecord(this);
//                String msgOut = ofs.getMsgOut().getValue();
            } catch (Exception e) {}

            // LOCKER CHARGE RECORD
            try {
                lockerChargeRec = new AaJblLockerChargeRecord();
                lockerChargeRec.setLockerAcctId(lockerAcctId);
                lockerChargeRec.setDueDate(openingDateStr);
                lockerChargeRec.setPaymentDate(openingDateStr);
                lockerChargeRec.setStatus("Paid");
//                lockerChargeRec.setTxnReference(id);
                
                
                
            } catch (Exception e) {}

            // OFS 2
            try {
               TransactionData txn2 = new TransactionData();
                txn2.setFunction("INPUT");
                txn2.setTransactionId(lockerAcctId + "-" + openingDate.getYear());
                txn2.setUserName("INPUTT");
                txn2.setNumberOfAuthoriser("0");
                txn2.setSourceId("BULK.OFS");
                txn2.setVersionId("AA.JBL.LOCKER.CHARGE,OFS");
                
                transactionData.add(txn2);
                currentRecords.add(lockerChargeRec.toStructure());

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}

