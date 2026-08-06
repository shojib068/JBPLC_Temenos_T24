package com.temenos.t24;

import java.util.List;

import com.temenos.api.TStructure;
import com.temenos.t24.api.complex.eb.servicehook.TransactionData;
import com.temenos.t24.api.complex.eb.templatehook.TransactionContext;
import com.temenos.t24.api.hook.system.RecordLifecycle;
import com.temenos.t24.api.records.ebaajbllockeracct.EbAaJblLockerAcctRecord;
import com.temenos.t24.api.records.ebaajbllockerdetails.EbAaJblLockerDetailsRecord;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.tables.ebaajbllockerdetails.EbAaJblLockerDetailsTable;

/**
 * TODO: after authorization, the status and locker account of EB.AA.JBL.LOCKER.DETAILS will be updated
 *
 * @author kawsar
 *
 */
public class GbEbAaJblLockerDetStat extends RecordLifecycle{

    @Override
    public void postUpdateRequest(String application, String currentRecordId, TStructure currentRecord,
            List<TransactionData> transactionData, List<TStructure> currentRecords,
            TransactionContext transactionContext) {
        
        DataAccess da = new DataAccess(this);
        EbAaJblLockerAcctRecord lockerAccRec = null;
        EbAaJblLockerDetailsRecord lockerDetailsRec = null;
        EbAaJblLockerDetailsTable lockerDetailsTable = null;
        
        String lockerId = "";
        String status = "";
        String lockerAcct = "";
        
        // find the lockerId and account no
        
        
        try{
            lockerAccRec = new EbAaJblLockerAcctRecord(currentRecord);
            lockerId = lockerAccRec.getLockerId().getValue();
//            acctNo = lockerAccRec.getAcctNo().getValue(); 
        }catch(Exception e){
            throw e;
            }
        
        
        //update status and locker account of EB.AA.JBL.LOCKER.DETAILS
        
        try{
            lockerDetailsRec = new EbAaJblLockerDetailsRecord(da.getRecord("EB.AA.JBL.LOCKER.DETAILS", lockerId));
            status = lockerDetailsRec.getStatus().getValue();
            lockerAcct = lockerDetailsRec.getLockerAcct().getValue();
            
            if(status.equals("Free") && lockerAcct.isEmpty())
            {
                lockerDetailsRec.setStatus("Used");
                lockerDetailsRec.setLockerAcct(currentRecordId);
     
                lockerDetailsTable = new EbAaJblLockerDetailsTable(this);
                lockerDetailsTable.write(lockerId, lockerDetailsRec);
            }
            
            else if(!status.equals("Free"))
            {
                lockerAccRec.getLockerId().setError("This Locker is already taken");
            }
        }catch(Exception e){
            
        }
    }

   
}
