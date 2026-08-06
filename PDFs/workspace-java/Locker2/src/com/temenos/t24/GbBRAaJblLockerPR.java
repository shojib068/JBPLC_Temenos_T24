package com.temenos.t24;

import java.util.ArrayList;
import java.util.List;

import com.temenos.api.TField;
import com.temenos.api.exceptions.T24CoreException;
import com.temenos.t24.api.complex.eb.enquiryhook.EnquiryContext;
import com.temenos.t24.api.complex.eb.enquiryhook.FilterCriteria;
import com.temenos.t24.api.hook.system.Enquiry;
import com.temenos.t24.api.records.account.AccountRecord;
import com.temenos.t24.api.system.DataAccess;

/**
 * TODO: Document me!
 *
 * @author kawsar
 *
 */
public class GbBRAaJblLockerPR extends Enquiry{

    @Override
    public List<FilterCriteria> setFilterCriteria(
            List<FilterCriteria> filterCriteria, EnquiryContext enquiryContext) {
       
        DataAccess da = new DataAccess(this);
        AccountRecord accRec = null;
        String selectionCritValue = "";
        String acctNo = "";
        
        List<TField> prList = new ArrayList<>();
        for (FilterCriteria fc : filterCriteria){
            String selectionFieldName = fc.getFieldname();
            
            switch(selectionFieldName){
            
            case "ACCT.NO":
                selectionCritValue = fc.getValue();
                break;
                
            default:
            }
        }
        if(selectionCritValue.contains(".")){
            acctNo = selectionCritValue.split("\\.")[0];
            
        }else{
            acctNo = selectionCritValue;
        }
      try{
          accRec = new AccountRecord(da.getRecord("ACCOUNT", acctNo)); 
      }catch(Exception e){}
      if(accRec != null){
          try{
              prList = accRec.getPostingRestrict();
          }catch(Exception e){}
      }
      if(prList != null ){
          for(int i = 0; i< prList.size(); i++){
              String pr = prList.get(i).getValue();
              if(pr.equals("15")){
                  throw new T24CoreException("","AC-POSTING.RESTRICTION.SET");
              }
          }
      }
        return filterCriteria;
    }
}
