package com.temenos.t24;

import java.util.List;

import com.temenos.t24.api.complex.eb.enquiryhook.EnquiryContext;
import com.temenos.t24.api.complex.eb.enquiryhook.FilterCriteria;
import com.temenos.t24.api.hook.system.Enquiry;

/**
 * TODO: Document me!
 *
 * @author Fahim Hasan issue: Suit File
 *
 */
public class CrJblBuildSuitLawyerId extends Enquiry {

    @Override
    public List<FilterCriteria> setFilterCriteria(List<FilterCriteria> filterCriteria, EnquiryContext enquiryContext) {
        // TODO Auto-generated method stub
        String value = "";
        for (int i = 0; i < filterCriteria.size(); i++) {
            value = filterCriteria.get(i).getValue();
        }
        filterCriteria.clear();
        if (value == "101" || value == "102") {
            FilterCriteria f1 = new FilterCriteria();
            f1.setFieldname("DISTRICT.CODE");
            f1.setOperand("EQ");
            f1.setValue(" 100");
            filterCriteria.add(f1);
        } else {
            try {
                filterCriteria.clear();
                FilterCriteria f1 = new FilterCriteria();
                f1.setFieldname("DISTRICT.CODE");
                f1.setOperand("EQ");
                f1.setValue(value + " 100");
                filterCriteria.add(f1);
            } catch (Exception e) {
            }
        }
        return filterCriteria;
    }
}
