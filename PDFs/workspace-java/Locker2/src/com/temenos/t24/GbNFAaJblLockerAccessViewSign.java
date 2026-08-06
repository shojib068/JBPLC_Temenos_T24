package com.temenos.t24;

import java.util.ArrayList;
import java.util.List;

import com.temenos.t24.api.complex.eb.enquiryhook.EnquiryContext;
import com.temenos.t24.api.complex.eb.enquiryhook.FilterCriteria;
import com.temenos.t24.api.hook.system.Enquiry;
import com.temenos.t24.api.records.aajbllockeracct.AaJblLockerAcctRecord;
import com.temenos.t24.api.records.aajbllockeracct.MandateCustomerIdClass;
import com.temenos.t24.api.records.aajbllockeracct.NomineeTypeClass;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.system.Session;

/**
 * SS: NOFILE.VIEW.SIGN
 * EB.API: GbNFAaJblLockerAccessViewSign
 * Enquiry: JBL.ENQ.LOCKER.VIEW.SIGN
* Business Logic:
 *
 * This is a No-File Enquiry that fetches signature image records
 * from IM.DOCUMENT.IMAGE based on input filter criteria.
 *
 * The hook reads IMAGE.REFERENCE from the enquiry input,
 * extracts the base reference (before "."), and retrieves
 * matching image records.
 *
 * It filters records based on:
 * - CO.CODE = Current company
 * - IMAGE.REFERENCE = Input reference
 * - IMAGE.TYPE = SIGNATURES
 *
 * Output:
 * - Returns list of matching IM.DOCUMENT.IMAGE record IDs
 *
 * Notes:
 * - If IMAGE.REFERENCE is not provided, no records are returned
 * - Only authorised records are fetched (default T24 behaviour)
 *
 */

public class GbNFAaJblLockerAccessViewSign extends Enquiry {

    @Override
    public List<String> setIds(List<FilterCriteria> filterCriteria, EnquiryContext enquiryContext) {

        List<String> returnList = new ArrayList<>();
        List<String> fetchIds = new ArrayList<>();
        List<String> mandateList = new ArrayList<>();
        List<String> nomineeList = new ArrayList<>();
        List<MandateCustomerIdClass> mandateCustIdList = null;
        List<NomineeTypeClass> nomineeTypeList = null;

        AaJblLockerAcctRecord locAcctRec = null;
        DataAccess da = new DataAccess(this);
        Session ss = new Session(this);

        String imgRef = "";
        String lockerAcctNo = "";
        String coCode = ss.getCompanyId();

        try {
            if (filterCriteria != null) {
                for(FilterCriteria fc : filterCriteria){ 
                    String selectFieldName = fc.getFieldname(); 
                    switch(selectFieldName){ 
                        case "IMAGE.REFERENCE": 
                            lockerAcctNo = fc.getValue();
                            imgRef = lockerAcctNo.split("\\.")[0]; 
                            break; 
                        default: 
                             } 
                    }
            }
            if (imgRef.isEmpty()) {
                return returnList;
            }
//            fetch image 
            String selectStmt = " WITH CO.CODE EQ " + coCode +
                                " AND IMAGE.REFERENCE EQ " + imgRef +
                                " AND IMAGE.TYPE EQ SIGNATURES";
            try{
                fetchIds = da.selectRecords("", "IM.DOCUMENT.IMAGE", "", selectStmt);
            }catch(Exception e){}
            

            if (fetchIds != null) {
                returnList.addAll(fetchIds);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return returnList;
    }
}