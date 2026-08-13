package com.temenos.t24;

import java.util.ArrayList;
import java.util.List;

import com.temenos.api.exceptions.T24CoreException;
import com.temenos.t24.api.complex.eb.templatehook.TransactionContext;
import com.temenos.t24.api.hook.system.RecordLifecycle;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.system.Session;

/**
 * TODO: Document me!
 *
 * @author kawsar
 *
 */
public class GbICrReq extends RecordLifecycle{

    @Override
    public String checkId(String currentRecordId, TransactionContext transactionContext) {
        DataAccess da = new DataAccess(this);
        Session ss = new Session(this);
        String coCode = ss.getCompanyId();
        String stmt = " WITH CO.CODE EQ "+coCode+" AND STATUS EQ 'REQUEST'";
        List<String> cfRecId = new ArrayList<>();
        cfRecId = da.selectRecords("", "EB.JBL.CASH.FEEDING$NAU", "", stmt);
        if(!cfRecId.isEmpty() || cfRecId!=null){
            throw new T24CoreException("EB.ERROR", "GB-CR-REQUEST");
        }
        return currentRecordId;
    }

}
