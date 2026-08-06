package com.temenos.t24;

import com.temenos.t24.api.complex.eb.templatehook.TransactionContext;
import com.temenos.t24.api.hook.system.RecordLifecycle;
import com.temenos.t24.api.records.cardissue.CardIssueRecord;
import com.temenos.t24.api.records.cardtype.CardTypeRecord;
import com.temenos.t24.api.system.DataAccess;


/**
 * TODO: Document me!
 *
 * @author kawsar
 *
 */
public class GbIdCardIssueNew extends RecordLifecycle{

    @Override
    public String checkId(String currentRecordId, TransactionContext transactionContext) {
        DataAccess da = new DataAccess(this);
        String SeqNoStr = null;
        
        String cardType = currentRecordId;
        CardIssueRecord issueRec = new CardIssueRecord();
        CardTypeRecord typeRec = new CardTypeRecord(da.getRecord("CARD.TYPE", cardType));
        
        SeqNoStr = typeRec.getLocalRefField("LT.SEQ.NO").getValue();
        int seqNo = Integer.parseInt(SeqNoStr);
        
        String newSeq = String.format("%06d", seqNo);
        String newId = cardType + "." + newSeq;
        seqNo++;
        typeRec.getLocalRefField("LT.SEQ.NO").setValue(String.format("%06d", seqNo));
        
        
        return newId;
        
        
        
        

    }
    
    

}
