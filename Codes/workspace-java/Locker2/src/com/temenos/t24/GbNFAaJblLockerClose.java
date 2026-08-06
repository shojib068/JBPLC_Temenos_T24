package com.temenos.t24;

import java.util.ArrayList;
import java.util.List;

import com.temenos.t24.api.complex.eb.enquiryhook.EnquiryContext;
import com.temenos.t24.api.complex.eb.enquiryhook.FilterCriteria;
import com.temenos.t24.api.hook.system.Enquiry;
import com.temenos.t24.api.records.aajbllockeracct.AaJblLockerAcctRecord;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.system.Session;

/**
 * SS: NOFILE.LOCKER.CLOSE
 * EB.API: GbNFAaJblLockerClose
 * Enquiry: JBL.ENQ.LOCKER.ACCT.TO.CLOSE
 * Business Logic:
 * 
 * This is a No-File Enquiry that fetches Locker Account details
 * based on input filters and returns formatted output strings.
 * 
 * It excludes:
 * - Unauthorised records (NAU)
 * Output Format:
 * LOCKER.ID * ACCOUNT.NO * CUSTOMER * STATUS
 * 
 */
public class GbNFAaJblLockerClose extends Enquiry {

    @Override
    public List<String> setIds(List<FilterCriteria> filterCriteria, EnquiryContext enquiryContext) {

        List<String> returnList = new ArrayList<>();
        List<String> fetchedIds = new ArrayList<>();

        DataAccess da = new DataAccess(this);
        Session session = new Session(this);
        String coCode = session.getCompanyId();
        String selectionCritValue = "";
        
        for (FilterCriteria fc : filterCriteria){
            String selectionFieldName = fc.getFieldname();
            
            switch(selectionFieldName){
            
            case "ACCT.NO":
                selectionCritValue = fc.getValue();
                break;
                
            default:
            }
        }
        

        String selectStmt = " WITH CO.CODE EQ "+coCode+" AND STATUS NE Closed";
        
        if(selectionCritValue.contains(".")){
            selectStmt+= " AND @ID EQ "+selectionCritValue;
            
        }else{
            selectStmt+= " AND ACCT.NO EQ "+selectionCritValue;
        }

        try{
            fetchedIds = da.selectRecords("BNK", "AA.JBL.LOCKER.ACCT", "", selectStmt) ;
        }catch( Exception e){ }
        
        for ( String id : fetchedIds){
            
            try {
                da.getRecord("AA.JBL.LOCKER.ACCT$NAU", id);
                continue;
            } catch (Exception e) {
                // not in NAU
            }
            AaJblLockerAcctRecord lockerAcctRec = new AaJblLockerAcctRecord(
                    da.getRecord("AA.JBL.LOCKER.ACCT", id));
            String outLockerId = id;
            String outAcctNo = lockerAcctRec.getAcctNo().getValue();
            String outCustomer = lockerAcctRec.getCustomer().getValue();
            String outStatus = lockerAcctRec.getStatus().getValue();
            String result = outLockerId+"*"+outAcctNo+"*"+outCustomer+"*"+outStatus;
            
            returnList.add(result);

        }
        return returnList;
    }
}