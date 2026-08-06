package com.temenos.t24;

import java.util.List;

import com.temenos.api.LocalRefGroup;
import com.temenos.api.LocalRefList;
import com.temenos.api.TStructure;
import com.temenos.t24.api.complex.eb.servicehook.TransactionData;
import com.temenos.t24.api.complex.eb.templatehook.TransactionContext;
import com.temenos.t24.api.hook.system.RecordLifecycle;
import com.temenos.t24.api.records.aajbllockeracct.AaJblLockerAcctRecord;
import com.temenos.t24.api.records.ebjblsdsaentrydetails.EbJblSdsaEntryDetailsRecord;
import com.temenos.t24.api.records.ebjblsdsaentrydetails.OrgTransRefNoClass;
import com.temenos.t24.api.records.fundstransfer.FundsTransferRecord;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.system.Session;

/**
 * TODO: Document me!
 *
 * @author kawsar
 *
 */
public class GbAAaJblLockerBreakupAdj extends RecordLifecycle{

    @Override
    public void postUpdateRequest(String application, String currentRecordId, TStructure currentRecord,
            List<TransactionData> transactionData, List<TStructure> currentRecords,
            TransactionContext transactionContext) {
        AaJblLockerAcctRecord locAccRec = new AaJblLockerAcctRecord(currentRecord);
        DataAccess da = new DataAccess(this);
        Session ss = new Session(this);
        String coCode = ss.getCompanyId();
        String acctno = "";
        String keyCharge = "";
        String drCr = "DR";
        String lockerAcctId = currentRecordId;
        
        try{
            acctno = locAccRec.getAcctNo().getValue();
        }catch(Exception e){}
        try{
            keyCharge = locAccRec.getKeyCharge().getValue();
        }catch(Exception e){}
        
        
        // =========================
        // FUNDS.TRANSFER,LOCKER.OPENING.CRG.OFS
        // =========================
        
        FundsTransferRecord ftRecord = null;
        try{
            ftRecord = new FundsTransferRecord(this);
            ftRecord.setTransactionType("ACLK");
            ftRecord.setDebitAcctNo("BDT17608");
            ftRecord.setDebitCurrency("BDT");
            ftRecord.setDebitAmount(keyCharge);
            
            ftRecord.setCreditAcctNo(acctno);
            ftRecord.setCreditCurrency("BDT");
            
            ftRecord.getLocalRefField("LT.OLD.PO.NO").setValue(lockerAcctId);
            ftRecord.setOrderingBank("JBL", 0);
            ftRecord.setPaymentDetails("REFUND KEYCHARGE", 0);
        }catch(Exception e){}
        
//        OFS 1
        try{
            TransactionData   txn1 = new TransactionData();
            
            txn1.setFunction("INPUT");
//            txn1.setTransactionId(id);
            txn1.setUserName("INPUTT");
//            txn1.setNumberOfAuthoriser("0");
            txn1.setSourceId("LOCKER.OFS");                
            txn1.setVersionId("FUNDS.TRANSFER,LOCKER.OPENING.CRG.OFS");
            
            transactionData.add(txn1);
            currentRecords.add(ftRecord.toStructure());
        }catch(Exception e){}
        
        // =========================
        // EB.JBL.SDSA.ENTRY.DETAILS,LOCKER.OFS
        // =========================   
        
        
        EbJblSdsaEntryDetailsRecord sdsaEntryDetailsRec = null;
        try{

            List<String> SdsaEntryId= da.selectRecords("BNK", "EB.JBL.SDSA.ENTRY.DETAILS", "", " WITH REF.NO EQ "+lockerAcctId+" AND CO.CODE EQ "+coCode);
            for( String ids: SdsaEntryId){
                sdsaEntryDetailsRec = new EbJblSdsaEntryDetailsRecord(this);
                
                OrgTransRefNoClass orgTransRefNoClass = new OrgTransRefNoClass();
                orgTransRefNoClass.setOrgDrcr(drCr);
                sdsaEntryDetailsRec.setTotAdjAmt(keyCharge);
                sdsaEntryDetailsRec.setOutstandingAmt("0.0");
                
                //OFS 2
                
                try {
                            
                            TransactionData   txn2 = new TransactionData();
                            
                            txn2.setFunction("INPUT");
                            txn2.setTransactionId(ids);
                            txn2.setUserName("INPUTT");
                            txn2.setNumberOfAuthoriser("0");
                            txn2.setSourceId("BULK.OFS");                
                            txn2.setVersionId("EB.JBL.SDSA.ENTRY.DETAILS,LOCKER.OFS");
                            
                            transactionData.add(txn2);
                            currentRecords.add(sdsaEntryDetailsRec.toStructure());
                        } catch (Exception e) {}
            }
        }catch(Exception e){}      
    }

}
