package com.temenos.t24;

import java.util.ArrayList;
import java.util.List;

import com.temenos.api.exceptions.T24CoreException;
import com.temenos.t24.api.complex.eb.enquiryhook.EnquiryContext;
import com.temenos.t24.api.complex.eb.enquiryhook.FilterCriteria;
import com.temenos.t24.api.hook.system.Enquiry;
import com.temenos.t24.api.records.aaarrangement.AaArrangementRecord;
import com.temenos.t24.api.system.DataAccess;

/**
 * TODO: ENQUIRY AA.ENQ.JBL.REQUEST.PAYOFF
 *
 * @author kawsar
 *
 */
public class GbBRAaJblLockerAcctClosure extends Enquiry{

    @Override
    public List<FilterCriteria> setFilterCriteria(List<FilterCriteria> filterCriteria, EnquiryContext enquiryContext) {
       DataAccess da = new DataAccess(this);
       String selectionCritValue = "";
       
       for( FilterCriteria fc : filterCriteria){
           String selectionFieldName = fc.getFieldname();
           switch(selectionFieldName){
           case "ARRANGEMENT.ID":
               selectionCritValue = fc.getValue(); 
               break;
           default:
           }
       }
       AaArrangementRecord aaArrRec = null;
       String linkedApplId = "";
       try{
           aaArrRec = new AaArrangementRecord(da.getRecord("AA.ARRANGEMENT", selectionCritValue));
       }catch(Exception e){}
       if(aaArrRec!=null){
           try{
               linkedApplId = aaArrRec.getLinkedAppl(0).getLinkedApplId().getValue();
           }catch(Exception e){}
       }
       List<String> lockerAcctIdList = new ArrayList<>();
       String selectStmt = " WITH ACCT.NO EQ "+linkedApplId;
       lockerAcctIdList = da.selectRecords("BNK", "AA.JBL.LOCKER.ACCT", "", selectStmt);
       if(lockerAcctIdList != null && !lockerAcctIdList.isEmpty()){
           throw new T24CoreException("", "AA-LOC-ACCT-CLOSURE");
       }
        return filterCriteria;
    }

}
