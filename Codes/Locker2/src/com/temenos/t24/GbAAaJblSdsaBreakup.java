package com.temenos.t24;

import java.util.List;

import com.temenos.api.LocalRefGroup;
import com.temenos.api.LocalRefList;
import com.temenos.api.TStructure;
import com.temenos.t24.api.complex.eb.servicehook.TransactionData;
import com.temenos.t24.api.hook.system.RecordLifecycle;
import com.temenos.t24.api.records.ebjblsdsaentrydetails.EbJblSdsaEntryDetailsRecord;
import com.temenos.t24.api.records.ebjblsdsaentrydetails.OrgTransRefNoClass;
import com.temenos.t24.api.records.fundstransfer.FundsTransferRecord;

public class GbAAaJblSdsaBreakup extends RecordLifecycle {

    @Override
    public void postUpdateRequest(String application, String currentRecordId, TStructure currentRecord,
            List<TransactionData> transactionData, List<TStructure> currentRecords,
            com.temenos.t24.api.complex.eb.templatehook.TransactionContext transactionContext) {
        
        String refNo = "";
        String creditAccNo = "";
        String orgTransCurrency = "";
        String orgParticular = "";
        String sdsaId = "";
        String creditCurrency = "";
        String creditValueDate = "";
        String acNumber = "";
        String orgAmt = "";
        String payDeString = "";
        String acType = "L";
        String drCr = "CR";
        String orgTransRefNo = currentRecordId;
        FundsTransferRecord ftRec = new FundsTransferRecord(currentRecord);
        EbJblSdsaEntryDetailsRecord ebJblSdsaEntryDetailsRecord = null;
        
        if(ftRec!= null){
            try{
                refNo = ftRec.getLocalRefField("LT.OLD.PO.NO").getValue();
            }catch(Exception e){}
            try{
                creditAccNo = ftRec.getCreditAcctNo().getValue();
            }catch(Exception e){}
            try{
                creditCurrency = ftRec.getCreditCurrency().getValue();
            }catch(Exception e){}
            try{
                creditValueDate = ftRec.getCreditValueDate().getValue();
            }catch(Exception e){}
            try{
                orgAmt = ftRec.getDebitAmount().getValue();
            }catch(Exception e){}
            
        }
        
        orgTransCurrency = creditCurrency;
        orgParticular = refNo;
        acNumber = creditAccNo;
        sdsaId = acNumber + refNo;
        
        LocalRefList PaymentDetails = ftRec.getLocalRefGroups("LT.FT.DR.DES");
        for (LocalRefGroup localRefGroup : PaymentDetails) 
        {
            payDeString = localRefGroup.getLocalRefField("LT.FT.DR.DES").getValue();
        }
        
        try{
            ebJblSdsaEntryDetailsRecord = new EbJblSdsaEntryDetailsRecord(this);
            
            
            ebJblSdsaEntryDetailsRecord.setRefNo(refNo);
            ebJblSdsaEntryDetailsRecord.setAcNumber(acNumber);
            ebJblSdsaEntryDetailsRecord.setAcType(acType);
            ebJblSdsaEntryDetailsRecord.setPaymentDetails(payDeString);
            
            
            OrgTransRefNoClass orgTransRefNoClass = new OrgTransRefNoClass();
            orgTransRefNoClass.setOrgTransRefNo(orgTransRefNo);
            orgTransRefNoClass.setOrgAmt(orgAmt);
            orgTransRefNoClass.setOrgTransCur(orgTransCurrency);
            orgTransRefNoClass.setOrgDate(creditValueDate);
            orgTransRefNoClass.setOrgDrcr(drCr);
            orgTransRefNoClass.setOrgParticular(orgParticular);
            ebJblSdsaEntryDetailsRecord.setOrgTransRefNo(orgTransRefNoClass, 0);
            
            ebJblSdsaEntryDetailsRecord.setTotOrgAmt(orgAmt);
            ebJblSdsaEntryDetailsRecord.setTotAdjAmt("0.0");
            ebJblSdsaEntryDetailsRecord.setOutstandingAmt(orgAmt);
        }catch(Exception e){}
        
        try {
            
            TransactionData   txn1 = new TransactionData();
            
            txn1.setFunction("INPUT");
            txn1.setTransactionId(sdsaId);
            txn1.setUserName("INPUTT");
            txn1.setNumberOfAuthoriser("0");
            txn1.setSourceId("BULK.OFS");                
            txn1.setVersionId("EB.JBL.SDSA.ENTRY.DETAILS,LOCKER.OFS");
            
            transactionData.add(txn1);
            currentRecords.add(ebJblSdsaEntryDetailsRecord.toStructure());
        } catch (Exception e) {}
        

    }  
}