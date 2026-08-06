package com.temenos.t24;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import com.ibm.icu.text.SimpleDateFormat;
import com.temenos.t24.api.arrangement.accounting.Contract;
import com.temenos.t24.api.complex.eb.enquiryhook.EnquiryContext;
import com.temenos.t24.api.complex.eb.enquiryhook.FilterCriteria;
import com.temenos.t24.api.hook.system.Enquiry;
import com.temenos.t24.api.records.aaactivityhistory.AaActivityHistoryRecord;
import com.temenos.t24.api.records.aaactivityhistory.ActivityRefClass;
import com.temenos.t24.api.records.aaactivityhistory.EffectiveDateClass;
import com.temenos.t24.api.records.aaarrangement.AaArrangementRecord;

import com.temenos.t24.api.records.aaprddesaccount.AaPrdDesAccountRecord;
import com.temenos.t24.api.records.aaprddeschangeproduct.AaPrdDesChangeProductRecord;
import com.temenos.t24.api.records.aaprddesinterest.AaPrdDesInterestRecord;
import com.temenos.t24.api.records.aaprddeslimit.AaPrdDesLimitRecord;
import com.temenos.t24.api.records.aaprddespaymentschedule.AaPrdDesPaymentScheduleRecord;
import com.temenos.t24.api.records.aaprddessettlement.AaPrdDesSettlementRecord;
import com.temenos.t24.api.records.aaprddestermamount.AaPrdDesTermAmountRecord;
import com.temenos.t24.api.records.drawings.DrawingsRecord;
import com.temenos.t24.api.records.lctypes.LcTypesRecord;
import com.temenos.t24.api.records.letterofcredit.LetterOfCreditRecord;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.system.Session;

/**
 * TODO: Document me!
 *
 * @author abdul.khaleque 
 *  ------- TF 678 ----------
 *  FDBP Foreign Register
 *  ENQ -- JBL.ENQ.NOF.TRF.FDBP.FORIGN 
 *  SS -- NOFILE.ENQ.TRF.FDBP.FORIGN 
 *  EB.API -- RpJblTrfFdbpForign 
 *  Date : 30 Mar 2026
 */
public class RpJblTrfFdbpForign extends Enquiry {

    public static String addDaysToT24Date(String t24Date, int daysToAdd) {
        // 1. Define the T24 format
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");

        // 2. Parse the String into a LocalDate object
        LocalDate date = LocalDate.parse(t24Date, formatter);

        // 3. Add the days
        LocalDate newDate = date.plusDays(daysToAdd);

        // 4. Format it back to YYYYMMDD string
        return newDate.format(formatter);
    }

    public static boolean isValidYYYYMMDD(String dateStr) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
            sdf.setLenient(false);
            sdf.parse(dateStr);
            return true;
        } catch (ParseException e) {
            return false;
        }
    }

    public static String loanDisbursement(String arrangementId, DataAccess da) {
        String ret = "";

        AaActivityHistoryRecord aaActivityHistoryRecord = null;
        double totalLoanDisbursementamount = 0.0;
        String loanDisbursementDates = "00000000";
        List<EffectiveDateClass> effectiveDates = null;
        List<ActivityRefClass> activityRef = null;
        try {
            aaActivityHistoryRecord = new AaActivityHistoryRecord(da.getRecord("AA.ACTIVITY.HISTORY", arrangementId));
        } catch (Exception e) {
        }
        if (aaActivityHistoryRecord == null) {
            return ret;
        }
        try {
            effectiveDates = aaActivityHistoryRecord.getEffectiveDate();
        } catch (Exception e) {
        }
        for (EffectiveDateClass effectiveDate : effectiveDates) {
            activityRef = effectiveDate.getActivityRef();

            for (ActivityRefClass activity : activityRef) {
                String act = "";
                String actStatus = "";
                String amount = "";
                String date = "";
                try {
                    act = activity.getActivity().getValue();
                } catch (Exception e) {
                }
                try {
                    actStatus = activity.getActStatus().getValue();
                } catch (Exception e) {
                }

                if ((act.equalsIgnoreCase("LENDING-DISBURSE-COMMITMENT")
                        || act.equalsIgnoreCase("LENDING-AUTO.DISBURSE-COMMITMENT")
                        || act.equalsIgnoreCase("ACCOUNTS-DEBIT-ARRANGEMENT"))
                        && (actStatus.equalsIgnoreCase("AUTH") || actStatus.equalsIgnoreCase("ADJREPLAY-AUT"))) {
                    try {

                        amount = activity.getActivityAmt().getValue();
                    } catch (Exception e) {
                    }
                    try {
                        date = effectiveDate.getEffectiveDate().getValue();
                    } catch (Exception e) {
                    }
                    totalLoanDisbursementamount += Double.parseDouble(amount);
                    if (Integer.parseInt(loanDisbursementDates) < Integer.parseInt(date)) {
                        loanDisbursementDates = date;
                    }

                }

            }
        }
        if (loanDisbursementDates.equalsIgnoreCase("00000000")) {
            ret = "" + "*" + String.valueOf(totalLoanDisbursementamount);
        } else {
            ret = loanDisbursementDates + "*" + String.valueOf(totalLoanDisbursementamount);
        }

        return ret;
    }

    public static String dueDateCalc(String creditType, DataAccess da, String openingDate, String tenor) {

        String dueDate = "";
        String payType = "";

        Boolean isSight = false;
        Boolean isUsance = false;

        LcTypesRecord lcTypesRecord = null;

        try {
            lcTypesRecord = new LcTypesRecord(da.getRecord("LC.TYPES", creditType));
        } catch (Exception e) {
        }

        payType = lcTypesRecord.getPayType().getValue();

        isSight = "P".equalsIgnoreCase(payType);
        isUsance = "A".equalsIgnoreCase(payType);

        if (isSight) {
            dueDate = addDaysToT24Date(openingDate, 21);
        }

        if (isUsance) {
            int tenorInt = Integer.parseInt(tenor);
            dueDate = addDaysToT24Date(openingDate, tenorInt);
        }

        return dueDate;
    }

    public static Boolean isForign(String creditType, DataAccess da) {

        
        String forign = "";

        Boolean isFor = false;
        LcTypesRecord lcTypesRecord = null;

        try {
            lcTypesRecord = new LcTypesRecord(da.getRecord("LC.TYPES", creditType));
        } catch (Exception e) {
        }
        if(lcTypesRecord==null){
            return isFor;
        }
        
        forign = lcTypesRecord.getLocalRefField("LT.LCTP.LOC.FRG").getValue();

        isFor = "FOREIGN".equalsIgnoreCase(forign);
        
        return isFor;
    }

    
    public static String extractAmount(String input) {
        if (input == null)
            return "";

        return input.replaceAll("[^0-9.]", "");
    }

    @Override
    public List<String> setIds(List<FilterCriteria> filterCriteria, EnquiryContext enquiryContext) {

        DataAccess da = new DataAccess(this);
        Session ss = new Session(this);
        String coCode = ss.getCompanyId();
        Contract contract = new Contract(this);

        String bookingDate = "";
        String drawingId = "";
        Boolean bookinDateflag = false;
        Boolean isFrgdr = false;
        
        String dateFieldOperand = "";
        String lcId = "";
        String benCusNo = "";
        String property = "COMMITMENT";

        String product = "AL.JBL.FDBP.GARMENTS.SIGHT.LN AL.JBL.FDBP.GARMENTS.USNC.LN AL.JBL.FDBP.OTHERS.SIGHT.LN AL.JBL.FDBP.OTHERS.USNC.LN";
        String arrStatus = "CURRENT MATURED EXPIRED CLOSE AUTH RESTORE-AUTH PENDING.CLOSURE";

        String inputDate = "";
        String fromDate = "";
        String toDate = "";
        Boolean trueFalseinpDt = false;
        Boolean trueFalsefromDate = false;
        Boolean trueFalsetoDate = false;
        String[] dates = null;

        List<String> arrList = new ArrayList<>();
        List<String> finalRet = new ArrayList<>();

        AaArrangementRecord aaArrangementRecord = null;
        AaPrdDesTermAmountRecord aaArrTermAmountRecord = null;

        DrawingsRecord drawingsRecord = null;
        LetterOfCreditRecord letterOfCreditRecord = null;

        for (FilterCriteria filtr : filterCriteria) {
            String SelectfieldName = filtr.getFieldname();
            switch (SelectfieldName) {
            case "BOOKING.DATE":
                bookingDate = filtr.getValue();
                dateFieldOperand = filtr.getOperand();
                break;
            case "DRAW.ID":
                drawingId = filtr.getValue();
                break;
            case "LC.ID":
                lcId = filtr.getValue();
                break;
            case "BEN.CUSTNO":
                benCusNo = filtr.getValue();
                break;
            default:
                break;
            }
        } // User Selection

        // Date trim
        if (!bookingDate.isEmpty()) {
            dates = bookingDate.split(" ");
            if (dates.length == 1) {
                inputDate = dates[0];
                trueFalseinpDt = isValidYYYYMMDD(inputDate);
            } else if (dates.length == 2) {
                fromDate = dates[0];
                toDate = dates[1];
                trueFalsefromDate = isValidYYYYMMDD(fromDate);
                trueFalsetoDate = isValidYYYYMMDD(toDate);
            }
        }

        // if user give Wrong Operation
        if (!((dates.length == 1 && Integer.parseInt(dateFieldOperand) == 1)
                || (dates.length == 2 && Integer.parseInt(dateFieldOperand) == 2))) {
            // || dates.length==1 && Integer.parseInt(dateFieldOperand)==8
            return finalRet;
        }
        /* 3. Validate Date Logic (Return empty if invalid) */
        boolean isSingleDateInvalid = !inputDate.isEmpty() && !trueFalseinpDt;
        boolean isRangeInvalid = !fromDate.isEmpty() && !toDate.isEmpty()
                && (!trueFalsefromDate || !trueFalsetoDate || Integer.parseInt(fromDate) > Integer.parseInt(toDate));

        if (!bookingDate.isEmpty() && (isSingleDateInvalid || isRangeInvalid)) {
            return finalRet;
        }

        try {
            arrList = da.selectRecords("BNK", "AA.ARRANGEMENT", "",
                    " WITH PRODUCT EQ " + product + " AND ARR.STATUS EQ " + arrStatus + " AND CO.CODE EQ " + coCode);
        } catch (Exception e) {
        }
        if (arrList.size() == 0)
            return finalRet;

        for (String arrid : arrList) {
            bookinDateflag = false;
            aaArrTermAmountRecord = null;
            String termId = "";
            String loanOpenDate = "";
            String recordDrawingId = "";
            String RecordbenCustNo = "";
            String loanDis = "";
            String RecordlcId = "";
            String recordBookingDate = "";
            String fdbpNo = "";
            String fcType = "";
            String docPresentedOn = "";
            String documentAmount = "";
            String tfPureAmtCur = "";
            String tfPureAmt = "";
            Double floatingValue = 0.0;
            String exchangeRate = "";
            Double enqTk = 0.0;
            String eXPFormNo = "";
            String dupExpReportDate = "";
            String inputter = "";
            String tripExpReportDate = "";
            String authoriser = "";
            String dateRealised = "";
            String lcType = "";
            String lcCreditType = "";
            String loanId = "";

            // LC
            String drawer = "";
            String drawee = "";
            String steamer = "";
            String lcNo = "";
            String expLcDate = "";
            String value = "";
            String lcValueAvailable = "";
            String shipmentDate = "";
            String expiry = "";
            String openingBank = "";
            String collectingBank = "";
            String tenor = "";
            String dueDate = "";

            // Documents
            String invoiceNo = "";
            String invoiceDate = "";
            String awbNo = "";
            String awbDate = "";

            try {
                aaArrangementRecord = new AaArrangementRecord(da.getRecord("AA.ARRANGEMENT", arrid));
            } catch (Exception e) {
            }

            if (aaArrangementRecord == null)
                continue;
            
            try {
                loanId = aaArrangementRecord.getLinkedAppl(0).getLinkedApplId().getValue();
            } catch (Exception e) {
            }

            try {
                loanOpenDate = aaArrangementRecord.getStartDate().getValue();
            } catch (Exception e) {
            }

            contract.setContractId(arrid);

            termId = returnLatestDateId(arrid, property, coCode, da, contract);
            try {
                aaArrTermAmountRecord = new AaPrdDesTermAmountRecord(da.getRecord("AA.ARR.TERM.AMOUNT", termId));
            } catch (Exception e) {
            }

            if (aaArrTermAmountRecord == null)
                continue;

            try {
                recordDrawingId = aaArrTermAmountRecord.getLocalRefField("LT.LC.DR.NO").getValue();
            } catch (Exception e) {
            }

            if (recordDrawingId.isEmpty())
                continue;

            if (!drawingId.isEmpty()) {
                if (!recordDrawingId.equalsIgnoreCase(drawingId)) {
                    continue;
                }
            }

            RecordlcId = recordDrawingId.substring(0, 12);
            if (!lcId.isEmpty()) {
                if (!RecordlcId.equalsIgnoreCase(lcId)) {
                    continue;
                }
            }

            try {
                drawingsRecord = new DrawingsRecord(da.getRecord("DRAWINGS", recordDrawingId));
            } catch (Exception e) {

            }
            if (drawingsRecord == null) {
                try {
                    drawingsRecord = new DrawingsRecord(da.getHistoryRecord("DRAWINGS", recordDrawingId));
                } catch (Exception e) {
                }
            }

            if (drawingsRecord == null) {
                continue;
            }

            try {
                recordBookingDate = drawingsRecord.getBookingDate().getValue();
            } catch (Exception e) {
            }

            // Date Filtering Logic
            if (!bookingDate.isEmpty() && !recordBookingDate.isEmpty()) {
                int operand = Integer.parseInt(dateFieldOperand);
                if (operand == 1) { // Equals
                    if (trueFalseinpDt && recordBookingDate.equals(inputDate)) {
                        bookinDateflag = true;
                    }
                } else if (operand == 2) { // Between
                    int drawDate = Integer.parseInt(recordBookingDate);
                    if (drawDate >= Integer.parseInt(fromDate) && drawDate <= Integer.parseInt(toDate)) {
                        bookinDateflag = true;
                    }
                }
            }

            if (!bookinDateflag)
                continue;
            
            try {
                lcCreditType = drawingsRecord.getLcCreditType().getValue();
            } catch (Exception e) {
            }
            
            isFrgdr=isForign(lcCreditType,da);
            
            if(!isFrgdr){
                continue;
            }
            

            loanDis = loanDisbursement(arrid, da);

            try {
                fdbpNo = drawingsRecord.getLocalRefField("LT.TF.ELC.COLNO").getValue();
            } catch (Exception e) {
            }

            try {
                fcType = drawingsRecord.getDrawCurrency().getValue();
            } catch (Exception e) {
            }

            try {
                docPresentedOn = drawingsRecord.getPresentationDate().getValue();
            } catch (Exception e) {
            }

            try {
                documentAmount = drawingsRecord.getDocumentAmount().getValue();
            } catch (Exception e) {
            }

            try {
                tfPureAmtCur = aaArrTermAmountRecord.getLocalRefField("LT.TF.PUR.AMT").getValue();
                tfPureAmt = extractAmount(tfPureAmtCur);
            } catch (Exception e) {
            }
            if(tfPureAmtCur.isEmpty()){
                tfPureAmt="0";
            }
            floatingValue = Double.parseDouble(documentAmount) - Double.parseDouble(tfPureAmt);

            try {
                exchangeRate = aaArrTermAmountRecord.getLocalRefField("LT.TF.EXCH.RATE").getValue();
            } catch (Exception e) {
            }
            if(exchangeRate.isEmpty()){
                tfPureAmt="0.0";
            }

            enqTk = Double.parseDouble(tfPureAmt) * Double.parseDouble(exchangeRate);

            // Documents

            try {
                invoiceNo = drawingsRecord.getLocalRefField("LT.TF.INVOIC.NO").getValue();
            } catch (Exception e) {
            }

            try {
                invoiceDate = drawingsRecord.getLocalRefField("LT.TF.INVOIC.DT").getValue();
            } catch (Exception e) {
            }
            try {
                awbNo = drawingsRecord.getLocalRefField("LT.TF.LND.AWBNO").getValue();
            } catch (Exception e) {
            }
            try {
                awbDate = drawingsRecord.getLocalRefField("LT.TF.LND.AWBDT").getValue();
            } catch (Exception e) {
            }

            try {
                eXPFormNo = drawingsRecord.getLocalRefField("LT.TF.EXP.FM.NO").getValue();
            } catch (Exception e) {
            }

            try {
                dupExpReportDate = drawingsRecord.getLocalRefField("LT.TF.EXP2.DATE").getValue();
            } catch (Exception e) {
            }

            try {
                inputter = drawingsRecord.getInputter(0);
                String[] parts = inputter.split("_");
                inputter = parts[1];
            } catch (Exception e) {
            }

            try {
                tripExpReportDate = drawingsRecord.getLocalRefField("LT.TF.EXP3.DATE").getValue();
            } catch (Exception e) {
            }

            try {
                authoriser = drawingsRecord.getAuthoriser();
                String[] parts = authoriser.split("_");
                authoriser = parts[1];
            } catch (Exception e) {
            }

            try {
                dateRealised = drawingsRecord.getDebitValue().getValue();
            } catch (Exception e) {
            }

            try {
                letterOfCreditRecord = new LetterOfCreditRecord(da.getRecord("LETTER.OF.CREDIT", RecordlcId));
            } catch (Exception e) {
            }
            if (letterOfCreditRecord == null) {
                try {
                    letterOfCreditRecord = new LetterOfCreditRecord(
                            da.getHistoryRecord("LETTER.OF.CREDIT", RecordlcId));
                } catch (Exception e) {
                }

            }
            if (letterOfCreditRecord == null) {
                continue;
            }

            try {
                RecordbenCustNo = letterOfCreditRecord.getBeneficiaryCustno().getValue();
            } catch (Exception e) {
            }

            if (!benCusNo.isEmpty()) {
                if (!RecordbenCustNo.equalsIgnoreCase(benCusNo)) {
                    continue;
                }
            }

            try {
                tenor = letterOfCreditRecord.getLocalRefField("LT.TF.LC.TENOR").getValue();
            } catch (Exception e) {
            }

            try {
                drawer = letterOfCreditRecord.getBeneficiary(0).getValue();
            } catch (Exception e) {
            }
            try {
                drawee = letterOfCreditRecord.getApplicant(0).getValue();
            } catch (Exception e) {
            }
            try {
                steamer = letterOfCreditRecord.getModeOfShipment().getValue();
            } catch (Exception e) {
            }
            try {
                lcNo = letterOfCreditRecord.getIssBankRef().getValue();
            } catch (Exception e) {
            }
            try {
                expLcDate = letterOfCreditRecord.getIssueDate().getValue();
            } catch (Exception e) {
            }
            try {
                value = letterOfCreditRecord.getLcAmount().getValue();
            } catch (Exception e) {
            }

            try {
                lcValueAvailable = letterOfCreditRecord.getLiabilityAmt().getValue();
            } catch (Exception e) {
            }

            try {
                shipmentDate = letterOfCreditRecord.getLatestShipment().getValue();
            } catch (Exception e) {
            }

            try {
                expiry = letterOfCreditRecord.getExpiryDate().getValue();
            } catch (Exception e) {
            }

            try {
                openingBank = letterOfCreditRecord.getIssuingBank().get(0).getValue();
            } catch (Exception e) {
            }

            try {
                collectingBank = letterOfCreditRecord.getAdvisingBk().get(0).getValue();
            } catch (Exception e) {
            }
            if (collectingBank.isEmpty()) {
                collectingBank = letterOfCreditRecord.getAdvisingBkCustno().getValue();
            }
            try {
                lcType = drawingsRecord.getLocalRefField("LT.TF.LC.TYPE").getValue();
            } catch (Exception e) {
            }

            if (lcType.equalsIgnoreCase("EXP") || lcType.equalsIgnoreCase("EXPB")) {
                
                dueDate = dueDateCalc(lcCreditType, da, loanOpenDate, tenor);
            }

            String finalString =loanId +"*" +  loanOpenDate + "*" + loanDis + "*" + fdbpNo + "*" + drawer + "*" + drawee + "*"
                    + fcType + "*" + docPresentedOn + "*" + documentAmount + "*" + floatingValue + "*" + exchangeRate
                    + "*" + enqTk + "*" + steamer + "*" + invoiceNo + "*" + invoiceDate + "*" + awbNo + "*" + awbDate
                    + "*" + lcNo + "*" + expLcDate + "*" + value + "*" + lcValueAvailable + "*" + shipmentDate + "*"
                    + expiry + "*" + openingBank + "*" + collectingBank + "*" + dueDate + "*" + eXPFormNo + "*"
                    + dupExpReportDate + "*" + inputter + "*" + tripExpReportDate + "*" + authoriser + "*"
                    + dateRealised + "*" + documentAmount + "*" + recordDrawingId + "*" + tfPureAmtCur;
            finalRet.add(finalString);

        }
        return finalRet;
    }

    private static String returnLatestDateId(String arrangementId, String propertyName, String companyCode,
            DataAccess da, Contract contract) {

        /*
         * !!!Instructions from the dev!!! Send property as parameter shown as
         * below
         */

        /**
         * "Application
         * Name"-------------------------------------------"Expected String
         * Parameter"
         * AA.ARR.ACCOUNT------------------------------------------------ACCOUNT
         * AA.ARR.TERM.AMOUNT--------------------------------------------COMMITMENT
         * AA.ARR.INTEREST-----------------------------------------------PRINCIPALINT
         * -----------------------------------------------PENALTYINT
         * -----------------------------------------------DEPOSITINT
         * -----------------------------------------------REDEEMINT
         * AA.ARR.LIMIT--------------------------------------------------LIMIT
         * AA.ARR.CHANGE.PRODUCT-----------------------------------------RENEWAL
         * AA.ARR.PAYMENT.SCHEDULE---------------------------------------SCHEDULE
         * AA.ARR.SETTLEMENT---------------------------------------------SETTLEMENT
         **/

        String processedId = "";
        String partialQueryString = "";
        String cmIdComp1 = "";
        String cmIdComp2 = "";
        String cmIdComp3 = "";
        String cmRecordStatus = "";

        List<String> listOfInterestProperties = Arrays.asList("PRINCIPALINT", "PENALTYINT", "DEPOSITINT", "REDEEMINT");

        List<String> aaArrAccountRecords = new ArrayList<String>();
        List<String> aaArrTermAmountRecords = new ArrayList<String>();
        List<String> aaArrInterestRecords = new ArrayList<String>();
        List<String> aaArrLimitRecords = new ArrayList<String>();
        List<String> aaArrChangeProductRecords = new ArrayList<String>();
        List<String> aaArrPaymentScheduleRecords = new ArrayList<String>();
        List<String> aaArrSettlementRecords = new ArrayList<String>();

        AaPrdDesAccountRecord aaPrdDesAccountRecord = null;
        AaPrdDesTermAmountRecord aaPrdDesTermAmountRecord = null;
        AaPrdDesInterestRecord aaPrdDesInterestRecord = null;
        AaPrdDesLimitRecord aaPrdDesLimitRecord = null;
        AaPrdDesChangeProductRecord aaPrdDesChangeProductRecord = null;
        AaPrdDesPaymentScheduleRecord aaPrdDesPaymentScheduleRecord = null;
        AaPrdDesSettlementRecord aaPrdDesSettlementRecord = null;

        /** AA.ARR.ACCOUNT */
        if (propertyName.equalsIgnoreCase("ACCOUNT")) {

            /*
             * Check of whether latest date record is an authorized record or
             * not
             */
            contract.setContractId(arrangementId);
            try {
                aaPrdDesAccountRecord = new AaPrdDesAccountRecord(contract.getConditionForProperty(propertyName));
                try {
                    cmRecordStatus = aaPrdDesAccountRecord.getRecordStatus();
                } catch (Exception e) {
                }

                /* is a live record */
                if (cmRecordStatus.isEmpty()) {
                    try {
                        cmIdComp1 = aaPrdDesAccountRecord.getIdComp1().getValue();
                    } catch (Exception e) {
                    }
                    try {
                        cmIdComp2 = aaPrdDesAccountRecord.getIdComp2().getValue();
                    } catch (Exception e) {
                    }
                    try {
                        cmIdComp3 = aaPrdDesAccountRecord.getIdComp3().getValue();
                    } catch (Exception e) {
                    }
                    if ((!cmIdComp1.isEmpty()) && (!cmIdComp2.isEmpty()) && (!cmIdComp3.isEmpty())) {
                        processedId = cmIdComp1 + "-" + cmIdComp2 + "-" + cmIdComp3;
                    }
                }
                /* not a live record */
                else {
                    partialQueryString = arrangementId + "-" + propertyName.toUpperCase() + "...";

                    try {
                        aaArrAccountRecords = da.selectRecords("BNK", "AA.ARR.ACCOUNT", "",
                                " WITH @ID LIKE " + partialQueryString + " AND CO.CODE EQ " + companyCode);

                        if (aaArrAccountRecords.size() > 0) {

                            /** order by descending order */
                            Collections.sort(aaArrAccountRecords, (a, b) -> {
                                String[] partsA = a.split("-");
                                String[] partsB = b.split("-");

                                String lastA = partsA[partsA.length - 1];
                                String lastB = partsB[partsB.length - 1];

                                String[] dateVerA = lastA.split("\\.");
                                String[] dateVerB = lastB.split("\\.");

                                String dateA = dateVerA[0];
                                String dateB = dateVerB[0];

                                int verA = dateVerA.length > 1 ? Integer.parseInt(dateVerA[1]) : 0;
                                int verB = dateVerB.length > 1 ? Integer.parseInt(dateVerB[1]) : 0;

                                int dateCompare = dateB.compareTo(dateA);
                                if (dateCompare != 0) {
                                    return dateCompare;
                                }
                                return Integer.compare(verB, verA);
                            });
                        }

                        /** retrieving latest date record id */
                        int count = 0;

                        for (String currId : aaArrAccountRecords) {
                            if (count > 0) {
                                break;
                            } else {
                                try {
                                    aaPrdDesAccountRecord = new AaPrdDesAccountRecord(
                                            da.getRecord("AA.ARR.ACCOUNT", currId));
                                    try {
                                        String currRecordStatus = aaPrdDesAccountRecord.getRecordStatus();
                                        if (currRecordStatus.isEmpty()) {
                                            count++;
                                            processedId = currId;
                                        }
                                    } catch (Exception e) {
                                    }
                                } catch (Exception e) {
                                }
                            }
                        }
                    } catch (Exception e) {
                    }
                }
            } catch (Exception e) {
            }
        }

        /** AA.ARR.TERM.AMOUNT */
        else if (propertyName.equalsIgnoreCase("COMMITMENT")) {

            /*
             * Check of whether latest date record is an authorized record or
             * not
             */
            contract.setContractId(arrangementId);
            try {
                aaPrdDesTermAmountRecord = new AaPrdDesTermAmountRecord(contract.getConditionForProperty(propertyName));
                try {
                    cmRecordStatus = aaPrdDesTermAmountRecord.getRecordStatus();
                } catch (Exception e) {
                }

                /* is a live record */
                if (cmRecordStatus.isEmpty()) {
                    try {
                        cmIdComp1 = aaPrdDesTermAmountRecord.getIdComp1().getValue();
                    } catch (Exception e) {
                    }
                    try {
                        cmIdComp2 = aaPrdDesTermAmountRecord.getIdComp2().getValue();
                    } catch (Exception e) {
                    }
                    try {
                        cmIdComp3 = aaPrdDesTermAmountRecord.getIdComp3().getValue();
                    } catch (Exception e) {
                    }
                    if ((!cmIdComp1.isEmpty()) && (!cmIdComp2.isEmpty()) && (!cmIdComp3.isEmpty())) {
                        processedId = cmIdComp1 + "-" + cmIdComp2 + "-" + cmIdComp3;
                    }
                }

                /* not a live record */
                else {
                    partialQueryString = arrangementId + "-" + propertyName.toUpperCase() + "...";
                    try {
                        aaArrTermAmountRecords = da.selectRecords("BNK", "AA.ARR.TERM.AMOUNT", "",
                                " WITH @ID LIKE " + partialQueryString + " AND CO.CODE EQ " + companyCode);

                        if (aaArrTermAmountRecords.size() > 0) {

                            /** order by descending order */
                            Collections.sort(aaArrTermAmountRecords, (a, b) -> {
                                String[] partsA = a.split("-");
                                String[] partsB = b.split("-");

                                String lastA = partsA[partsA.length - 1];
                                String lastB = partsB[partsB.length - 1];

                                String[] dateVerA = lastA.split("\\.");
                                String[] dateVerB = lastB.split("\\.");

                                String dateA = dateVerA[0];
                                String dateB = dateVerB[0];

                                int verA = dateVerA.length > 1 ? Integer.parseInt(dateVerA[1]) : 0;
                                int verB = dateVerB.length > 1 ? Integer.parseInt(dateVerB[1]) : 0;

                                int dateCompare = dateB.compareTo(dateA);
                                if (dateCompare != 0) {
                                    return dateCompare;
                                }
                                return Integer.compare(verB, verA);
                            });

                            /** retrieving latest date record id */
                            int count = 0;

                            for (String currId : aaArrTermAmountRecords) {
                                if (count > 0) {
                                    break;
                                } else {
                                    try {
                                        aaPrdDesTermAmountRecord = new AaPrdDesTermAmountRecord(
                                                da.getRecord("AA.ARR.TERM.AMOUNT", currId));
                                        try {
                                            String currRecordStatus = aaPrdDesTermAmountRecord.getRecordStatus();
                                            if (currRecordStatus.isEmpty()) {
                                                count++;
                                                processedId = currId;
                                            }
                                        } catch (Exception e) {
                                        }
                                    } catch (Exception e) {
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                    }
                }
            } catch (Exception e) {
            }
        }

        /** AA.ARR.INTREST */
        else if (listOfInterestProperties.contains(propertyName.toUpperCase())) {

            /*
             * Check of whether latest date record is an authorized record or
             * not
             */
            contract.setContractId(arrangementId);
            try {
                aaPrdDesInterestRecord = new AaPrdDesInterestRecord(contract.getConditionForProperty(propertyName));
                try {
                    cmRecordStatus = aaPrdDesInterestRecord.getRecordStatus();
                } catch (Exception e) {
                }

                /* is a live record */
                if (cmRecordStatus.isEmpty()) {
                    try {
                        cmIdComp1 = aaPrdDesInterestRecord.getIdComp1().getValue();
                    } catch (Exception e) {
                    }
                    try {
                        cmIdComp2 = aaPrdDesInterestRecord.getIdComp2().getValue();
                    } catch (Exception e) {
                    }
                    try {
                        cmIdComp3 = aaPrdDesInterestRecord.getIdComp3().getValue();
                    } catch (Exception e) {
                    }
                    if ((!cmIdComp1.isEmpty()) && (!cmIdComp2.isEmpty()) && (!cmIdComp3.isEmpty())) {
                        processedId = cmIdComp1 + "-" + cmIdComp2 + "-" + cmIdComp3;
                    }
                }

                /* not a live record */
                else {
                    partialQueryString = arrangementId + "-" + propertyName.toUpperCase() + "...";
                    try {
                        aaArrInterestRecords = da.selectRecords("BNK", "AA.ARR.INTEREST", "",
                                " WITH @ID LIKE " + partialQueryString + " AND CO.CODE EQ " + companyCode);

                        if (aaArrInterestRecords.size() > 0) {

                            /** order by descending order */
                            Collections.sort(aaArrInterestRecords, (a, b) -> {
                                String[] partsA = a.split("-");
                                String[] partsB = b.split("-");

                                String lastA = partsA[partsA.length - 1];
                                String lastB = partsB[partsB.length - 1];

                                String[] dateVerA = lastA.split("\\.");
                                String[] dateVerB = lastB.split("\\.");

                                String dateA = dateVerA[0];
                                String dateB = dateVerB[0];

                                int verA = dateVerA.length > 1 ? Integer.parseInt(dateVerA[1]) : 0;
                                int verB = dateVerB.length > 1 ? Integer.parseInt(dateVerB[1]) : 0;

                                int dateCompare = dateB.compareTo(dateA);
                                if (dateCompare != 0) {
                                    return dateCompare;
                                }
                                return Integer.compare(verB, verA);
                            });

                            /** retrieving latest date record id */
                            int count = 0;

                            for (String currId : aaArrInterestRecords) {
                                if (count > 0) {
                                    break;
                                } else {
                                    try {
                                        aaPrdDesInterestRecord = new AaPrdDesInterestRecord(
                                                da.getRecord("AA.ARR.INTEREST", currId));
                                        try {
                                            String currRecordStatus = aaPrdDesInterestRecord.getRecordStatus();
                                            if (currRecordStatus.isEmpty()) {
                                                count++;
                                                processedId = currId;
                                            }
                                        } catch (Exception e) {
                                        }
                                    } catch (Exception e) {
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                    }
                }
            } catch (Exception e) {
            }
        }

        /** AA.ARR.LIMIT */
        else if (propertyName.equalsIgnoreCase("LIMIT")) {

            /*
             * Check of whether latest date record is an authorized record or
             * not
             */
            contract.setContractId(arrangementId);
            try {
                aaPrdDesLimitRecord = new AaPrdDesLimitRecord(contract.getConditionForProperty(propertyName));
                try {
                    cmRecordStatus = aaPrdDesLimitRecord.getRecordStatus();
                } catch (Exception e) {
                }

                /* is a live record */
                if (cmRecordStatus.isEmpty()) {
                    try {
                        cmIdComp1 = aaPrdDesLimitRecord.getIdComp1().getValue();
                    } catch (Exception e) {
                    }
                    try {
                        cmIdComp2 = aaPrdDesLimitRecord.getIdComp2().getValue();
                    } catch (Exception e) {
                    }
                    try {
                        cmIdComp3 = aaPrdDesLimitRecord.getIdComp3().getValue();
                    } catch (Exception e) {
                    }
                    if ((!cmIdComp1.isEmpty()) && (!cmIdComp2.isEmpty()) && (!cmIdComp3.isEmpty())) {
                        processedId = cmIdComp1 + "-" + cmIdComp2 + "-" + cmIdComp3;
                    }
                }

                /* not a live record */
                else {
                    partialQueryString = arrangementId + "-" + propertyName.toUpperCase() + "...";
                    try {
                        aaArrLimitRecords = da.selectRecords("BNK", "AA.ARR.LIMIT", "",
                                " WITH @ID LIKE " + partialQueryString + " AND CO.CODE EQ " + companyCode);

                        if (aaArrLimitRecords.size() > 0) {

                            /** order by descending order */
                            Collections.sort(aaArrLimitRecords, (a, b) -> {
                                String[] partsA = a.split("-");
                                String[] partsB = b.split("-");

                                String lastA = partsA[partsA.length - 1];
                                String lastB = partsB[partsB.length - 1];

                                String[] dateVerA = lastA.split("\\.");
                                String[] dateVerB = lastB.split("\\.");

                                String dateA = dateVerA[0];
                                String dateB = dateVerB[0];

                                int verA = dateVerA.length > 1 ? Integer.parseInt(dateVerA[1]) : 0;
                                int verB = dateVerB.length > 1 ? Integer.parseInt(dateVerB[1]) : 0;

                                int dateCompare = dateB.compareTo(dateA);
                                if (dateCompare != 0) {
                                    return dateCompare;
                                }
                                return Integer.compare(verB, verA);
                            });

                            /** retrieving latest date record id */
                            int count = 0;

                            for (String currId : aaArrLimitRecords) {
                                if (count > 0) {
                                    break;
                                } else {
                                    try {
                                        aaPrdDesLimitRecord = new AaPrdDesLimitRecord(
                                                da.getRecord("AA.ARR.LIMIT", currId));
                                        try {
                                            String currRecordStatus = aaPrdDesLimitRecord.getRecordStatus();
                                            if (currRecordStatus.isEmpty()) {
                                                count++;
                                                processedId = currId;
                                            }
                                        } catch (Exception e) {
                                        }
                                    } catch (Exception e) {
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                    }
                }
            } catch (Exception e) {
            }
        }

        /** AA.ARR.CHANGE.PRODUCT */
        else if (propertyName.equalsIgnoreCase("RENEWAL")) {

            /*
             * Check of whether latest date record is an authorized record or
             * not
             */
            contract.setContractId(arrangementId);
            try {
                aaPrdDesChangeProductRecord = new AaPrdDesChangeProductRecord(
                        contract.getConditionForProperty(propertyName));
                try {
                    cmRecordStatus = aaPrdDesChangeProductRecord.getRecordStatus();
                } catch (Exception e) {
                }

                /* is a live record */
                if (cmRecordStatus.isEmpty()) {
                    try {
                        cmIdComp1 = aaPrdDesChangeProductRecord.getIdComp1().getValue();
                    } catch (Exception e) {
                    }
                    try {
                        cmIdComp2 = aaPrdDesChangeProductRecord.getIdComp2().getValue();
                    } catch (Exception e) {
                    }
                    try {
                        cmIdComp3 = aaPrdDesChangeProductRecord.getIdComp3().getValue();
                    } catch (Exception e) {
                    }
                    if ((!cmIdComp1.isEmpty()) && (!cmIdComp2.isEmpty()) && (!cmIdComp3.isEmpty())) {
                        processedId = cmIdComp1 + "-" + cmIdComp2 + "-" + cmIdComp3;
                    }
                }

                /* not a live record */
                else {
                    partialQueryString = arrangementId + "-" + propertyName.toUpperCase() + "...";
                    try {
                        aaArrChangeProductRecords = da.selectRecords("BNK", "AA.ARR.CHANGE.PRODUCT", "",
                                " WITH @ID LIKE " + partialQueryString + " AND CO.CODE EQ " + companyCode);

                        if (aaArrChangeProductRecords.size() > 0) {

                            /** order by descending order */
                            Collections.sort(aaArrChangeProductRecords, (a, b) -> {
                                String[] partsA = a.split("-");
                                String[] partsB = b.split("-");

                                String lastA = partsA[partsA.length - 1];
                                String lastB = partsB[partsB.length - 1];

                                String[] dateVerA = lastA.split("\\.");
                                String[] dateVerB = lastB.split("\\.");

                                String dateA = dateVerA[0];
                                String dateB = dateVerB[0];

                                int verA = dateVerA.length > 1 ? Integer.parseInt(dateVerA[1]) : 0;
                                int verB = dateVerB.length > 1 ? Integer.parseInt(dateVerB[1]) : 0;

                                int dateCompare = dateB.compareTo(dateA);
                                if (dateCompare != 0) {
                                    return dateCompare;
                                }
                                return Integer.compare(verB, verA);
                            });

                            /** retrieving latest date record id */
                            int count = 0;

                            for (String currId : aaArrChangeProductRecords) {
                                if (count > 0) {
                                    break;
                                } else {
                                    try {
                                        aaPrdDesChangeProductRecord = new AaPrdDesChangeProductRecord(
                                                da.getRecord("AA.ARR.CHANGE.PRODUCT", currId));
                                        try {
                                            String currRecordStatus = aaPrdDesChangeProductRecord.getRecordStatus();
                                            if (currRecordStatus.isEmpty()) {
                                                count++;
                                                processedId = currId;
                                            }
                                        } catch (Exception e) {
                                        }
                                    } catch (Exception e) {
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                    }
                }
            } catch (Exception e) {
            }
        }

        /** AA.ARR.PAYMENT.SCHEDULE */
        else if (propertyName.equalsIgnoreCase("SCHEDULE")) {

            /*
             * Check of whether latest date record is an authorized record or
             * not
             */
            contract.setContractId(arrangementId);
            try {
                aaPrdDesPaymentScheduleRecord = new AaPrdDesPaymentScheduleRecord(
                        contract.getConditionForProperty(propertyName));
                try {
                    cmRecordStatus = aaPrdDesPaymentScheduleRecord.getRecordStatus();
                } catch (Exception e) {
                }

                /* is a live record */
                if (cmRecordStatus.isEmpty()) {
                    try {
                        cmIdComp1 = aaPrdDesPaymentScheduleRecord.getIdComp1().getValue();
                    } catch (Exception e) {
                    }
                    try {
                        cmIdComp2 = aaPrdDesPaymentScheduleRecord.getIdComp2().getValue();
                    } catch (Exception e) {
                    }
                    try {
                        cmIdComp3 = aaPrdDesPaymentScheduleRecord.getIdComp3().getValue();
                    } catch (Exception e) {
                    }
                    if ((!cmIdComp1.isEmpty()) && (!cmIdComp2.isEmpty()) && (!cmIdComp3.isEmpty())) {
                        processedId = cmIdComp1 + "-" + cmIdComp2 + "-" + cmIdComp3;
                    }
                }

                /* not a live record */
                else {
                    partialQueryString = arrangementId + "-" + propertyName.toUpperCase() + "...";
                    try {
                        aaArrPaymentScheduleRecords = da.selectRecords("BNK", "AA.ARR.PAYMENT.SCHEDULE", "",
                                " WITH @ID LIKE " + partialQueryString + " AND CO.CODE EQ " + companyCode);

                        if (aaArrPaymentScheduleRecords.size() > 0) {

                            /** order by descending order */
                            Collections.sort(aaArrPaymentScheduleRecords, (a, b) -> {
                                String[] partsA = a.split("-");
                                String[] partsB = b.split("-");

                                String lastA = partsA[partsA.length - 1];
                                String lastB = partsB[partsB.length - 1];

                                String[] dateVerA = lastA.split("\\.");
                                String[] dateVerB = lastB.split("\\.");

                                String dateA = dateVerA[0];
                                String dateB = dateVerB[0];

                                int verA = dateVerA.length > 1 ? Integer.parseInt(dateVerA[1]) : 0;
                                int verB = dateVerB.length > 1 ? Integer.parseInt(dateVerB[1]) : 0;

                                int dateCompare = dateB.compareTo(dateA);
                                if (dateCompare != 0) {
                                    return dateCompare;
                                }
                                return Integer.compare(verB, verA);
                            });

                            /** retrieving latest date record id */
                            int count = 0;

                            for (String currId : aaArrPaymentScheduleRecords) {
                                if (count > 0) {
                                    break;
                                } else {
                                    try {
                                        aaPrdDesPaymentScheduleRecord = new AaPrdDesPaymentScheduleRecord(
                                                da.getRecord("AA.ARR.PAYMENT.SCHEDULE", currId));
                                        try {
                                            String currRecordStatus = aaPrdDesPaymentScheduleRecord.getRecordStatus();
                                            if (currRecordStatus.isEmpty()) {
                                                count++;
                                                processedId = currId;
                                            }
                                        } catch (Exception e) {
                                        }
                                    } catch (Exception e) {
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                    }
                }
            } catch (Exception e) {
            }
        }

        /** AA.ARR.SETTLEMENT */
        else if (propertyName.equalsIgnoreCase("SETTLEMENT")) {
            /*
             * Check of whether latest date record is an authorized record or
             * not
             */
            contract.setContractId(arrangementId);
            try {
                aaPrdDesSettlementRecord = new AaPrdDesSettlementRecord(contract.getConditionForProperty(propertyName));
                try {
                    cmRecordStatus = aaPrdDesSettlementRecord.getRecordStatus();
                } catch (Exception e) {
                }
                /* is a live record */
                if (cmRecordStatus.isEmpty()) {
                    try {
                        cmIdComp1 = aaPrdDesSettlementRecord.getIdComp1().getValue();
                    } catch (Exception e) {
                    }
                    try {
                        cmIdComp2 = aaPrdDesSettlementRecord.getIdComp2().getValue();
                    } catch (Exception e) {
                    }
                    try {
                        cmIdComp3 = aaPrdDesSettlementRecord.getIdComp3().getValue();
                    } catch (Exception e) {
                    }
                    if ((!cmIdComp1.isEmpty()) && (!cmIdComp2.isEmpty()) && (!cmIdComp3.isEmpty())) {
                        processedId = cmIdComp1 + "-" + cmIdComp2 + "-" + cmIdComp3;
                    }
                }
                /* not a live record */
                else {
                    partialQueryString = arrangementId + "-" + propertyName.toUpperCase() + "...";
                    try {
                        aaArrSettlementRecords = da.selectRecords("BNK", "AA.ARR.SETTLEMENT", "",
                                " WITH @ID LIKE " + partialQueryString + " AND CO.CODE EQ " + companyCode);
                        if (aaArrSettlementRecords.size() > 0) {
                            /** order by descending order */
                            Collections.sort(aaArrSettlementRecords, (a, b) -> {
                                String[] partsA = a.split("-");
                                String[] partsB = b.split("-");

                                String lastA = partsA[partsA.length - 1];
                                String lastB = partsB[partsB.length - 1];

                                String[] dateVerA = lastA.split("\\.");
                                String[] dateVerB = lastB.split("\\.");

                                String dateA = dateVerA[0];
                                String dateB = dateVerB[0];

                                int verA = dateVerA.length > 1 ? Integer.parseInt(dateVerA[1]) : 0;
                                int verB = dateVerB.length > 1 ? Integer.parseInt(dateVerB[1]) : 0;

                                int dateCompare = dateB.compareTo(dateA);
                                if (dateCompare != 0) {
                                    return dateCompare;
                                }
                                return Integer.compare(verB, verA);
                            });
                            /** retrieving latest date record id */
                            int count = 0;
                            for (String currId : aaArrSettlementRecords) {
                                if (count > 0) {
                                    break;
                                } else {
                                    try {
                                        aaPrdDesSettlementRecord = new AaPrdDesSettlementRecord(
                                                da.getRecord("AA.ARR.SETTLEMENT", currId));
                                        try {
                                            String currRecordStatus = aaPrdDesSettlementRecord.getRecordStatus();
                                            if (currRecordStatus.isEmpty()) {
                                                count++;
                                                processedId = currId;
                                            }
                                        } catch (Exception e) {
                                        }
                                    } catch (Exception e) {
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                    }
                }
            } catch (Exception e) {
            }
        }

        /** end of clauses */
        return processedId;
    }

}