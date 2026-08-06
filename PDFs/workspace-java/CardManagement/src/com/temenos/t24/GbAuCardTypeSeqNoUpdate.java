package com.temenos.t24;

import java.util.List;

import com.temenos.api.TStructure;
import com.temenos.t24.api.complex.eb.servicehook.TransactionData;
import com.temenos.t24.api.complex.eb.templatehook.TransactionContext;
import com.temenos.t24.api.hook.system.RecordLifecycle;
import com.temenos.t24.api.records.cardtype.CardTypeRecord;
import com.temenos.t24.api.system.DataAccess;

/**
 * TODO: Document me!
 *
 * @author kawsar
 *
 */
public class GbAuCardTypeSeqNoUpdate extends RecordLifecycle{

    @Override
    public void postUpdateRequest(String application, String currentRecordId, TStructure currentRecord,
            List<TransactionData> transactionData, List<TStructure> currentRecords,
            TransactionContext transactionContext) {
        DataAccess da = new DataAccess(this);
        String cardType = currentRecordId.split("\\.")[0];
        CardTypeRecord typeRec = new CardTypeRecord(da.getRecord("CARD.TYPE", cardType));
        int seqNo = Integer.parseInt(typeRec.getLocalRefField("LT.SEQ.NO").getValue());
        seqNo++;
        typeRec.getLocalRefField("LT.SEQ.NO").setValue(String.format("%06d", seqNo));
        
        
    }

}
