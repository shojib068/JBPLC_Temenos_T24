package com.temenos.t24;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import com.temenos.api.TStructure;
import com.temenos.t24.api.complex.eb.servicehook.ServiceData;
import com.temenos.t24.api.complex.eb.servicehook.SynchronousTransactionData;
import com.temenos.t24.api.complex.eb.servicehook.TransactionControl;
import com.temenos.t24.api.hook.system.ServiceLifecycle;
import com.temenos.t24.api.records.aajbllockeraccess.AaJblLockerAccessRecord;
import com.temenos.t24.api.records.aajbllockeracct.AaJblLockerAcctRecord;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.system.Session;

/**
 * EB.API: LOCKER.UNCLAIMED, LOCKER.UNCLAIMED.SELECT
 * PGM.FILE: LOCKER.UNCLAIMED
 * BATCH: BNK/LOCKER.UNCLAIMED
 * TS.SERVICE: BNK/LOCKER.UNCLAIMED
* Business Logic:
 * =====================================================
 * CASE: Identify and Mark Unclaimed Locker Accounts
 * =====================================================
 *
 * - This service identifies locker accounts that are
 *   eligible to be marked as "Unclaimed".
 *
 * - Eligibility rule:
 *     → No activity for 10 years from last access date
 *
 * =====================================================
 * Step 1: Fetch Live Locker Accounts
 * =====================================================
 *
 * - Retrieve records from:
 *     → AA.JBL.LOCKER.ACCT (LIVE)
 *
 * - Filter by current company code
 *
 * =====================================================
 * Step 2: Exclude Pending (UNAUTH) Records
 * =====================================================
 *
 * - Retrieve records from:
 *     → AA.JBL.LOCKER.ACCT$NAU
 *
 * - Remove UNAUTH records from LIVE list
 *
 * =====================================================
 * Step 3: Fetch Last Access Information
 * =====================================================
 *
 * - For each remaining account:
 *     → Fetch record from AA.JBL.LOCKER.ACCESS
 *     → Extract LAST ACCESS DATE
 *
 * - If no access date exists:
 *     → Skip record
 *
 * =====================================================
 * Step 4: Apply 10-Year Inactivity Rule
 * =====================================================
 *
 * - Convert last access date using format:
 *     → yyyyMMdd (T24 standard format)
 *
 * - Check condition:
 *
 *     lastAccessDate + 10 years <= today
 *
 * - If condition satisfied:
 *     → Add account to FINAL list (unclaimed list)
 *
 * =====================================================
 * Step 5: Return Eligible IDs
 * =====================================================
 *
 * - Return list of locker accounts marked as:
 *     → Unclaimed candidates
 *
 * =====================================================
 * CASE: Update Locker Account Status
 * =====================================================
 *
 * - This step updates selected accounts to "Unclaimed"
 *
 * =====================================================
 * Step 6: Fetch Locker Account Record
 * =====================================================
 *
 * - Retrieve record from:
 *     → AA.JBL.LOCKER.ACCT
 *
 * =====================================================
 * Step 7: Update Status
 * =====================================================
 *
 * - Set:
 *     → STATUS = "Unclaimed"
 *
 * =====================================================
 * Step 8: Prepare OFS Transaction
 * =====================================================
 *
 * - Create Synchronous Transaction:
 *     → Version: AA.JBL.LOCKER.ACCT,OFS
 *     → Function: INPUT
 *     → Source: BULK.OFS
 *     → User: INPUTT
 *     → Company: Current CoCode
 *     → Transaction ID: Locker Account ID
 *
 * =====================================================
 * Step 9: Apply Update
 * =====================================================
 *
 * - Add:
 *     → Transaction data
 *     → Updated record structure
 *
 * =====================================================
 * NOTE
 * =====================================================
 *
 * - Exceptions are silently handled in current implementation
 * - Date comparison is strictly based on:
 *     → yyyyMMdd format
 * - This routine is designed for bulk processing
 * 
 * @author kawsar
 *
 */
public class GbSAaJblLockerUnclaimed extends ServiceLifecycle{
    private static final DateTimeFormatter T24_DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMdd");
    @Override
    public List<String> getIds(ServiceData serviceData, List<String> controlList) {
        List<String> recIds = new ArrayList<>();
        List<String> nauIds = new ArrayList<>();
        List<String> finalIds = new ArrayList<>();
        
        DataAccess da = new DataAccess(this);
        Session ss = new Session(this);
        LocalDate today = LocalDate.now();
        
        String coCode = ss.getCompanyId();
        String selectStmt = " WITH CO.CODE EQ "+coCode;
        
        try{
            recIds = da.selectRecords("BNK", "AA.JBL.LOCKER.ACCT", "", selectStmt); 
        }catch(Exception e){}
        
        try{
            nauIds = da.selectRecords("BNK", "AA.JBL.LOCKER.ACCT$NAU", "", selectStmt); 
        }catch(Exception e){}
        
        if(recIds != null && nauIds != null){
            recIds.removeAll(nauIds);
        }
        
//        filter 10years old unclaimed account from last transaction date
        if( recIds!= null){
            for( String id : recIds ){ // 01.01
                List<String> locAccessRecId = new ArrayList<>();
                LocalDate lastAccessDate = null;
                String lastAccessDateStr = "";
                String stmt = " WITH LOCKER.ID EQ "+id;
                try{
                    locAccessRecId = da.selectRecords("BNK", "AA.JBL.LOCKER.ACCESS", "", stmt);
                }catch(Exception e){}
                if(!locAccessRecId.isEmpty() && locAccessRecId!= null){
                    for(String ids : locAccessRecId){
                        AaJblLockerAccessRecord locAccessRec = null;
                        try{
                            locAccessRec = new AaJblLockerAccessRecord(da.getRecord("AA.JBL.LOCKER.ACCESS", ids));
                        }catch(Exception e){}
                        if(locAccessRec!=null){
                            try{
                                lastAccessDateStr = locAccessRec.getDate().getValue();
                            }catch(Exception e){}
                            
                        }
                        if(lastAccessDateStr == null || lastAccessDateStr.isEmpty())
                            continue;
                        if(lastAccessDateStr!=null && !lastAccessDateStr.isEmpty()){
                            lastAccessDate = LocalDate.parse(lastAccessDateStr,T24_DATE_FORMAT);
                        } 
                        if(!lastAccessDate.plusYears(10).isAfter(today))
                            finalIds.add(id);
                    }
                }                              
            }
        }
        return finalIds ;
    }

    @Override
    public void updateRecord(String id, ServiceData serviceData, String controlItem,
            TransactionControl transactionControl, List<SynchronousTransactionData> transactionData,
            List<TStructure> records) {
        
        DataAccess da = new DataAccess(this);
        AaJblLockerAcctRecord lockerAccRec = null;
        SynchronousTransactionData txn = null;
        try{
            lockerAccRec = new AaJblLockerAcctRecord(da.getRecord("AA.JBL.LOCKER.ACCT", id));
            lockerAccRec.setStatus("Unclaimed");       
        }catch(Exception e){}
        
//        ofs
        
        try{
            txn = new SynchronousTransactionData();
            
            
            txn.setFunction("INPUT");            
            txn.setUserName("INPUTT");
            txn.setTransactionId(id);
            txn.setSourceId("LOCKER.OFS");
            txn.setVersionId("AA.JBL.LOCKER.ACCT,OFS");
            
            transactionData.add(txn);
            records.add(lockerAccRec.toStructure());
        }catch(Exception e){}  
    }
}
