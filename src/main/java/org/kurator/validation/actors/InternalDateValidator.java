package org.kurator.validation.actors;

/*
 * This code is an adaptation of akka.fp.InternalDateValidator
 * in the FP-Akka package as of 29Oct2014.
 */

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.TreeSet;

import org.filteredpush.kuration.services.InternalDateValidationService;
import org.filteredpush.kuration.util.CurationComment;
import org.filteredpush.kuration.util.CurationCommentType;
import org.filteredpush.kuration.util.CurationException;
import org.filteredpush.kuration.util.CurationStatus;
import org.filteredpush.kuration.util.SpecimenRecord;
import org.filteredpush.kuration.util.SpecimenRecordTypeConf;
import org.kurator.akka.AkkaActor;

public class InternalDateValidator extends AkkaActor {

    public String singleServiceClassQN = "org.filteredpush.kuration.services.InternalDateValidationService";

    private String collectorLabel;
    private String yearCollectedLabel;
    private String monthCollectedLabel;
    private String dayCollectedLabel;
    private String eventDateLabel;
    private String startDayOfYearLabel;
    private String verbatimEventDateLabel;
    private String modifiedLabel;

    InternalDateValidationService singleDateValidationService = null;
    private LinkedList<SpecimenRecord> inputObjList = new LinkedList<SpecimenRecord>();
    private LinkedHashMap<String, TreeSet<SpecimenRecord>> inputDataMap = new LinkedHashMap<String, TreeSet<SpecimenRecord>>();

    @Override
    public void onInitialize() {

        try {
            // initialize required label
            SpecimenRecordTypeConf speicmenRecordTypeConf = SpecimenRecordTypeConf.getInstance();

            collectorLabel = speicmenRecordTypeConf.getLabel("RecordedBy");
            if (collectorLabel == null) {
                throw new CurationException(
                        getName() + " failed since the RecordedBy label of the SpecimenRecordType is not set.");
            }

            yearCollectedLabel = speicmenRecordTypeConf
                    .getLabel("YearCollected");
            if (yearCollectedLabel == null) {
                throw new CurationException(
                        getName() + " failed since the YearCollected label of the SpecimenRecordType is not set.");
            }

            monthCollectedLabel = speicmenRecordTypeConf
                    .getLabel("MonthCollected");
            if (monthCollectedLabel == null) {
                throw new CurationException(
                        getName() + " failed since the MonthCollected label of the SpecimenRecordType is not set.");
            }

            dayCollectedLabel = speicmenRecordTypeConf.getLabel("DayCollected");
            if (dayCollectedLabel == null) {
                throw new CurationException(
                        getName() + " failed since the DayCollected label of the SpecimenRecordType is not set.");
            }

            eventDateLabel = speicmenRecordTypeConf.getLabel("EventDate");
            if (eventDateLabel == null) {
                throw new CurationException(
                        getName() + " failed since the eventDate label of the SpecimenRecordType is not set.");
            }

            startDayOfYearLabel = speicmenRecordTypeConf
                    .getLabel("StartDayOfYear");
            if (startDayOfYearLabel == null) {
                throw new CurationException(
                        getName() + " failed since the startDayOfYearLabel label of the SpecimenRecordType is not set.");
            }

            verbatimEventDateLabel = speicmenRecordTypeConf
                    .getLabel("VerbatimEventDate");
            if (verbatimEventDateLabel == null) {
                throw new CurationException(
                        getName() + " failed since the verbatimEventDate label of the SpecimenRecordType is not set.");
            }

            modifiedLabel = speicmenRecordTypeConf.getLabel("Modified");
            if (modifiedLabel == null) {
                throw new CurationException(
                        getName() + " failed since the modified label of the SpecimenRecordType is not set.");
            }

            singleDateValidationService = (InternalDateValidationService) Class.forName(singleServiceClassQN).newInstance();

        } catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
        }

        inputObjList.clear();
        inputDataMap.clear();
    }

    public String getName() {
        return "CollectionEventOutlierFinder";
    }

    @Override
    public void onData(Object value) throws Exception {

        if (value instanceof SpecimenRecord) {

            SpecimenRecord inputSpecimenRecord = (SpecimenRecord) value;

            String eventDate = inputSpecimenRecord.get(eventDateLabel);
            String collector = inputSpecimenRecord.get(collectorLabel);
            String year = inputSpecimenRecord.get(yearCollectedLabel);
            String month = inputSpecimenRecord.get(monthCollectedLabel);
            String day = inputSpecimenRecord.get(dayCollectedLabel);
            String startDayOfYear = inputSpecimenRecord.get(startDayOfYearLabel);
            String verbatimEventDate = inputSpecimenRecord.get(verbatimEventDateLabel);
            String modified = inputSpecimenRecord.get(modifiedLabel);

            singleDateValidationService.validateDate(eventDate,
                    verbatimEventDate, startDayOfYear, year, month, day,
                    modified, collector);

            CurationCommentType curationComment = null;
            CurationStatus curationStatus = singleDateValidationService
                    .getCurationStatus();

            if (curationStatus == CurationComment.CURATED || curationStatus == CurationComment.FILLED_IN) {
                inputSpecimenRecord.put("eventDate",
                        String.valueOf(singleDateValidationService
                                .getCorrectedDate()));
            }

            curationComment = CurationComment.construct(
                    curationStatus,
                    singleDateValidationService.getComment(),
                    singleDateValidationService.getServiceName());
            updateAndSendRecord(inputSpecimenRecord, curationComment);
        }
    }

    private void updateAndSendRecord(SpecimenRecord result,
            CurationCommentType comment) {

        if (comment != null) {
            result.put("dateComment", comment.getDetails());
            result.put("dateStatus", comment.getStatus());
            result.put("dateSource", comment.getSource());
        } else {
            result.put("dateComment", "None");
            result.put("dateStatus", CurationComment.CORRECT.toString());
            result.put("dateSource", "None");
        }

        broadcast(result);
    }

}
