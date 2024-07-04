# Use displayXXX instead of XXX for Program (ANDROSDK-1871)

ALTER TABLE Program RENAME TO Program_Old;
CREATE TABLE Program (_id INTEGER PRIMARY KEY AUTOINCREMENT, uid TEXT NOT NULL UNIQUE, code TEXT, name TEXT, displayName TEXT, created TEXT, lastUpdated TEXT, shortName TEXT, displayShortName TEXT, description TEXT, displayDescription TEXT, version INTEGER, onlyEnrollOnce INTEGER, displayEnrollmentDateLabel TEXT, displayIncidentDate INTEGER, displayIncidentDateLabel TEXT, registration INTEGER, selectEnrollmentDatesInFuture INTEGER, dataEntryMethod INTEGER, ignoreOverdueEvents INTEGER, selectIncidentDatesInFuture INTEGER, useFirstStageDuringRegistration INTEGER, displayFrontPageList INTEGER, programType TEXT, relatedProgram TEXT, trackedEntityType TEXT, categoryCombo TEXT, accessDataWrite INTEGER, expiryDays INTEGER, completeEventsExpiryDays INTEGER, expiryPeriodType TEXT, minAttributesRequiredToSearch INTEGER, maxTeiCountToReturn INTEGER, featureType TEXT, accessLevel TEXT, color TEXT, icon TEXT, displayEnrollmentLabel TEXT, displayFollowUpLabel TEXT, displayOrgUnitLabel TEXT, displayRelationshipLabel TEXT, displayNoteLabel TEXT, displayTrackedEntityAttributeLabel TEXT, displayProgramStageLabel TEXT, displayEventLabel TEXT, FOREIGN KEY (trackedEntityType) REFERENCES TrackedEntityType (uid) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED, FOREIGN KEY (categoryCombo) REFERENCES CategoryCombo (uid) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED);
INSERT INTO Program (_id, uid, code, name, displayName, created, lastUpdated, shortName, displayShortName, description, displayDescription, version, onlyEnrollOnce, displayEnrollmentDateLabel, displayIncidentDate, displayIncidentDateLabel, registration, selectEnrollmentDatesInFuture, dataEntryMethod, ignoreOverdueEvents, selectIncidentDatesInFuture, useFirstStageDuringRegistration, displayFrontPageList, programType, relatedProgram, trackedEntityType, categoryCombo, accessDataWrite, expiryDays, completeEventsExpiryDays, expiryPeriodType, minAttributesRequiredToSearch, maxTeiCountToReturn, featureType, accessLevel, color, icon, displayEnrollmentLabel, displayFollowUpLabel, displayOrgUnitLabel, displayRelationshipLabel, displayNoteLabel, displayTrackedEntityAttributeLabel, displayProgramStageLabel, displayEventLabel) SELECT _id, uid, code, name, displayName, created, lastUpdated, shortName, displayShortName, description, displayDescription, version, onlyEnrollOnce, enrollmentDateLabel AS displayEnrollmentDateLabel, displayIncidentDate, incidentDateLabel AS displayIncidentDateLabel, registration, selectEnrollmentDatesInFuture, dataEntryMethod, ignoreOverdueEvents, selectIncidentDatesInFuture, useFirstStageDuringRegistration, displayFrontPageList, programType, relatedProgram, trackedEntityType, categoryCombo, accessDataWrite, expiryDays, completeEventsExpiryDays, expiryPeriodType, minAttributesRequiredToSearch, maxTeiCountToReturn, featureType, accessLevel, color, icon, enrollmentLabel AS displayEnrollmentLabel, followUpLabel AS displayFollowUpLabel, orgUnitLabel AS displayOrgUnitLabel,  relationshipLabel AS displayRelationshipLabel, noteLabel AS displayNoteLabel, trackedEntityAttributeLabel AS displayTrackedEntityAttributeLabel, programStageLabel AS displayProgramStageLabel, eventLabel AS displayEventLabel FROM Program_Old;
DROP TABLE IF EXISTS Program_Old;
