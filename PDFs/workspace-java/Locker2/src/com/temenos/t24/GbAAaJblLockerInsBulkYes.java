package com.temenos.t24;

import java.util.List;

import com.temenos.api.TStructure;
import com.temenos.t24.api.complex.eb.servicehook.TransactionData;
import com.temenos.t24.api.complex.eb.templatehook.TransactionContext;
import com.temenos.t24.api.hook.system.RecordLifecycle;
import com.temenos.t24.api.records.aajbllockeracct.AaJblLockerAcctRecord;
import com.temenos.t24.api.records.acchargerequest.AcChargeRequestRecord;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.tables.aajbllockeracct.AaJblLockerAcctTable;

/**
 * TODO: Document me!
 *
 * @author kawsar
 *
 */
public class GbAAaJblLockerInsBulkYes extends RecordLifecycle{

    @Override
    public void postUpdateRequest(String application, String currentRecordId, TStructure currentRecord,
            List<TransactionData> transactionData, List<TStructure> currentRecords,
            TransactionContext transactionContext) {
        DataAccess da = new DataAccess(this);
        String locAcctId = "";
        
        AcChargeRequestRecord req = null;
        try{
            req = new AcChargeRequestRecord(currentRecord);
        }catch(Exception e){}
        if(req!=null){
            try{
                locAcctId = req.getExtraDetails().get(0).getValue();
            }catch(Exception e){}
        }
        AaJblLockerAcctRecord locAcctRec = null;
        if(!locAcctId.isEmpty()){
            try{
                locAcctRec = new AaJblLockerAcctRecord(da.getRecord("AA.JBL.LOCKER.ACCT", locAcctId));
            }catch(Exception e){}
        }
        locAcctRec.setInsurance("YES");
        AaJblLockerAcctTable locAcctTable = null;
        try{
            locAcctTable = new AaJblLockerAcctTable(this);
        }catch(Exception e){}
        try {
            locAcctTable.write(locAcctId, locAcctRec);
        } catch (Exception e) {}
        
    }

}
