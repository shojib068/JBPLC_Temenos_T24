package com.temenos.t24;

import java.util.ArrayList;
import java.util.List;

import com.temenos.api.TStructure;
import com.temenos.api.TValidationResponse;
import com.temenos.api.exceptions.T24CoreException;
import com.temenos.t24.api.complex.eb.templatehook.TransactionContext;
import com.temenos.t24.api.hook.system.RecordLifecycle;
import com.temenos.t24.api.records.aajbllockeracct.AaJblLockerAcctRecord;
import com.temenos.t24.api.records.aajbllockeracct.MandateCustomerIdClass;
import com.temenos.t24.api.records.aajbllockeracct.NomineeTypeClass;
import com.temenos.t24.api.records.aajbllockerdetails.AaJblLockerDetailsRecord;
import com.temenos.t24.api.system.DataAccess;

/**
 * TODO: Document me!
 *
 * @author kawsar
 *
 */
public class GbInAaJblLockerAcctError extends RecordLifecycle{

    @Override
    public TValidationResponse validateRecord(String application, String currentRecordId, TStructure currentRecord,
            TStructure unauthorisedRecord, TStructure liveRecord, TransactionContext transactionContext) {
        AaJblLockerAcctRecord locAcctRec = new AaJblLockerAcctRecord(currentRecord);
        DataAccess da = new DataAccess(this);
        AaJblLockerDetailsRecord locDetRec = null;
        String lockerId = "";
        String locStatus = "";
        String locDetAccNo = "";
        double percent = 0.0;
        
        
        List<MandateCustomerIdClass> manCusIdClassList = new ArrayList<>();
        List <NomineeTypeClass> nomTypeClassList = new ArrayList<>();
        List<String> nauLocIds = new ArrayList<>();
        
        
        if(locAcctRec != null){
            try{
                lockerId = locAcctRec.getLockerId().getValue();
            }catch(Exception e){}
        }
        
//        locker id error
        
        if(!lockerId.isEmpty()){
            try{
                locDetRec = new AaJblLockerDetailsRecord(da.getRecord("AA.JBL.LOCKER.DETAILS", lockerId));  
            }catch(Exception e){}
            try{
                nauLocIds = da.selectRecords("BNK", "AA.JBL.LOCKER.ACCT$NAU", "", " WITH LOCKER.ID EQ "+lockerId);
            }catch(Exception e){}
        }
        else{
            locAcctRec.getLockerId().setError("Locker ID cannot be empty");
        }
        
        
        if(locDetRec != null){
            try{
                locStatus = locDetRec.getStatus().getValue();
            }catch(Exception e){}
            try{
                locDetAccNo = locDetRec.getLockerAcct().getValue();
            }catch(Exception e){}
        }
        else{
         locAcctRec.getLockerId().setError("Invalid Locker Account");   
        }
        
        if(nauLocIds != null){
            for( String id: nauLocIds ){
                if(!id.equals(currentRecordId)){
                    locAcctRec.getLockerId().setError("This locker is already taken and unauthorised");
                }
            }
        }
        
        if(!locStatus.isEmpty() && 
                ( "USED".equalsIgnoreCase(locStatus) || 
                        "DAMAGED".equalsIgnoreCase(locStatus) || 
                "MAINTENANCE".equalsIgnoreCase(locStatus)) 
                && !locDetAccNo.isEmpty() && !locDetAccNo.equals(currentRecordId)){
            locAcctRec.getLockerId().setError("Cannot Allovate this locker. "+ lockerId+" already "+locStatus);
            
        }
//        mandate customer error
        
        manCusIdClassList = locAcctRec.getMandateCustomerId();
        if(manCusIdClassList!= null){
            for(int i=0; i<manCusIdClassList.size(); i++){
                String manRelation = "";
                try{
                    manRelation = manCusIdClassList.get(i).getMandateRelation().getValue();
                }catch(Exception e){}
                if(manRelation.isEmpty() || manRelation == null){
                    manCusIdClassList.get(i).getMandateRelation().setError("Give mandate relation");
                }               
            }
        }
 
//        nominee type error 
        nomTypeClassList = locAcctRec.getNomineeType();
        if(nomTypeClassList != null ){
            for(int i=0; i<nomTypeClassList.size(); i++){
                String nomCustId = "";
                String personEntId = "";
                String relation = "";
                String percentage = "";
                String nomType = "";
                
                
//                relation error
                try{
                    relation = nomTypeClassList.get(i).getNomineeRelation().getValue();
                }catch(Exception e){}
                if(relation.isEmpty()){
                    nomTypeClassList.get(i).getNomineeRelation().setError("Relation cannot be empty");
                }
//                percentage error
                try{
                    percentage = nomTypeClassList.get(i).getNomineePercentage().getValue();
                }catch(Exception e){}
                if(percentage.isEmpty()){
                    nomTypeClassList.get(i).getNomineePercentage().setError("Percentage cannot be empty");
                }else if(!percentage.isEmpty()) {
                    try {
                        double p = Double.parseDouble(percentage);
                        percent += p;
                    } catch (Exception e) {}
                }

                try{
                    nomType = nomTypeClassList.get(i).getNomineeType().getValue(); 
                }catch(Exception e){}
                if("CUSTOMER".equalsIgnoreCase(nomType)){
                    try{
                        personEntId = nomTypeClassList.get(i).getPersonEntityId().getValue();
                    }catch(Exception e){}
                    if(!personEntId.isEmpty()){
                        nomTypeClassList.get(i).getPersonEntityId().setError(
                                "No value when type is Customer");
                    }
                }
                if("PERSON.ENTITY".equalsIgnoreCase(nomType)){
                    try{
                        nomCustId = nomTypeClassList.get(i).getNomineeCustomerId().getValue();
                    }catch(Exception e){}
                    if(!nomCustId.isEmpty()){
                        nomTypeClassList.get(i).getNomineeCustomerId().setError(
                                "No value when Nominee type is Person Entity");
                    }
                }
                        
            }
        } 
        if (Math.abs(percent - 100.0) > 0.001) {
            throw new T24CoreException("","ST-INVALID.PERCENTAGE");
        }
        return locAcctRec.getValidationResponse();   
    }

}
