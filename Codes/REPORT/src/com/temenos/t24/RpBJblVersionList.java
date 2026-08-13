package com.temenos.t24;

import java.util.ArrayList;
import java.util.List;

import com.temenos.t24.api.complex.eb.enquiryhook.EnquiryContext;
import com.temenos.t24.api.complex.eb.enquiryhook.FilterCriteria;
import com.temenos.t24.api.hook.system.Enquiry;

/**
 * TODO: Document me!
 *
 * @author kawsar
 *
 */
public class RpBJblVersionList extends Enquiry{

    @Override
    public List<FilterCriteria> setFilterCriteria(List<FilterCriteria> filterCriteria, EnquiryContext enquiryContext) {
        
        List<FilterCriteria> newCriteria = new ArrayList<>();
        String ne = "NE";
        
//        INPUT.ROUTINE
        FilterCriteria inputRtn = new FilterCriteria();
        inputRtn.setFieldname("INPUT.ROUTINE");
        inputRtn.setValue(ne);
        inputRtn.setOperand("");
        newCriteria.add(inputRtn);
        
//        DEFAULT.ROUTINE
        FilterCriteria defaultRtn = new FilterCriteria();
        defaultRtn.setFieldname("DEFAULT.ROUTINE");
        defaultRtn.setValue(ne);
        defaultRtn.setOperand("");
        newCriteria.add(defaultRtn);
        
//        CHECK.REC.RTN
        FilterCriteria checkRecRtn = new FilterCriteria();
        checkRecRtn.setFieldname("CHECK.REC.RTN");
        checkRecRtn.setValue(ne);
        checkRecRtn.setOperand("");
        newCriteria.add(checkRecRtn);
        
//        AFTER.UNAU.RTN
        FilterCriteria afterUnauRtn = new FilterCriteria();
        afterUnauRtn.setFieldname("AFTER.UNAU.RTN");
        afterUnauRtn.setValue(ne);
        afterUnauRtn.setOperand("");
        newCriteria.add(afterUnauRtn);
        
//        BEFORE.AUTH.RTN
        
        FilterCriteria beforeAuthRtn = new FilterCriteria();
        beforeAuthRtn.setFieldname("BEFORE.AUTH.RTN");
        beforeAuthRtn.setValue(ne);
        beforeAuthRtn.setOperand("");
        newCriteria.add(beforeAuthRtn);
        
//        AUTH.ROUTINE
        
        FilterCriteria authRtn = new FilterCriteria();
        authRtn.setFieldname("AUTH.ROUTINE");
        authRtn.setValue(ne);
        authRtn.setOperand("");
        newCriteria.add(authRtn);
        
//        ID.RTN
        FilterCriteria idRtn = new FilterCriteria();
        idRtn.setFieldname("ID.RTN");
        idRtn.setValue(ne);
        idRtn.setOperand("");
        newCriteria.add(idRtn);
        
//        VALIDATION.RTN
        FilterCriteria validationRtn = new FilterCriteria();
        validationRtn.setFieldname("VALIDATION.RTN");
        validationRtn.setValue(ne);
        validationRtn.setOperand("");
        newCriteria.add(validationRtn);
        
        return newCriteria;
    }

}
