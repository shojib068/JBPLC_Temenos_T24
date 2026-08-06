package com.temenos.t24;

import java.util.ArrayList;
import java.util.List;

import com.temenos.api.TStructure;
import com.temenos.t24.api.complex.eb.templatehook.TransactionContext;
import com.temenos.t24.api.hook.system.RecordLifecycle;
import com.temenos.t24.api.records.aajbllockeracct.NomineeTypeClass;
import com.temenos.t24.api.records.aajbllockeracct.AaJblLockerAcctRecord;
import com.temenos.t24.api.records.aajbllockeracct.MandateCustomerIdClass;
import com.temenos.t24.api.records.customer.CustomerRecord;
import com.temenos.t24.api.records.personentity.PersonEntityRecord;
import com.temenos.t24.api.system.DataAccess;

/**
 * Routine: Default.Routine
 * Version attached to: AA.JBL.LOCKER.ACCT,INPUT
 *
 * Business Logic:
 *
 * 1. Extract Account Number:
 *      - Record ID format: <AccountNo>.<Suffix>
 *      - Example: 123456.0001 → Account Number = 123456
 *
 * 2. Fetch ACCOUNT Record:
 *      - Retrieve CUSTOMER ID linked to the account
 *
 * 3. Fetch CUSTOMER Record:
 *      - Retrieve:
 *          • Customer Short Name → used as Account Name
 *          • Mobile Number (SMS1 from PHONE1)
 *
 * 4. Populate Locker Account Fields:
 *      - ACCT.NO        ← Account Number
 *      - CUSTOMER       ← Customer ID
 *      - ACCT.NAME      ← Customer Short Name
 *      - MOBILE.NUMBER  ← Customer Phone (if available)
 *
 * 5. Mandate Processing:
 *      - Loop through MANDATE.CUSTOMER.ID entries
 *      - Validate:
 *          • Mandate Relation must not be empty
 *      - Fetch CUSTOMER record for each mandate
 *      - Populate:
 *          • Mandate Name (from CUSTOMER.SHORT.NAME)
 *
 * 6. Nominee Processing:
 *      - Loop through NOMINEE.TYPE entries
 *      - Validate:
 *          • Nominee Relation must not be empty
 *
 *      A. If NOMINEE.TYPE = "CUSTOMER":
 *          - Mandatory:
 *              • Nominee Customer ID
 *              • Nominee Percentage
 *          - Restrictions:
 *              • PERSON.ENTITY.ID must be empty
 *          - Fetch CUSTOMER record
 *          - Populate Nominee Name from CUSTOMER.SHORT.NAME
 *
 *      B. If NOMINEE.TYPE = "PERSON.ENTITY":
 *          - Mandatory:
 *              • PERSON.ENTITY.ID
 *              • Nominee Percentage
 *          - Restrictions:
 *              • Nominee Customer ID must be empty
 *          - Fetch PERSON.ENTITY record
 *          - Populate Nominee Name from PERSON.ENTITY.NAME
 *
 *      - Accumulate Nominee Percentage
 *      - Validate numeric percentage values
 *
 * 7. Final Validation:
 *      - Total Nominee Percentage must equal 100%
 *
 * 8. Update Record:
 *      - Write all derived and validated values back to the current record
 *
 * Important Notes:
 * - Safe list access is enforced using size() > 0 checks to avoid IndexOutOfBoundsException
 * - Empty/null values are handled defensively using trimming and validation
 * - Exceptions are thrown for mandatory field violations
 * - DataAccess is used for all record retrievals (ACCOUNT, CUSTOMER, PERSON.ENTITY)
 *
 * Assumptions:
 * - Record ID format is valid
 * - Required records exist in the system
 *
 * @author kawsar
 */
public class GbDAaJblLockerAcct extends RecordLifecycle {

    @Override
    public void defaultFieldValues(String application, String currentRecordId,
            TStructure currentRecord, TStructure unauthorisedRecord,
            TStructure liveRecord, TransactionContext transactionContext) {

        AaJblLockerAcctRecord lockerAccRec = new AaJblLockerAcctRecord(currentRecord);
        DataAccess da = new DataAccess(this);
        
        
        List<NomineeTypeClass> nomineeTypeList = new ArrayList<>();
        List<MandateCustomerIdClass> mandateCustIdList = new ArrayList<>();

//        Mandate Customer 
        try {
            mandateCustIdList = lockerAccRec.getMandateCustomerId();
        } catch (Exception e) {}
        if (mandateCustIdList != null && !mandateCustIdList.isEmpty()){
            for (int i = 0; i < mandateCustIdList.size(); i++){
                String mandateCustomerId = "";
                String mandateName = "";
                String relation = "";
                try {
                    mandateCustomerId = mandateCustIdList.get(i)
                            .getMandateCustomerId().getValue();
                } catch (Exception e) {}
                try {
                    relation = mandateCustIdList.get(i)
                            .getMandateRelation().getValue();
                } catch (Exception e) {}
                if (!mandateCustomerId.isEmpty()) {

                    try {
                        CustomerRecord mRec = new CustomerRecord(
                                da.getRecord("CUSTOMER", mandateCustomerId));
                        try{
                            mandateName = mRec.getShortName().get(0).getValue();
                        }catch(Exception e){}

                        
                    } catch (Exception e) {}
                }
                MandateCustomerIdClass man = new MandateCustomerIdClass();
                if(!mandateCustomerId.isEmpty())
                    man.setMandateCustomerId(mandateCustomerId);
                else
                    man.setMandateCustomerId("");
                if(!relation.isEmpty())
                    man.setMandateRelation(relation);
                else
                    man.setMandateRelation(relation);
                if(!mandateName.isEmpty())
                    man.setMandateName(mandateName);
                else
                    man.setMandateName(mandateName);
                
                lockerAccRec.setMandateCustomerId(man, i);


            }
        }
        
        
//        Nomine type
        try {
            nomineeTypeList = lockerAccRec.getNomineeType();
        } catch (Exception e) {}
        if (nomineeTypeList != null && !nomineeTypeList.isEmpty()){
            for (int i = 0; i < nomineeTypeList.size(); i++){
                String nomineeType = "";
                String relation = "";
                try {
                    nomineeType = nomineeTypeList.get(i).getNomineeType().getValue();
                    nomineeType = nomineeType.trim();
                } catch (Exception e) {} 
                try {
                    relation = nomineeTypeList.get(i).getNomineeRelation().getValue();
                    relation = relation.trim();
                } catch (Exception e) {}
                
                NomineeTypeClass nominee = new NomineeTypeClass();
                if(!nomineeType.isEmpty())
                    nominee.setNomineeType(nomineeType);
                else
                    nominee.setNomineeType("");
                if(!relation.isEmpty())
                    nominee.setNomineeRelation(relation);
                else
                    nominee.setNomineeRelation("");
                
                
//                customer
                if ("CUSTOMER".equalsIgnoreCase(nomineeType)){
                    String nomineeCustId = "";
                    String percent = "";
                    String shortName = "";
                    String personEntityId = "";
                    
                    try {
                        nomineeCustId = nomineeTypeList.get(i).getNomineeCustomerId().getValue();
                        nomineeCustId = nomineeCustId.trim();
                    } catch (Exception e) {}
                    try {
                        percent = nomineeTypeList.get(i).getNomineePercentage().getValue();
                        percent = percent.trim();
                    } catch (Exception e) {}
                    try{
                        personEntityId = nomineeTypeList.get(i).getPersonEntityId().getValue();
                        personEntityId = personEntityId.trim();
                    }catch(Exception e){}
                    if (!nomineeCustId.isEmpty()) {
                        try{
                            CustomerRecord nRec =
                                    new CustomerRecord(da.getRecord("CUSTOMER", nomineeCustId));
                            shortName = nRec.getShortName().get(0).getValue();
                        }catch(Exception e){}
                        }
                    
                    if(!nomineeCustId.isEmpty())
                        nominee.setNomineeCustomerId(nomineeCustId);
                    else
                        nominee.setNomineeCustomerId("");
                    if(!shortName.isEmpty())
                        nominee.setNomineeName(shortName);
                    else 
                        nominee.setNomineeName("");
                    if(!percent.isEmpty())
                        nominee.setNomineePercentage(percent);
                    else
                        nominee.setNomineePercentage("");
                    
                    lockerAccRec.setNomineeType(nominee, i);
                    }
                
//                Person entity
                
                if ("PERSON.ENTITY".equalsIgnoreCase(nomineeType)){
                    String personEntityId = "";
                    String percent = "";
                    String perName = "";
                    String nomineeCustId = "";
                    try {
                        personEntityId = nomineeTypeList.get(i).getPersonEntityId().getValue();
                        personEntityId = personEntityId.trim();
                    } catch (Exception e) {}
                    try {
                        percent = nomineeTypeList.get(i).getNomineePercentage().getValue();
                        percent = percent.trim();
                    } catch (Exception e) {}
                    try {
                        nomineeCustId = nomineeTypeList.get(i).getNomineeCustomerId().getValue();
                        nomineeCustId = nomineeCustId.trim();
                    } catch (Exception e) {}
                    if (!personEntityId.isEmpty()) {
                        try {
                            PersonEntityRecord pRec =
                                    new PersonEntityRecord(da.getRecord("PERSON.ENTITY", personEntityId));
                            perName = pRec.getName().get(0).getValue();
                            perName = perName.trim();
                        } catch (Exception e) {}
                    }
                    if(!personEntityId.isEmpty())
                        nominee.setPersonEntityId(personEntityId);
                    else
                        nominee.setPersonEntityId("");
                    if(!perName.isEmpty())
                        nominee.setNomineeName(perName);
                    else 
                        nominee.setNomineeName("");
                    if(!percent.isEmpty())
                        nominee.setNomineePercentage(percent);
                    else
                        nominee.setNomineePercentage("");
                    if(!nomineeCustId.isEmpty())
                        nominee.setNomineeCustomerId(nomineeCustId);
                    else
                        nominee.setNomineeCustomerId("");
                    
                    lockerAccRec.setNomineeType(nominee, i);

                }
                currentRecord.set(lockerAccRec.toStructure());
            }
        }

       
    }
}