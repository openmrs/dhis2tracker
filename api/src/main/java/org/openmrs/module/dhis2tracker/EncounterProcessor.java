/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.dhis2tracker;

import static org.openmrs.module.dhis2tracker.Dhis2TrackerConstants.CIEL_CODE_NEW_HIV_CASE;
import static org.openmrs.module.dhis2tracker.Dhis2TrackerConstants.CODE_SYSTEM_CIEL;
import static org.openmrs.module.dhis2tracker.Dhis2TrackerConstants.PERSON_ATTRIBUTE_TYPE_UUID;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Concept;
import org.openmrs.ConceptMap;
import org.openmrs.ConceptReferenceTerm;
import org.openmrs.Encounter;
import org.openmrs.Obs;
import org.openmrs.Patient;
import org.openmrs.PersonAttribute;
import org.openmrs.PersonAttributeType;
import org.openmrs.api.APIException;
import org.openmrs.api.PersonService;
import org.openmrs.api.context.Context;

public class EncounterProcessor {
	
	protected static final Log log = LogFactory.getLog(EncounterProcessor.class);
	
	private Dhis2HttpClient dhis2HttpClient;
	
	private EncounterProcessor() {
	}
	
	public static EncounterProcessor newInstance() {
		return new EncounterProcessor();
	}
	
	/**
	 * Processes the specified encounter
	 *
	 * @param encounter the encounter to process
	 * @return true if the encounter was processed successfully otherwise false
	 */
	public boolean process(Encounter encounter) {
		log.debug("Processing encounter");
		if (!hasNewHivCaseEvent(encounter)) {
			log.debug("Ignoring case report encounter with no new HIV case");
			return false;
		}
		
		PersonService ps = Context.getPersonService();
		PersonAttributeType uidAttributeType = ps.getPersonAttributeTypeByUuid(PERSON_ATTRIBUTE_TYPE_UUID);
		if (uidAttributeType == null) {
			throw new APIException("Cannot find person attribute type for dhis2 uid");
		}
		
		if (dhis2HttpClient == null) {
			dhis2HttpClient = Dhis2HttpClient.newInstance();
		}
		
		Patient patient = encounter.getPatient();
		PersonAttribute pAttrib = patient.getAttribute(uidAttributeType);
		if (pAttrib == null) {
			log.debug("Registering and enrolling the patient with id " + patient.getId() + " in DHIS2");
			//Register and enroll the patient in the program
			try {
				String data = Dhis2Utils.buildRegisterAndEnrollContent(encounter);
				String patientUid = dhis2HttpClient.registerAndEnroll(data);
				patient.addAttribute(new PersonAttribute(uidAttributeType, patientUid));
				ps.savePerson(patient);
			}
			catch (IOException e) {
				log.error("Failed to register and enroll the patient with id " + patient.getId() + " in DHIS2");
				return false;
			}
		} else {
			log.debug("Patient with id " + patient.getId() + " is already registered and enrolled in DHIS2");
		}
		
		return true;
		
		//TODO look up trigger concept GP
		/*List<TriggerEvent> events = new ArrayList<>();
		for (Obs obs : encounter.getObs()) {
			if (isTriggerObs(obs)) {
				events.add(new TriggerEvent(obs));
			}
		}
		
		log.debug("Sending event(s) to DHIS2");
		
		try {
			String data = Dhis2Utils.buildEventsContent(encounter.getObs());
			return dhis2HttpClient.sendEvents(data);
		}
		catch (IOException e) {
			log.error("Failed to submit event(s) to DHIS2 for patient with id: " + patient.getId());
			return false;
		}*/
	}
	
	private boolean hasNewHivCaseEvent(Encounter e) {
		for (Obs o : e.getObs()) {
			Concept codedValue = o.getValueCoded();
			if (codedValue == null) {
				continue;
			}
			for (ConceptMap map : codedValue.getConceptMappings()) {
				String hl7Code = map.getConceptReferenceTerm().getConceptSource().getHl7Code();
				String code = map.getConceptReferenceTerm().getCode();
				if (CODE_SYSTEM_CIEL.equals(hl7Code) && CIEL_CODE_NEW_HIV_CASE.equals(code)) {
					return true;
				}
			}
		}
		
		return false;
	}
	
	private boolean isTriggerObs(Obs obs) {
		Concept c = obs.getConcept();
		for (ConceptMap map : c.getConceptMappings()) {
			ConceptReferenceTerm term = map.getConceptReferenceTerm();
			if (Dhis2TrackerConstants.TRIGGER_CONCEPT_CODE.equalsIgnoreCase(term.getCode())
			        && Dhis2TrackerConstants.TRIGGER_CONCEPT_SOURCE.equalsIgnoreCase(term.getConceptSource().getHl7Code())) {
				return true;
			}
		}
		
		return false;
	}
	
}
