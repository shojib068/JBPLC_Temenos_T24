package com.temenos.t24;

import java.util.ArrayList;

import java.util.List;

import com.temenos.api.TStructure;
import com.temenos.t24.api.complex.eb.servicehook.ServiceData;
import com.temenos.t24.api.complex.eb.servicehook.SynchronousTransactionData;
import com.temenos.t24.api.complex.eb.servicehook.TransactionControl;
import com.temenos.t24.api.hook.system.ServiceLifecycle;
import com.temenos.t24.api.records.ebaajbllockeracct.EbAaJblLockerAcctRecord;
import com.temenos.t24.api.records.ebaajbllockerdetails.EbAaJblLockerDetailsRecord;
import com.temenos.t24.api.records.ebaajbllockerparameter.EbAaJblLockerParameterRecord;
import com.temenos.t24.api.records.ftcommissiontype.FtCommissionTypeRecord;
import com.temenos.t24.api.records.tax.TaxRecord;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.system.Date;

/**
 * TODO: Document me!
 *
 * @author kawsar
 *
 */
public class GbEbAaJblLockerFeeCollection extends ServiceLifecycle{

    private String chargeReqId = "";
    private boolean ofsFlag = false;
    
    //get ids
    @Override
    public List<String> getIds(ServiceData serviceData, List<String> controlList) {
        
        DataAccess da = new DataAccess(this);
        List<String> ids = new ArrayList<>();
        try{
            Date dt = new Date(this);
            String today = dt.getDates().getToday().getValue();
            
            // for 1st January
            
            if(today.substring(4, 8).equals("0101")){
                ids = da.selectRecords("BNK", "EB.AA.JBL.LOCKER.ACCT", "", "WITH STATUS NE CLOSED ");
                
            }
            //daily cob
            else{
                ids = da.selectRecords("BNK", "EB.AA.JBL.LOCKER.FAIL", "", "");
            }
            
        }catch(Exception e){
            throw e;
        }
        return ids;
    }


    
    //update ids
    @Override
    public void updateRecord(String id, ServiceData serviceData, String controlItem,
            TransactionControl transactionControl, List<SynchronousTransactionData> transactionData,
            List<TStructure> records) {
       DataAccess da = new DataAccess(this);
       
       try{
           Date dt = new Date(this);
           String today = dt.getDates().getToday().getValue();
           boolean isAnnualRun = today.substring(4, 8).equals("0101");
           
           String lockerId = "";
           String debitAccount = "";
           String feeAmount = "";
           String lockerType = "";
           String commission = "";
           String taxCode = "";
           int flatAmt = 0;
           String rate = "";
           if(isAnnualRun){
               EbAaJblLockerAcctRecord lockerRec = new EbAaJblLockerAcctRecord(da.getRecord("EB.AA.JBL.LOCER.ACCT", id));
               
               lockerId = id;
               debitAccount = lockerRec.getAcctNo().getValue();
               EbAaJblLockerDetailsRecord locDetRec = new EbAaJblLockerDetailsRecord(da.getRecord("EB.AA.JBL.LOCKER.DETAILS", lockerId));
               lockerType = locDetRec.getLockerType().getValue();
               EbAaJblLockerParameterRecord locParamRec = new EbAaJblLockerParameterRecord(da.getRecord("EB.AA.JBL.LOCKER.PARAMETER", lockerType));
               commission = locParamRec.getCommission().getValue();
               FtCommissionTypeRecord commTypeRec= new FtCommissionTypeRecord(da.getRecord("FT.COMMISSION.TYPE", commission));
               taxCode = commTypeRec.getTaxCode().getValue();
               flatAmt = commTypeRec.getFlatAmt().getValue();
               
               switch(commission){
               case "LOCKERSMALL":
                   flatAmt = 2000;
                   break;
               case "SMLKRSTAFF":
                   flatAmt = 1000;
                   break;
               case "LKKMEDIUM":
                   flatAmt = 2500;
                   break;
               case "LKRMEDSTAFF":
                   flatAmt = 1250;
                   break;
               case "LOCKERLARGE":
                   flatAmt = 3000;
                   break;
               case "LKRLRGSTAFF":
                   flatAmt = 1500;
                   break;
               }
               TaxRecord taxRec = new TaxRecord(da.getRecord("TAX", taxCode));
               rate = taxRec.getRate().getValue();
               
               feeAmount = String.format("%.2f", flatAmt + (flatAmt * Double.parseDouble(rate) / 100.0));

               
           }
             
       }catch(Exception e){throw e;}
       
    }
    
    
    

}
