package com.temenos.t24;

import java.util.ArrayList;
import java.util.List;

import com.temenos.api.TStructure;
import com.temenos.t24.api.complex.eb.templatehook.TransactionContext;
import com.temenos.t24.api.hook.system.RecordLifecycle;
import com.temenos.t24.api.records.aajbllockeracct.AaJblLockerAcctRecord;
import com.temenos.t24.api.records.account.AccountRecord;
import com.temenos.t24.api.records.customer.CustomerRecord;
import com.temenos.t24.api.records.customer.Phone1Class;
import com.temenos.t24.api.system.DataAccess;

/**
 * TODO: Document me!
 *
 * @author kawsar
 *
 */
public class GbCAaJblLockerAcctDefVal extends RecordLifecycle{

    @Override
    public void defaultFieldValues(String application, String currentRecordId, TStructure currentRecord,
            TStructure unauthorisedRecord, TStructure liveRecord, TransactionContext transactionContext) {
        AaJblLockerAcctRecord locAcctRec = new AaJblLockerAcctRecord(currentRecord);
        DataAccess da = new DataAccess(this);
        AccountRecord accRec = null;
        CustomerRecord cusRec = null;
        String acctNo = currentRecordId.split("\\.")[0];
        String customerId = "";
        String cusName = "";
        String phone = "";
        List<Phone1Class> phoneList = new ArrayList<>();
        try{
            accRec = new AccountRecord(da.getRecord("ACCOUNT", acctNo)); 
        }catch(Exception e){}
        
        if(accRec != null){
            try{
                customerId = accRec.getCustomer().getValue(); 
            }catch(Exception e){}           
        }
        
        if(!customerId.isEmpty()){
            try{
                cusRec = new CustomerRecord(da.getRecord("CUSTOMER", customerId));
            }catch(Exception e){}
        }
        
        if(cusRec != null){
            try{
                cusName = cusRec.getShortName(0).getValue();
            }catch(Exception e){}
        }
        
        try{
            phoneList = cusRec.getPhone1();
        }catch(Exception e){}
        
        if(phoneList != null && !phoneList.isEmpty()){
            try{
                phone = phoneList.get(0).getSms1().getValue();
            }catch(Exception e){}
        }
        
//        set default values
        locAcctRec.setAcctNo(acctNo);
        locAcctRec.setAcctName(cusName);
        locAcctRec.setCustomer(customerId);
        locAcctRec.setMobileNumber(phone);
        
        currentRecord.set(locAcctRec.toStructure());
        
        
    }

}
