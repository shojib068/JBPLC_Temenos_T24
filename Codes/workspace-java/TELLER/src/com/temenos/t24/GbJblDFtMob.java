package com.temenos.t24;

import java.util.ArrayList;
import java.util.List;

import com.temenos.api.TStructure;
import com.temenos.t24.api.complex.eb.templatehook.TransactionContext;
import com.temenos.t24.api.hook.system.RecordLifecycle;
import com.temenos.t24.api.records.account.AccountRecord;
import com.temenos.t24.api.records.customer.CustomerRecord;
import com.temenos.t24.api.records.customer.Phone1Class;
import com.temenos.t24.api.records.fundstransfer.FundsTransferRecord;
import com.temenos.t24.api.system.DataAccess;

public class GbJblDFtMob extends RecordLifecycle {

    @Override
    public void defaultFieldValues(String application, String currentRecordId,
            TStructure currentRecord, TStructure unauthorisedRecord,
            TStructure liveRecord, TransactionContext transactionContext) {
        FundsTransferRecord ftRec = new FundsTransferRecord(currentRecord);
        DataAccess da = new DataAccess(this);
        
        AccountRecord accRec = null;
        CustomerRecord custRec = null;
        
        String debitAcc = "";
        String custId = "";
        String sms = "";
        
        List<Phone1Class> phoneList = new ArrayList<>();
        
        if(ftRec != null){
            try{
                debitAcc = ftRec.getDebitAcctNo().getValue();
            }catch(Exception e){}
        }
        if(!debitAcc.isEmpty() && debitAcc != null){
            try{
                accRec = new AccountRecord(da.getRecord("ACCOUNT", debitAcc));
            }catch(Exception e){}
        }
        
        if( accRec != null){
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
            ftRec.getLocalRefField("LT.MOBILE.NUM").setValue(sms);
        }
        
        currentRecord.set(ftRec.toStructure());


    }
}