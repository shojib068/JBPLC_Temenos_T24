package com.temenos.t24;

import java.util.ArrayList;
import java.util.List;

import com.temenos.t24.api.complex.eb.enquiryhook.EnquiryContext;
import com.temenos.t24.api.complex.eb.enquiryhook.FilterCriteria;
import com.temenos.t24.api.hook.system.Enquiry;
import com.temenos.t24.api.records.aajbllockeracct.AaJblLockerAcctRecord;
import com.temenos.t24.api.records.customer.CustomerRecord;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.system.Session;

/**
 * TODO: Document me!
 *
 * @author kawsar
 *
 */
public class GbRptLocStaffList extends Enquiry{

    @Override
    public List<String> setIds(List<FilterCriteria> filterCriteria, EnquiryContext enquiryContext) {
        List<String> returnList = new ArrayList<>();
        List<String> fetchedIds = new ArrayList<>();
        
        DataAccess da = new DataAccess(this);
        Session ss = new Session(this);
        
        String coCode = ss.getCompanyId();
        
        String selectStmt = " WITH CO.CODE EQ "+coCode;
        fetchedIds = da.selectRecords("BNK", "AA.JBL.LOCKER.ACCT", "", selectStmt);
        
        for ( String ids : fetchedIds){
            AaJblLockerAcctRecord locAcctRec = null;
            String custId = "";
            try{
                locAcctRec = new AaJblLockerAcctRecord(da.getRecord("AA.JBL.LOCKER.ACCT", ids));
            }catch(Exception e){}
            if(locAcctRec != null){
                try{
                    custId = locAcctRec.getCustomer().getValue();
                }catch(Exception e){}
            }
            CustomerRecord cusRec = null;
            String cusStatus = ""; 
            if(!custId.isEmpty()){
                try{
                    cusRec = new CustomerRecord(da.getRecord("CUSTOMER", custId));
                }catch(Exception e){}
            }
            if(cusRec != null){
                try{
                    cusStatus = cusRec.getCustomerStatus().getValue();
                }catch(Exception e){}
            }
            if("7".equals(cusStatus)){
                returnList.add(ids); 
            }
        }
        return returnList;
    }
}
