package com.temenos.t24; 
import java.util.List;
import com.temenos.api.TStructure;
import com.temenos.api.exceptions.T24IOException;
import com.temenos.t24.api.complex.eb.servicehook.SynchronousTransactionData;
import com.temenos.t24.api.complex.eb.servicehook.ServiceData;
import com.temenos.t24.api.complex.eb.servicehook.TransactionControl;
import com.temenos.t24.api.hook.system.ServiceLifecycle;
import com.temenos.t24.api.records.account.AccountRecord;
import com.temenos.t24.api.records.ebbdschedulechargeinfo.EbBdScheduleChargeInfoRecord;
import com.temenos.t24.api.records.fundstransfer.FundsTransferRecord;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.system.Date;
import com.temenos.t24.api.tables.ebbdschedulechargeinfo.EbBdScheduleChargeInfoTable;
 
public class ServiceRoutineEg extends ServiceLifecycle {
 
    @Override
    public List<String> getIds(ServiceData serviceData, List<String> controlList) {
        List<String> recIds = null;
        DataAccess da = new DataAccess(this);
        recIds = da.selectRecords("BNK", "EB.BD.SCHEDULE.CHARGE.INFO", "", "");
        System.out.println("Total Records: " + recIds.size());
        return recIds;
    }
 
    @Override
    public void updateRecord(String id, ServiceData serviceData, String controlItem,
            TransactionControl transactionControl, List<SynchronousTransactionData> transactionData,
            List<TStructure> records) {
 
        DataAccess da = new DataAccess(this);
        Date dt = new Date(this);
 
        EbBdScheduleChargeInfoRecord chargeRec = null;
        String today = dt.getDates().getToday().getValue();
 
        try {
            String scheduleInfoId = id;
            String accountId = id.split("-")[0];
 
            AccountRecord accRec = new AccountRecord(da.getRecord("ACCOUNT", accountId));
            double workingBalance = 0.0;
            try {
                workingBalance = Double.parseDouble(accRec.getWorkingBalance().getValue());
            } catch (Exception e) {
                workingBalance = 0.0;
                logger.error("Error fetching account " + accountId + ": " + e.getMessage());
                return;
            }
            boolean alreadyFull = false;
            double totalDueAmount = 0.0;
            try {
                chargeRec = new EbBdScheduleChargeInfoRecord(
                        da.getRecord("EB.BD.SCHEDULE.CHARGE.INFO", scheduleInfoId));
 
                try {
                    totalDueAmount = Double.parseDouble(chargeRec.getTotalDueAmt().getValue());
                } catch (Exception e) {
                    totalDueAmount = 0.0;
                }
            } catch (Exception e) {
                // chargeRec = new EbBdScheduleChargeInfoRecord();
                logger.error("Charge info record not found for ID: " + scheduleInfoId);
                return;
 
            }
            String status = chargeRec.getStatus().getValue();
 
            if (status != null && status.equalsIgnoreCase("FULL")) {
                alreadyFull = true;
            }
 
            if (alreadyFull) {
                System.out.println("Already FULL → Skipping: " + scheduleInfoId);
                return;
            }
            // if (totalDueAmount > 0.0) {
            if (totalDueAmount <= 0.0) {
                return;
            }
 
            double paidAmount = 0.0;
            if (workingBalance > 0.0) {
                if (workingBalance >= totalDueAmount) {
 
                    paidAmount = totalDueAmount;
                    totalDueAmount = 0.0;
                } else if (workingBalance < totalDueAmount) {
                    paidAmount = workingBalance;
                    totalDueAmount = totalDueAmount - workingBalance;
                }
 
            }
 
            String company = chargeRec.getCoCode().toString();
            if (paidAmount > 0.0) {
                try {
                    FundsTransferRecord ftRecord = null;
                    ftRecord = new FundsTransferRecord();
 
                    ftRecord.setDebitAcctNo(accountId);
                    ftRecord.setCreditAcctNo("BDT1280000019999");
                    ftRecord.setOrderingBank("JBL", 0);
                    ftRecord.setCreditCurrency(accRec.getCurrency().getValue());
                    ftRecord.setDebitAmount(String.valueOf(paidAmount));
                    ftRecord.setDebitCurrency(accRec.getCurrency().getValue());
                    ftRecord.setDebitValueDate(today);
                    ftRecord.setCreditValueDate(today);
                    ftRecord.setTransactionType("AC");
                    SynchronousTransactionData ftData = new SynchronousTransactionData();
                    ftData.setVersionId("FUNDS.TRANSFER");
                    ftData.setSourceId("BULK.OFS");
                    ftData.setFunction("INPUT");
                    ftData.setCompanyId(company);
                    // ftData.setTransactionId(accountId);
                    transactionData.add(ftData);
 
                    records.add(ftRecord.toStructure());
 
                } catch (Exception e) {
 
                    logger.error("Error creating Funds Transfer for account " + accountId + ": " + e.getMessage());
                }
 
                try {
                    chargeRec.setTotalDueAmt(String.valueOf(totalDueAmount));
                    updateChargeInfo(scheduleInfoId, chargeRec);
                } catch (Exception e) {
                    logger.error("Error updating charge record " + scheduleInfoId + ": " + e.getMessage());
                }
             
            }
 
        } catch (Exception e) {
            logger.error("Error updating charge record "  + ": " + e.getMessage());
        }
    }
 
    public void updateChargeInfo(String recordId, EbBdScheduleChargeInfoRecord record) {
        try {
            EbBdScheduleChargeInfoTable table = new EbBdScheduleChargeInfoTable(this);
 
            table.write(recordId, record);
 
        } catch (T24IOException e) {
            logger.error("Error writing violation record: " + e.getMessage());
        }
    }
 
}