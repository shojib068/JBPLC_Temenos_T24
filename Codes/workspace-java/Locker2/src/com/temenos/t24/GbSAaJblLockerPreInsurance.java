package com.temenos.t24;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.temenos.api.TStructure;
import com.temenos.t24.api.complex.eb.servicehook.ServiceData;
import com.temenos.t24.api.complex.eb.servicehook.SynchronousTransactionData;
import com.temenos.t24.api.complex.eb.servicehook.TransactionControl;
import com.temenos.t24.api.hook.system.ServiceLifecycle;
import com.temenos.t24.api.records.aajbllockeracct.AaJblLockerAcctRecord;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.system.Session;

/**
 * TODO: Document me!
 *
 * @author kawsar
 *
 */
public class GbSAaJblLockerPreInsurance extends ServiceLifecycle{

    @Override
    public List<String> getIds(ServiceData serviceData, List<String> controlList) {

        DataAccess da = new DataAccess(this);
        Session ss = new Session(this);

        String coCode = ss.getCompanyId();

        List<String> lockerAcctIds = new ArrayList<>();

        try {
            lockerAcctIds = da.selectRecords(
                    "BNK",
                    "AA.JBL.LOCKER.ACCT",
                    "",
                    "WITH CO.CODE EQ " + coCode
            );
        } catch (Exception e) {
            e.printStackTrace();
        }

        return lockerAcctIds;
    }

    @Override
    public void updateRecord(String id, ServiceData serviceData, String controlItem,
            TransactionControl transactionControl, List<SynchronousTransactionData> transactionData,
            List<TStructure> records) {
        DataAccess da = new DataAccess(this);
        AaJblLockerAcctRecord lockerAcctRec = null;
        SynchronousTransactionData txn = null;
        Session ss = new Session(this);
        int today = LocalDate.now().getYear();
        String todayYearStr = String.valueOf(today); 
        String coCode = ss.getCompanyId();
        
        try {

            lockerAcctRec = new AaJblLockerAcctRecord(
                    da.getRecord("AA.JBL.LOCKER.ACCT", id));

            // Step 1 : Reset Insurance
            lockerAcctRec.setInsurance("NO");

            // Step 2 : Check if this locker has a Due RENT charge
            String selection = "WITH STATUS EQ Due AND @ID LIKE ...-RENT-" + todayYearStr;

            List<String> chargeRecIds = da.selectRecords(
                    "BNK",
                    "AA.JBL.LOCKER.CHARGE",
                    "",
                    selection);

            boolean dueRent = false;

            if (chargeRecIds != null) {
                for (String chargeId : chargeRecIds) {

                    String[] parts = chargeId.split("-");

                    if (parts.length > 0 && id.equals(parts[0])) {
                        dueRent = true;
                        break;
                    }
                }
            }
            // Step 3 : Set due year
            if (dueRent) {
                lockerAcctRec.setLastDueYear(todayYearStr);
            }
            // OFS Transaction
            try{
                txn = new SynchronousTransactionData();

                txn.setFunction("INPUT");
                txn.setUserName("INPUTT");
                txn.setNumberOfAuthoriser("0");
                txn.setCompanyId(coCode);
                txn.setTransactionId(id);
                txn.setSourceId("LOCKER.OFS");
                txn.setVersionId("AA.JBL.LOCKER.ACCT,OFS");

                transactionData.add(txn);
                records.add(lockerAcctRec.toStructure());
            }catch(Exception e){}
        } catch (Exception e) {
            e.printStackTrace();
        }     
    }
}
