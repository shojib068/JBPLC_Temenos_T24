package com.temenos.t24;
 
import java.util.ArrayList;
import java.util.List;
 
import com.temenos.t24.api.complex.eb.enquiryhook.EnquiryContext;
import com.temenos.t24.api.complex.eb.enquiryhook.FilterCriteria;
import com.temenos.t24.api.complex.pp.componentapihook.Account;
import com.temenos.t24.api.hook.system.Enquiry;
import com.temenos.t24.api.records.account.AccountRecord;
import com.temenos.t24.api.records.customer.CustomerRecord;
import com.temenos.t24.api.system.DataAccess;
 
/**
* TODO: Document me!
*
* @author abdul.khaleque
*
*/
public class NoFileAcCusDetails extends Enquiry {
 
    @Override
    public List<String> setIds(List<FilterCriteria> filterCriteria, EnquiryContext enquiryContext) {
        Account acc = new Account();
        DataAccess da = new DataAccess(this);
 
        String fd = filterCriteria.get(0).getFieldname();
        String accId = filterCriteria.get(0).getValue();
 
        List<String> ret = new ArrayList<String>();
 
        if (fd.equals("ACCOUNT")) {
            acc.setAccountId(accId);
 
            try {
                AccountRecord accRec = new AccountRecord(da.getRecord("ACCOUNT", accId));
 
                String wb = accRec.getWorkingBalance().getValue();
 
                String cus = accRec.getCustomer().getValue();
 
                CustomerRecord cusRec = new CustomerRecord(da.getRecord("CUSTOMER", cus));
 
                String cusMnemonic = cusRec.getMnemonic().getValue();
 
                String lnOutstanding = accId + "*" + cus + "*" + wb + "*" + cusMnemonic;
 
                ret.add(lnOutstanding);
 
            } catch (Exception e) {
                throw e;
            }
 
        }
        return ret;
    }
 
}