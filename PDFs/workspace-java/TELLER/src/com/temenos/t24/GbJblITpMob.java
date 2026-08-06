package com.temenos.t24;

import java.util.ArrayList;
import java.util.List;
import com.temenos.api.TStructure;
import com.temenos.t24.api.complex.eb.templatehook.TransactionContext;
import com.temenos.t24.api.hook.system.RecordLifecycle;
import com.temenos.t24.api.records.account.AccountRecord;
import com.temenos.t24.api.records.customer.CustomerRecord;
import com.temenos.t24.api.records.customer.Phone1Class;
import com.temenos.t24.api.records.teller.TellerRecord;
import com.temenos.t24.api.system.DataAccess;

public class GbJblITpMob extends RecordLifecycle {

    @Override
    public void defaultFieldValues(String application, String currentRecordId,
            TStructure currentRecord, TStructure unauthorisedRecord,
            TStructure liveRecord, TransactionContext transactionContext) {
        TellerRecord tellerRec = new TellerRecord(currentRecord);
        DataAccess da = new DataAccess(this);
        AccountRecord accRec = null;
        CustomerRecord custRec = null;
        String creditAcc = "";
        String custId = "";
        String sms = "";
        List<Phone1Class> phoneList = new ArrayList<>();
        
        if(tellerRec != null){
            try{
                creditAcc = tellerRec.getAccount2().getValue();
            }catch(Exception e){}
        }
        
        if(!creditAcc.isEmpty() && creditAcc != null ){
            try{
                accRec = new AccountRecord(da.getRecord("ACCOUNT",creditAcc));
            }catch(Exception e){}
        }
        
        if(accRec != null){
            try{
                custId = accRec.getCustomer().getValue();
            }catch(Exception e){}
        }
        
        if(!custId.isEmpty() && custId != null){
            try{
                custRec = new CustomerRecord(da.getRecord("CUSTOMER", custId));
            }catch(Exception e){}          
        }
        
        if(custRec != null){
            try{
                phoneList = custRec.getPhone1();
            }catch(Exception e){}          
        }
        
        if(phoneList != null && !phoneList.isEmpty()){
            try{
                sms = phoneList.get(0).getSms1().getValue();
            }catch(Exception e){}            
        }
        
        if(sms != null && !sms.isEmpty()){
            tellerRec.getLocalRefField("LT.MOBILE.NUM").setValue(sms);
        }
        currentRecord.set(tellerRec.toStructure());
    }
}