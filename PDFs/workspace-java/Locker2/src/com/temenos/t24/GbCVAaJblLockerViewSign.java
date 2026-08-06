package com.temenos.t24;

import java.util.ArrayList;
import java.util.List;

import com.temenos.api.TStructure;
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
public class GbCVAaJblLockerViewSign extends Enquiry{

    @Override
    public List<String> setValues(String value, String currentId, TStructure currentRecord,
            List<FilterCriteria> filterCriteria, EnquiryContext enquiryContext) {
        String acctNo = currentId.split("\\.")[0];
        List<String>imgRecIds = new ArrayList<String>();
        List<String>resultIds = new ArrayList<String>();
        DataAccess da = new DataAccess(this);
        Session ss = new Session(this);
        String coCode = ss.getCompanyId();
        String selectStmt = "WITH IMAGE.REFERENCE EQ "+acctNo+" AND CO.CODE EQ "+coCode+" AND IMAGE.TYPE EQ SIGNATURES";
        try{
            imgRecIds = da.selectRecords("", "IM.DOCUMENT.IMAGE", "", selectStmt); 
        }catch(Exception e){}
        
        if (imgRecIds != null) {
            resultIds.addAll(imgRecIds);
        }        
        return resultIds;
    }
}
