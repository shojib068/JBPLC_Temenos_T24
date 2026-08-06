package com.temenos.t24;

import com.temenos.t24.api.complex.eb.enquiryhook.EnquiryContext;
import com.temenos.t24.api.complex.eb.enquiryhook.FilterCriteria;
import com.temenos.t24.api.hook.system.Enquiry;
import com.temenos.t24.api.records.aaarrangement.AaArrangementRecord;
import com.temenos.t24.api.records.aaproduct.AaProductRecord;
import com.temenos.t24.api.records.account.AccountRecord;
import com.temenos.t24.api.records.aaaccountdetails.AaAccountDetailsRecord;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.system.Session;
import java.util.ArrayList;
import java.util.List;

/*
 * 
 */

public class NoFileCusProDetails extends Enquiry {
    public List<String> setIds(List<FilterCriteria> filterCriteria, EnquiryContext enquiryContext) {
        
        
        DataAccess da = new DataAccess(this);
        Session ss = new Session(this);
        List<String> arrList = new ArrayList<>();
        List<String> retList = new ArrayList<>();
        
        
        // Variables
        String arrId = "";
        String coCode = ss.getCompanyId();
        String productLine = "DEPOSITS";
        String arrStatus = "CURRENT";
        String currency = "";
        String account = "";
        String product = "";
        String description = "";
        String startDate = "";
        String maturityDate = "";
        String linkApplId = "";
        String output = "";
        String name = "";

        AaArrangementRecord arrRec = null;
        AccountRecord acc = null;
        AaProductRecord prodRec = null;
        AaAccountDetailsRecord accDetRec = null;

//        // Selection Criteria
//        for (FilterCriteria filcrit : filterCriteria) {
//            String selectionFieldName = filcrit.getFieldname();
//            switch (selectionFieldName) {
//                case "CO.CODE":
//                    coCode = ss.getCompanyId();
//                    break;
//                case "PRODUCT.LINE":
//                    productLine = "DEPOSITS";
//                    break;
//                case "ARR.STATUS":
//                    arrStatus = "CURRENT";
//                    break;
//                default:
//                    break;
//            }
//        }

        // Build Query
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
                }catch(Exception e){
                    
                }
                    //get Account.
                    
                    try{
                        account = arrRec.getLinkedAppl(0).getLinkedApplId().getValue();
                    }catch(Exception e){
                        throw e;
                    }

                    // Get Currency
                    
                    
                    try {
                        currency = arrRec.getCurrency().getValue();
                    } catch (Exception e) {
                        currency = "";
                    }

                    // Get Product
                    try {
                        product = arrRec.getProduct(0).getProduct().getValue();
                    } catch (Exception e) {
                        product = "";
                    }

                    // Get Linked Application ID
                    try {
                        linkApplId = arrRec.getLinkedAppl(0).getLinkedApplId().getValue();
                    } catch (Exception e) {
                        linkApplId = "";
                    }

                    // Get Account (Customer Mnemonic)
                    if (!linkApplId.isEmpty()) {
                        try {
                            acc = new AccountRecord(da.getRecord("ACCOUNT", linkApplId));
                            name = acc.getShortTitle(0).getValue();
                        } catch (Exception e) {
                           name = "";
                        }
                    }

                    // Get Product Description
                    if (!product.isEmpty()) {
                        try {
                            prodRec = new AaProductRecord(da.getRecord("AA.PRODUCT", product));
                            description = prodRec.getDescription(0).getValue();
                        } catch (Exception e) {
                            description = "";
                        }
                    }

                    // Get Start & Maturity Dates
                    try {
                        accDetRec = new AaAccountDetailsRecord(da.getRecord("AA.ACCOUNT.DETAILS", arrId));
                    } catch (Exception e) {}
                    try{
                        startDate = accDetRec.getStartDate().getValue();
                    }catch(Exception e){}
                    
                    try{
                        maturityDate = accDetRec.getMaturityDate().getValue();
                    } catch (Exception e) {
                    }
                    
                    output = account + "*" + name + "*" + currency  + "*" + product + "*" + description + "*" + startDate + "*" + maturityDate;
                    retList.add(output);

                    account = "";
                    name = "";
                    currency = "";
            }
        }

        return retList;
    }
}
