package com.temenos.t24;

import java.util.List;

import com.temenos.api.TStructure;
import com.temenos.api.TValidationResponse;
import com.temenos.t24.api.complex.eb.templatehook.TransactionContext;
import com.temenos.t24.api.hook.system.RecordLifecycle;
import com.temenos.t24.api.records.aagblockerparam.AaGbLockerParamRecord;
import com.temenos.t24.api.records.aajbllockeracct.AaJblLockerAcctRecord;
import com.temenos.t24.api.records.aajbllockerdetails.AaJblLockerDetailsRecord;
import com.temenos.t24.api.records.aajbllockerparameter.AaJblLockerParameterRecord;
import com.temenos.t24.api.records.account.AccountRecord;
import com.temenos.t24.api.records.customer.CustomerRecord;
import com.temenos.t24.api.records.ftcommissiontype.CurrencyClass;
import com.temenos.t24.api.records.ftcommissiontype.FtCommissionTypeRecord;
import com.temenos.t24.api.records.tax.TaxRecord;
import com.temenos.t24.api.system.DataAccess;
/**
 * * Routine: Input.Routine
 * Vesrion Attached to: AA.JBL.LOCKER.ACCT,INPUT
 * Business Logic:
 * 
 * 
 */
public class GbInAaJblLockerAcctOpenLoc extends RecordLifecycle {

    @Override
    public TValidationResponse validateRecord(String application, String currentRecordId,
            TStructure currentRecord, TStructure unauthorisedRecord,
            TStructure liveRecord, TransactionContext transactionContext) {

        DataAccess da = new DataAccess(this);

        AaJblLockerAcctRecord lockerAccRec = new AaJblLockerAcctRecord(currentRecord);
        AaJblLockerDetailsRecord locDetRec = null;
        /* =========================================================
         * 🔹 PART 2: CHARGE & BALANCE VALIDATION (SECOND ROUTINE)
         * ========================================================= */

        double minBalance = 0.0;
        double commissionAmount = 0.0;
        double keyCommAmt = 0.0;
        double taxRate = 0.0;
        double workingBalance = 0.0;
        double totalAmount = 0.0;
        try {
            AaJblLockerParameterRecord paramRec = null;
            CustomerRecord cusRec = null;
            FtCommissionTypeRecord ftRec = null;
            // Locker type
            
            String lockerType = "";
            String commId = "";
            String staffCommId = "";
            String cusStatus = "";
            String applicableCommId = "";
            String taxCode = "";
            String wbStr = "";
            
            List<CurrencyClass> currencyList = null;
            String lockerId = "";
            try{
                lockerId = lockerAccRec.getLockerId().getValue();
            }catch(Exception e){}
            if(!lockerId.isEmpty()){
                try{
                    locDetRec = new AaJblLockerDetailsRecord(da.getRecord("AA.JBL.LOCKER.DETAILS", lockerId));
                }catch(Exception e){}
            }
            if(locDetRec!=null){
                try{
                    lockerType = locDetRec.getLockerType().getValue();                
                }catch(Exception e){}               
            }

            
            // Parameters
            if(!lockerType.isEmpty()){
                paramRec = new AaJblLockerParameterRecord(
                        da.getRecord("AA.JBL.LOCKER.PARAMETER", lockerType));
            }
            if(paramRec != null){
                try{
                    commId = paramRec.getCommission().getValue();  
                }catch(Exception e){}
                try{
                    staffCommId = paramRec.getStaffCommission().getValue();  
                }catch(Exception e){}                
            }

            String customer = "";
            String acctNo ="";
            try{
                customer = lockerAccRec.getCustomer().getValue();
            }catch(Exception e){}
            try{
                acctNo = lockerAccRec.getAcctNo().getValue();
            }catch(Exception e){}
            // Customer
            if(!customer.isEmpty()){
                cusRec = new CustomerRecord(da.getRecord("CUSTOMER", customer)); 
            }
            if(cusRec != null){
                try{
                    cusStatus = cusRec.getCustomerStatus().getValue(); 
                }catch(Exception e){}                
            }


            

            applicableCommId = "7".equals(cusStatus) ? staffCommId : commId;

            // Commission
            if(!applicableCommId.isEmpty()){
                ftRec = new FtCommissionTypeRecord(da.getRecord("FT.COMMISSION.TYPE", applicableCommId));   
            }
            
            if(ftRec != null){
                currencyList = ftRec.getCurrency(); 
                if (currencyList != null) {
                    for (int i = 0; i < currencyList.size(); i++) {
                        String flatAmt = currencyList.get(i).getFlatAmt().getValue();
                        try {
                            if (flatAmt != null && !flatAmt.trim().isEmpty()) {
                                commissionAmount += Double.parseDouble(flatAmt.trim());
                            }
                        } catch (NumberFormatException e) {}
                    }
                }
            }
         
            // Locker Key Charge Commission
            
            AaGbLockerParamRecord locParamRec = null;
            String locKeyCharge = "";
            try{
                locParamRec = new AaGbLockerParamRecord(da.getRecord("AA.GB.LOCKER.PARAM", "SYSTEM"));
            }catch(Exception e){}
            if(locParamRec!=null){
                try{
                    locKeyCharge = locParamRec.getLockerKeyCharge().getValue();
                }catch(Exception e){}
            }
            if(!locKeyCharge.isEmpty()){
                keyCommAmt = Double.parseDouble(locKeyCharge);
            }
            // Tax
            if(ftRec!=null){
                try{
                    taxCode = ftRec.getTaxCode().getValue(); 
                }catch(Exception e){}               
            }
            
            if (!taxCode.isEmpty()) {
                try {
                    List<String> taxRecList = da.selectRecords(
                            "BNK",
                            "TAX",
                            "",
                            "WITH @ID LIKE '" + taxCode + "...'"
                        );
                    if(taxRecList!=null){
                        for(String id : taxRecList){
                            TaxRecord taxRec =
                                    new TaxRecord(da.getRecord("TAX", id));
                            if(taxRec != null){
                                String rateStr = "";
                                try{
                                    rateStr = taxRec.getRate().getValue(); 
                                }catch(Exception e){}
                                if (!rateStr.isEmpty()) {
                                    taxRate = Double.parseDouble(rateStr);
                                }  
                            }                        
                        }                        
                    }       
                } catch (Exception e) {}
            }

            // Total
            totalAmount = keyCommAmt + commissionAmount +
                    (commissionAmount * taxRate) / 100.0;

            // Account balance
            try {
                AccountRecord accRec =
                        new AccountRecord(da.getRecord("ACCOUNT", acctNo));
                if(accRec !=null){
                    try{
                        wbStr = accRec.getWorkingBalance().getValue();                    
                    }catch(Exception e){}
                }              
                if (!wbStr.isEmpty()) {
                    workingBalance = Double.parseDouble(wbStr);
                }
                else 
                    lockerAccRec.getAcctNo().setError("Unable to fetch account balance");
                    
            } catch (Exception e) {}

            // Final check
            if ((workingBalance - totalAmount) < minBalance) {
                lockerAccRec.getAcctNo().setError("Insufficient balance");
            }
        } catch (Exception e) {}

        return lockerAccRec.getValidationResponse();
    }
}