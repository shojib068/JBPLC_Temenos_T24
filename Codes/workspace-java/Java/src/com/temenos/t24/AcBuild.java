package com.temenos.t24;
 
import java.util.List;
 
import com.temenos.t24.api.complex.eb.enquiryhook.EnquiryContext;
import com.temenos.t24.api.complex.eb.enquiryhook.FilterCriteria;
import com.temenos.t24.api.hook.system.Enquiry;
 
/**
* TODO: Document me!
*build routine
* @author kawsar
*
*/
public class AcBuild extends Enquiry {
 
    @Override
    public List<FilterCriteria> setFilterCriteria(List<FilterCriteria> filterCriteria, EnquiryContext enquiryContext) {
        // TODO Auto-generated method stub
        FilterCriteria newfilterCri = new FilterCriteria();
        String fieldName = filterCriteria.get(0).getFieldname();
        String fieldValue = filterCriteria.get(0).getValue();
        newfilterCri.setFieldname("WORKING.BALANCE");
        newfilterCri.setOperand("RG");
        if(fieldName.equals("CATEGORY")){
            if(fieldValue.equals("1001")){
                newfilterCri.setValue("0 10000");
            }
        }
        filterCriteria.add(newfilterCri);

        return filterCriteria;
    }

 
}