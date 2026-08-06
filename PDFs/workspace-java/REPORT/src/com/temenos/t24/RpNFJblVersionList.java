package com.temenos.t24;

import java.util.ArrayList;
import java.util.List;

import com.temenos.t24.api.complex.eb.enquiryhook.EnquiryContext;
import com.temenos.t24.api.complex.eb.enquiryhook.FilterCriteria;
import com.temenos.t24.api.hook.system.Enquiry;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.system.Session;

/**
 * TODO: Document me!
 *
 * @author kawsar
 *
 */
public class RpNFJblVersionList extends Enquiry{

    @Override
    public List<String> setIds(List<FilterCriteria> filterCriteria, EnquiryContext enquiryContext) {
        List<String> versionList = new ArrayList<>();
        DataAccess da = new DataAccess(this);
        Session ss = new Session(this);
        
        String criteriaId = "";
        String selectStatement = "";
        String coCode = ss.getCompanyId();;
        

            
        for( FilterCriteria fc : filterCriteria){
            String selectFieldName = fc.getFieldname();
            switch(selectFieldName){
            case "VERSION.NAME":
                criteriaId = fc.getValue().trim();
                break;
            default:
                
                }
            }

        
      
            String routineCondition = "(INPUT.ROUTINE NE '' OR DEFAULT.ROUTINE NE '' OR "
                                        + "CHECK.REC.RTN NE '' OR AFTER.UNAU.RTN NE '' OR "
                                        + "VALIDATION.RTN NE '' OR ID.RTN NE '' OR "
                                        + "AUTH.ROUTINE NE '' OR BEFORE.AUTH.RTN NE '')";
            
            try{
                if (criteriaId == null || criteriaId.isEmpty()) {
                    selectStatement = " WITH CO.CODE EQ '" + coCode + "' AND " + routineCondition;
                } else {
                    selectStatement = " WITH @ID EQ '" + criteriaId + "' AND CO.CODE EQ '" 
                            + coCode + "' AND " + routineCondition;
                }
            }catch(Exception e){}
           
            try{
                versionList = da.selectRecords("", "VERSION", "", selectStatement);
            }catch(Exception e){}
            

        if(versionList == null || versionList.isEmpty())
            return new ArrayList<>();
        else
            return versionList;
    }

}