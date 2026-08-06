package com.temenos.t24;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import com.ibm.icu.text.SimpleDateFormat;
import com.temenos.t24.api.complex.eb.enquiryhook.EnquiryContext;
import com.temenos.t24.api.complex.eb.enquiryhook.FilterCriteria;
import com.temenos.t24.api.hook.system.Enquiry;
import com.temenos.t24.api.records.company.CompanyRecord;
import com.temenos.t24.api.records.ebjblinstrumentsinfo.EbJblInstrumentsInfoRecord;
import com.temenos.t24.api.records.fundstransfer.FundsTransferRecord;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.system.Session;

/**
 * TODO: Document me!
 *
 * @author kawsar
 *
 */
public class RpJblTTPaymentRegister extends Enquiry {

    @Override
    public List<String> setIds(List<FilterCriteria> filterCriteria, EnquiryContext enquiryContext) {
        // TODO Auto-generated method stub
        String debitValueDate = "";
        String dVt = "";
        String fieldOperand = "";
        String inputDate = "";
        String fromDate = "";
        String toDate = "";

        Session ss = new Session(this);
        String coCode = ss.getCompanyId();
        List<String> ebJblInstrInfoRecs = new ArrayList<>();
        FundsTransferRecord ftRec = null;
        EbJblInstrumentsInfoRecord instrfoRec = null;
        CompanyRecord companyRec = null;

        DataAccess da = new DataAccess(this);
        String ret = "";
        String lmmLocRet = "";
        List<String> finalRet = new ArrayList<>();
        boolean trueFalseinpDt = false;
        boolean trueFalsefromDate = false;
        boolean trueFalsetoDate = false;

        /* to get input selection field value */
        for (FilterCriteria filcrit : filterCriteria) {
            String selectionFieldName = filcrit.getFieldname();
            switch (selectionFieldName) {
            case "PAYMENT.DATE":
                fieldOperand = filcrit.getOperand();
                debitValueDate = filcrit.getValue().toString();
                break;
            default:
            }
        }

        /* to check selection input date is valid or not */
        if (!debitValueDate.isEmpty()) {
            String[] dates = debitValueDate.split(" ");
            int count = dates.length;
            switch (count) {
            case 1:
                inputDate = dates[0];
                trueFalseinpDt = isValidYYYYMMDD(inputDate);
                break;
            case 2:
                fromDate = dates[0];
                toDate = dates[1];
                trueFalsefromDate = isValidYYYYMMDD(fromDate);
                trueFalsetoDate = isValidYYYYMMDD(toDate);
                break;
            }
        }

        /* check enquiry selection date is valid or not. return otherwise */
        if ((!debitValueDate.isEmpty() && !inputDate.isEmpty() && trueFalseinpDt == false)
                || (!debitValueDate.isEmpty() && !fromDate.isEmpty() && !toDate.isEmpty()
                        && (trueFalsefromDate == false || trueFalsetoDate == false))) {
            return finalRet;
        } else {
            try {
                ebJblInstrInfoRecs = da.selectRecords("", "EB.JBL.INSTRUMENTS.INFO", "", " WITH PAYEE.BRANCH EQ "
                        + coCode + " AND INSTRUMENT.TYPE EQ 'TT' AND STATUS EQ 'PAID' AND @ID LIKE 'FT...'");
            } catch (Exception e) {
                e.getMessage();
            }
            if (!ebJblInstrInfoRecs.isEmpty()) {
                for (String ftId : ebJblInstrInfoRecs) {
                    try {
                        ftRec = new FundsTransferRecord(da.getRecord("FUNDS.TRANSFER", ftId));
                    } catch (Exception e) {
                        ftRec = new FundsTransferRecord(da.getHistoryRecord("FUNDS.TRANSFER", ftId));
                    }

                    if (ftRec != null && !ftRec.toString().isEmpty()) {
                        try {
                            instrfoRec = new EbJblInstrumentsInfoRecord(da.getRecord("EB.JBL.INSTRUMENTS.INFO", ftId));
                            companyRec = new CompanyRecord(da.getRecord("COMPANY", instrfoRec.getPayeeBranch().getValue()));
                            dVt = ftRec.getDebitValueDate().getValue();

                        } catch (Exception e) {
                            e.getMessage();
                        }

                        switch (Integer.parseInt(fieldOperand)) {
                        case 1:
                            if ((!dVt.isEmpty() || dVt != null)) {
                                if (trueFalseinpDt == true && dVt.equals(inputDate)) {
                                    ret = getFtInstrumentInfo(ftRec, instrfoRec, da, companyRec, lmmLocRet);
                                    finalRet.add(ret);
                                }
                            }
                            break;

                        case 2:
                            if ((!dVt.isEmpty() || dVt != null)) {
                                if (trueFalsefromDate == true && trueFalsetoDate == true
                                        && Integer.parseInt(dVt) >= Integer.parseInt(fromDate)
                                        && Integer.parseInt(dVt) <= Integer.parseInt(toDate)) {
                                    ret = getFtInstrumentInfo(ftRec, instrfoRec, da, companyRec, lmmLocRet);
                                    finalRet.add(ret);
                                }
                            }
                            break;

                        default:
                        }
                    }
                }
            }
        }

        return finalRet;
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

    public static String getFtInstrumentInfo(FundsTransferRecord ftRec, EbJblInstrumentsInfoRecord instrfoRec,
            DataAccess da, CompanyRecord companyRec, String lmmLocRet) {
        String lmPaymentDate = "";
        String lmDVt = "";
        String lmIssueDate = "";
        String lmTTScrollNo = "";
        String lmBenificiaryAccNo = "";
        String lmInputter = "";
        String lmAuthorizer = "";
        String lmIssuingBranchCode = "";
        String lmAmtOfTT = "";
        String lmIssuingBranchName = "";

        try {
            lmPaymentDate = ftRec.getLocalRefField("LT.TXN.DATE").getValue();
            lmDVt = ftRec.getDebitValueDate().getValue();
            lmIssueDate = ftRec.getLocalRefField("LT.TXN.DATE").getValue();
            lmTTScrollNo = ftRec.getLocalRefField("LT.SCROLL").getValue();
            lmBenificiaryAccNo = ftRec.getLocalRefField("LT.ADV.REF.NO").getValue();
            lmInputter = ftRec.getInputter(0);
            lmAuthorizer = ftRec.getAuthoriser();
            lmIssuingBranchCode = instrfoRec.getIssuedBranch().getValue();
            lmAmtOfTT = instrfoRec.getAmount().getValue();
            lmIssuingBranchName = companyRec.getCompanyName().get(0).getValue();
        } catch (Exception e) {
            e.getMessage();
        }

        lmmLocRet = lmPaymentDate + "*" + lmDVt + "*" + lmIssueDate + "*" + lmTTScrollNo + "*" + lmIssuingBranchCode
                + "*" + lmIssuingBranchName + "*" + lmBenificiaryAccNo + "*" + lmAmtOfTT + "*" + lmInputter + "*"
                + lmAuthorizer;
        return lmmLocRet;
    }
}




