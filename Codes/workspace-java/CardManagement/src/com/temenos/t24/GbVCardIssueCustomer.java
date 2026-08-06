package com.temenos.t24;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.temenos.api.LocalRefList;
import com.temenos.api.TStructure;
import com.temenos.api.TValidationResponse;
import com.temenos.t24.api.complex.eb.templatehook.TransactionContext;
import com.temenos.t24.api.hook.system.RecordLifecycle;

import com.temenos.t24.api.records.account.AccountRecord;
import com.temenos.t24.api.records.cardissue.CardIssueRecord;
import com.temenos.t24.api.records.customer.CustomerRecord;
import com.temenos.t24.api.system.DataAccess;

/**
 * TODO: Document me!
 *
 * @author kawsar
 *
 */
public class GbVCardIssueCustomer extends RecordLifecycle {

    @Override
    public TValidationResponse validateRecord(
            String application,
            String currentRecordId,
            TStructure currentRecord,
            TStructure unauthorisedRecord,
            TStructure liveRecord,
            TransactionContext transactionContext) {

        DataAccess da = new DataAccess(this);

        /* ================= BUILD ISSUE RECORD ================= */
        CardIssueRecord issueRec = new CardIssueRecord(currentRecord);

        /* ================= CUSTOMER ID ================= */
        String customerId = issueRec.getCustomerId().getValue();
        if (isEmpty(customerId)) {
            issueRec.getCustomerId().setError("Customer ID Missing");
            return issueRec.getValidationResponse();
        }

        /* ================= ACCOUNT VALIDATION ================= */
        for (int i = 0; i < issueRec.getAccount().size(); i++) {
            String accountNo = issueRec.getAccount(i).getValue();
            if (isEmpty(accountNo)) continue;

            AccountRecord accRec = new AccountRecord(da.getRecord("ACCOUNT", accountNo));
            if (!accRec.getPostingRestrict().isEmpty()) {
                issueRec.getAccount(i)
                        .setError("Account " + accountNo + " is Posting Restricted");
                return issueRec.getValidationResponse();
            }
        }

        /* ================= CUSTOMER RECORD ================= */
        CustomerRecord cusRec = new CustomerRecord(da.getRecord("CUSTOMER", customerId));

        /* ================= FIELD EXTRACTION ================= */
        String title = cusRec.getTitle().getValue();
        String gender = cusRec.getGender().getValue();
        String ltFatherName = cusRec.getLocalRefField("LT.FATHER.NAME").getValue();
        String ltMotherName = cusRec.getLocalRefField("LT.MOTHER.NAME").getValue();
        String dateodBirth = cusRec.getDateOfBirth().getValue();

        // MV fields: LT.VILLAGE.AREA, LT.PHNE.MOBILE, LT.EMAIL
        List<String> ltVillageArea = new ArrayList<>();
        List<String> phoneMobile = new ArrayList<>();
        List<String> ltEmail = new ArrayList<>();

        LocalRefList ltAddrType = cusRec.getLocalRefGroups("LT.ADDR.TYPE");
        if (ltAddrType != null) {
            for (int i = 0; i < ltAddrType.size(); i++) {
                String village = ltAddrType.get(i).getLocalRefField("LT.VILLAGE.AREA").getValue();
                if (village != null && !village.trim().isEmpty()) ltVillageArea.add(village.trim());

                String phone = ltAddrType.get(i).getLocalRefField("LT.PHNE.MOBILE").getValue();
                if (phone != null && !phone.trim().isEmpty()) phoneMobile.add(phone.trim());

                String email = ltAddrType.get(i).getLocalRefField("LT.EMAIL").getValue();
                if (email != null && !email.trim().isEmpty()) ltEmail.add(email.trim());
            }
        }

        /* ================= PRESENT ADDRESS ================= */
        String presentDivision = cusRec.getLocalRefField("LT.DIVISION.PRS").getValue();
        String presentDistrict = cusRec.getLocalRefField("LT.DISTRICT.PRS").getValue();
        String presentUpazila = cusRec.getLocalRefField("LT.UPAZILA.PRS").getValue();
        String presentThana = cusRec.getLocalRefField("LT.THANA.PRS").getValue();
        String presentPostOffice = cusRec.getLocalRefField("LT.POSTOFF.PRS").getValue();

        /* ================= COMMUNICATION ADDRESS ================= */
        String commDivision = cusRec.getLocalRefField("LT.DIVISION.COM").getValue();
        String commDistrict = cusRec.getLocalRefField("LT.DISTRICT.COM").getValue();
        String commUpazila = cusRec.getLocalRefField("LT.UPAZILA.COM").getValue();
        String commThana = cusRec.getLocalRefField("LT.THANA.COM").getValue();
        String commPostOffice = cusRec.getLocalRefField("LT.POSTOFF.COM").getValue();


        /* ================= CUSTOMER FIELD VALIDATION ================= */
        if (isEmpty(title)) return error(issueRec, "Title Missing");
        if (isEmpty(gender)) return error(issueRec, "Gender Missing");
        if (isEmpty(ltFatherName)) return error(issueRec, "Father Name Missing");
        if (isEmpty(ltMotherName)) return error(issueRec, "Mother Name Missing");
        if (isEmpty(dateodBirth)) return error(issueRec, "Date of Birth Missing");

        if (isEmpty(ltVillageArea)) return error(issueRec, "Village Area Missing");
        if (isEmpty(phoneMobile)) return error(issueRec, "Mobile Number Missing");
        if (isEmpty(ltEmail)) return error(issueRec, "Email Missing");

        if (isEmpty(presentDivision)) return error(issueRec, "Present Division Missing");
        if (isEmpty(presentDistrict)) return error(issueRec, "Present District Missing");
        if (isEmpty(presentUpazila)) return error(issueRec, "Present Upazila Missing");
        if (isEmpty(presentThana)) return error(issueRec, "Present Thana Missing");
        if (isEmpty(presentPostOffice)) return error(issueRec, "Present Post Office Missing");

        if (isEmpty(commDivision)) return error(issueRec, "Communication Division Missing");
        if (isEmpty(commDistrict)) return error(issueRec, "Communication District Missing");
        if (isEmpty(commUpazila)) return error(issueRec, "Communication Upazila Missing");
        if (isEmpty(commThana)) return error(issueRec, "Communication Thana Missing");
        if (isEmpty(commPostOffice)) return error(issueRec, "Communication Post Office Missing");


        /* ================= EXISTING CARD VALIDATION ================= */

        List<String> liveCardIds = da.selectRecords("BNK", "CARD.ISSUE", "", "WITH CUSTOMER.ID EQ " + customerId);
        List<String> nauCardIds = da.selectRecords("BNK", "CARD.ISSUE$NAU", "", "WITH CUSTOMER.ID EQ " + customerId);

        List<String> allCardIds = new ArrayList<>();
        allCardIds.addAll(liveCardIds);
        allCardIds.addAll(nauCardIds);

        for (String cardId : allCardIds) {
            String fileName = liveCardIds.contains(cardId) ? "CARD.ISSUE" : "CARD.ISSUE$NAU";
            CardIssueRecord cardRec = new CardIssueRecord(da.getRecord(fileName, cardId));
            String cardStatus = cardRec.getCardStatus().getValue();

            if (!isEmpty(cardStatus)) {
                issueRec.getCustomerId().setError("Customer already has an issued card");
                return issueRec.getValidationResponse();
            }
        }

        return null; // ✅ All validation passed
    }

    /* ================= HELPERS ================= */
    private boolean isEmpty(String v) {
        return v == null || v.trim().isEmpty();
    }

    private boolean isEmpty(List<String> list) {
        if (list == null || list.isEmpty()) return true;
        for (String v : list) {
            if (!isEmpty(v)) return false;
        }
        return true;
    }

    private TValidationResponse error(CardIssueRecord rec, String msg) {
        rec.getCustomerId().setError(msg);
        return rec.getValidationResponse();
    }
}

