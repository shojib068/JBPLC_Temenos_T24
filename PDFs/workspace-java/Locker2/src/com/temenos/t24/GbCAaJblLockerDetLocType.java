package com.temenos.t24;

import com.temenos.api.TStructure;
import com.temenos.api.exceptions.T24CoreException;
import com.temenos.t24.api.complex.eb.templatehook.TransactionContext;
import com.temenos.t24.api.hook.system.RecordLifecycle;
import com.temenos.t24.api.records.aajbllockerdetails.AaJblLockerDetailsRecord;
import com.temenos.t24.api.records.aajbllockerparameter.AaJblLockerParameterRecord;
import com.temenos.t24.api.records.ftcommissiontype.FtCommissionTypeRecord;
import com.temenos.t24.api.system.DataAccess;

/**
 * ROUTINE.TYPE : CHECK.ID.RTN
 * Version Attached to: AA.JBL.LOCKER.DETAILS,ADMIN
 * @author Kawsar
 */
public class GbCAaJblLockerDetLocType extends RecordLifecycle{

    @Override
    public void defaultFieldValues(String application, String currentRecordId, TStructure currentRecord,
            TStructure unauthorisedRecord, TStructure liveRecord, TransactionContext transactionContext) {
        DataAccess da = new DataAccess(this);
        AaJblLockerDetailsRecord locDetRec = new AaJblLockerDetailsRecord(currentRecord);
        AaJblLockerParameterRecord locParamRec = null;
        FtCommissionTypeRecord ftCommTypeRec = null;
        String id = currentRecordId;
        String[] parts = null;
        parts = id.split("\\.");
        String locType = parts[2];
        switch(locType){
        case "L":
            locDetRec.setLockerType("LARGE");
            locParamRec = new AaJblLockerParameterRecord(
                    da.getRecord("AA.JBL.LOCKER.PARAMETER", "LARGE"));
            break;
        case "M":
            locDetRec.setLockerType("MEDIUM");
            locParamRec = new AaJblLockerParameterRecord(
                    da.getRecord("AA.JBL.LOCKER.PARAMETER", "MEDIUM"));
            break;
        case "S1":
            locDetRec.setLockerType("SMALL1");
            locParamRec = new AaJblLockerParameterRecord(
                    da.getRecord("AA.JBL.LOCKER.PARAMETER", "SMALL1"));
            break;
        case "S2":
            locDetRec.setLockerType("SMALL2");
            locParamRec = new AaJblLockerParameterRecord(
                    da.getRecord("AA.JBL.LOCKER.PARAMETER", "SMALL2"));
            break;
        default:
            throw new T24CoreException("","AA-LOCKER-ID");            
        }       
        String insComm = "";
        if(locParamRec!=null){
            try{
                insComm = locParamRec.getInsuranceCommission().getValue();
            }catch(Exception e){}
        }
        if(!insComm.isEmpty()){
            try{
                ftCommTypeRec = new FtCommissionTypeRecord(da.getRecord("FT.COMMISSION.TYPE", insComm));
            }catch(Exception e){}
        }
        String flatAmt = "";
        if(ftCommTypeRec!=null){
            try{
                flatAmt = ftCommTypeRec.getCurrency(0).getFlatAmt().getValue();
            }catch(Exception e){}
        }
       if(flatAmt.isEmpty()){
           locDetRec.setInsuranceAmount(flatAmt);
       }
        currentRecord.set(locDetRec.toStructure());        
    }
}
