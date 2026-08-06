package com.temenos.t24;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.temenos.t24.api.complex.eb.enquiryhook.EnquiryContext;
import com.temenos.t24.api.complex.eb.enquiryhook.FilterCriteria;
import com.temenos.t24.api.hook.system.Enquiry;
import com.temenos.t24.api.records.aajbllockeracct.AaJblLockerAcctRecord;
import com.temenos.t24.api.records.aajbllockerdetails.AaJblLockerDetailsRecord;
import com.temenos.t24.api.records.aajbllockerparameter.AaJblLockerParameterRecord;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.system.Session;

/**
 * Routine type: NOFILE ENQUIRY
 * EB.API: NOFILELockerDetStatFree
 * SS: NOFILE.LOCKER.DET.STATUS.FREE
 * Business Logic:
 *  *
 * This enquiry hook is responsible for retrieving a refined list of
 * lockers that are available for allocation. It ensures that lockers
 * are not only marked as "Free" but are also not currently referenced
 * in any unauthorised (NAU) locker account records.
 *
 * ---------------------------------------------------------
 * 🔹 PART 1: FILTER & DATA RETRIEVAL
 * ---------------------------------------------------------
 *
 * - Reads input FilterCriteria
 *   → Supports optional filtering using @ID
 *
 * - Retrieves Company Code from session
 *
 * - Fetches FREE lockers from:
 *   AA.JBL.LOCKER.DETAILS
 *   → Condition: STATUS = "Free"
 *   → Filtered by CO.CODE
 *
 * - Applies optional ID filter if provided
 *
 * - Fetches all NAU records from:
 *   AA.JBL.LOCKER.ACCT$NAU
 *
 * ---------------------------------------------------------
 * 🔹 PART 2: NAU LOCKER VALIDATION
 * ---------------------------------------------------------
 *
 * - Iterates through NAU records
 *   → Extracts assigned Locker IDs
 *
 * - Stores assigned lockers in a Set
 *   → Ensures uniqueness
 *   → Enables fast lookup
 *
 * - Purpose:
 *   Prevents selection of lockers already used
 *   in unauthorised records
 *
 * ---------------------------------------------------------
 * 🔹 PART 3: AVAILABLE LOCKER FILTERING
 * ---------------------------------------------------------
 *
 * - Iterates through FREE locker list
 *
 * - Excludes lockers already present in NAU set
 *
 * - For each valid locker:
 *   → Fetches locker details
 *   → Retrieves Locker Type
 *
 * - Fetches parameter details from:
 *   AA.JBL.LOCKER.PARAMETER
 *   → Retrieves description
 *
 * ---------------------------------------------------------
 * 🔹 OUTPUT FORMATTING
 * ---------------------------------------------------------
 *
 * - Prepares result in format:
 *   lockerId * lockerType * description
 *
 * - Adds each valid record to return list
 *
 * - Returns List<String> as enquiry output
 *
 * ---------------------------------------------------------
 * 🔹 OUTPUT
 * ---------------------------------------------------------
 *
 * ✔ Only truly available lockers are returned
 * ✔ Excludes NAU-assigned lockers
 * ✔ Includes descriptive metadata for UI display
 *
 * ---------------------------------------------------------
 * 🔹 EXCLUSIONS / NOTES
 * ---------------------------------------------------------
 *
 * - Exception handling is silent (no logging implemented)
 * - Assumes valid data in parameter table
 * - No pagination or performance optimization applied
 * - NAU validation is strictly enforced
 *
 * @author kawsar
 *
 */
public class GbNFAaJblLockerDetStatFree extends Enquiry{

    @Override
    public List<String> setIds(List<FilterCriteria> filterCriteria, EnquiryContext enquiryContext) {
        List<String> returnList = new ArrayList<>();
        List<String> freeLockerIds = new ArrayList<>();
        List<String> lockerAcctNauIds = new ArrayList<>();
        Set<String> assignedLoc = new HashSet<>();
        String lockerId = null;
        String criteriaID = null;
        DataAccess da = new DataAccess(this);
        Session session = new Session(this);
        AaJblLockerAcctRecord locAccRec = null;
        AaJblLockerDetailsRecord locDetRec = null;
        AaJblLockerParameterRecord locParamRec = null;
        
        String coCode = session.getCompanyId();
        
        for (FilterCriteria fc : filterCriteria){
            String selectionFieldName = fc.getFieldname();
            switch(selectionFieldName){
            case "@ID":
                criteriaID = fc.getValue();
                break;
            default:
            }
        }
        
        
        try{
            String selectStmt = " WITH STATUS EQ Free AND CO.CODE EQ " + coCode;
            if(!criteriaID.isEmpty()){
                selectStmt+=" AND @ID EQ "+criteriaID;
            }
            freeLockerIds = da.selectRecords("BNK", "AA.JBL.LOCKER.DETAILS", "", selectStmt);
            lockerAcctNauIds = da.selectRecords("BNK", "AA.JBL.LOCKER.ACCT$NAU", "", "");
        }catch(Exception e){
            
        }
        
        for(String recIds : lockerAcctNauIds){
            try{
                locAccRec = new AaJblLockerAcctRecord(da.getRecord("AA.JBL.LOCKER.ACCT$NAU", recIds));
                lockerId = locAccRec.getLockerId().getValue();
                
                if ( lockerId != null && !lockerId.isEmpty()){
                    assignedLoc.add(lockerId);
                }
            }catch(Exception e){
                
            }
            
        }
        for(String recIds : freeLockerIds){
            try{
                if(!assignedLoc.contains(recIds))
                {   
                    locDetRec = new AaJblLockerDetailsRecord(
                            da.getRecord("AA.JBL.LOCKER.DETAILS", recIds));

                    String lockerType = locDetRec.getLockerType().getValue();

                    locParamRec = new AaJblLockerParameterRecord(
                            da.getRecord("AA.JBL.LOCKER.PARAMETER", lockerType));

                    String locDes = locParamRec.getDescription().getValue();

                    String result = recIds + "*" + lockerType + "*" + locDes;
                    returnList.add(result);
                }
            }catch(Exception e){

            }
        }  
        return returnList;
    }

}
