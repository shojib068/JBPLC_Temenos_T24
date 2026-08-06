package com.temenos.t24;

import com.temenos.t24.api.complex.eb.enquiryhook.EnquiryContext;
import com.temenos.t24.api.complex.eb.enquiryhook.FilterCriteria;
import com.temenos.t24.api.hook.system.Enquiry;
import com.temenos.t24.api.records.aaarrangement.AaArrangementRecord;
import com.temenos.t24.api.records.aaproduct.AaProductRecord;
import com.temenos.t24.api.records.customer.CustomerRecord;
import com.temenos.t24.api.records.aaaccountdetails.AaAccountDetailsRecord;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.system.Session;
import java.util.ArrayList;
import java.util.List;

public class NoFileCusPro extends Enquiry {
    public List<String> setIds(List<FilterCriteria> filterCriteria, EnquiryContext enquiryContext) {
        String arrId = "";
        String coCode = "";
        String productLine = "";
        String arrStatus = "";
        String currency = "";
        String account = "";
        String product = "";
        String description = "";
        String startDate = "";
        String maturityDate = "";
        String linkApplId = "";

        AaArrangementRecord arrRec = null;
        CustomerRecord cusRec = null;
        AaProductRecord prodRec = null;
        AaAccountDetailsRecord accDetRec = null;

        DataAccess da = new DataAccess(this);
        Session ss = new Session(this);
        List<String> arrList = new ArrayList<>();
        List<String> retList = new ArrayList<>();

        // Selection Criteria ---
        for (FilterCriteria filcrit : filterCriteria) {
            String selectionFieldName = filcrit.getFieldname();
            switch (selectionFieldName) {
                case "CO.CODE":
                    coCode = ss.getCompanyId();
                    break;
                case "PRODUCT.LINE":
                    productLine = filcrit.getValue().toString();
                    break;
                case "ARR.STATUS":
                    arrStatus = filcrit.getValue().toString();
                    break;
                default:
                    break;
            }
        }

        // --- Build Query ---
        String query = "WITH CO.CODE EQ " + coCode + " AND PRODUCT.LINE EQ " + productLine + " AND ARR.STATUS EQ " + arrStatus;

        try {
            arrList = da.selectRecords("", "AA.ARRANGEMENT", "", query);
        } catch (Exception e) {
            e.getMessage();
        }

        if (!arrList.isEmpty()) {
            for (String arr : arrList) {
                try {
                    arrRec = new AaArrangementRecord(da.getRecord("AA.ARRANGEMENT", arr));
                    arrId = arr;

                    try {
                        currency = arrRec.getCurrency().getValue();
                    } catch (Exception e) {
                        currency = "";
                    }

                    try {
                        product = arrRec.getProduct(0).getProduct().getValue();
                    } catch (Exception e) {
                        product = "";
                    }

                    try {
                        linkApplId = arrRec.getLinkedAppl(0).getLinkedApplId().getValue();
                    } catch (Exception e) {
                        linkApplId = "";
                    }

                    if (!linkApplId.isEmpty()) {
                        try {
                            cusRec = new CustomerRecord(da.getRecord("CUSTOMER", linkApplId));
                            account = cusRec.getMnemonic().getValue();
                        } catch (Exception e) {
                            account = "";
                        }
                    }

                    if (!product.isEmpty()) {
                        try {
                            prodRec = new AaProductRecord(da.getRecord("AA.PRODUCT", product));
                            description = prodRec.getDescription(0).getValue();
                        } catch (Exception e) {
                            description = "";
                        }
                    }

                    try {
                        accDetRec = new AaAccountDetailsRecord(da.getRecord("AA.ACCOUNT.DETAILS", arrId));
                        startDate = accDetRec.getStartDate().getValue();
                        maturityDate = accDetRec.getMaturityDate().getValue();
                    } catch (Exception e) {
                        startDate = "";
                        maturityDate = "";
                    }

                } catch (Exception e) {
                    e.getMessage();
                }
            }
        }

        String output = arrId + "*" + currency + "*" + account + "*" + product + "*" + description + "*" + startDate + "*" + maturityDate;
        retList.add(output);
        return retList;
    }
}
