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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.map.ObjectMapper;
import org.openmrs.Concept;
import org.openmrs.Patient;
import org.openmrs.api.APIException;
import org.openmrs.api.context.Context;
import org.openmrs.module.dhis2tracker.model.Attribute;
import org.openmrs.module.dhis2tracker.model.Enrollment;
import org.openmrs.module.dhis2tracker.model.RegisterAndEnroll;

public class Dhis2Utils {
	
	private static final ObjectMapper mapper = new ObjectMapper();
	
	public static String buildRegisterAndEnrollContent(Patient patient, Date date) throws IOException {
		List<Attribute> attributes = new ArrayList<>();
		attributes.add(new Attribute(getPersonIdUID(), patient.getPatientIdentifier().getIdentifier()));
		attributes.add(new Attribute(getFirstnameUID(), patient.getGivenName()));
		attributes.add(new Attribute(getMiddlenameUID(), patient.getMiddleName()));
		attributes.add(new Attribute(getLastnameUID(), patient.getFamilyName()));
		attributes
		        .add(new Attribute(getBirthdateUID(), Dhis2TrackerConstants.DATE_FORMATTER.format(patient.getBirthdate())));
		String gender;
		if ("M".equals(patient.getGender())) {
			gender = getFemaleOptionUID();
		} else if ("F".equals(patient.getGender())) {
			gender = getMaleOptionUID();
		} else {
			throw new APIException("Unknown gender '" + patient.getGender() + "' for patient with id: " + patient.getId());
		}
		attributes.add(new Attribute(Dhis2Utils.getGenderUID(), gender));
		String incidentDate = Dhis2TrackerConstants.DATE_FORMATTER.format(date);
		attributes.add(new Attribute(Dhis2Utils.getDateOfDiagnosisUID(), incidentDate));
		Enrollment enrollment = new Enrollment(Dhis2Utils.getProgramUID(), incidentDate);
		RegisterAndEnroll ene = new RegisterAndEnroll(Dhis2Utils.getTrackedEntityTypeUID(), Dhis2Utils.getOrgUnitUID(),
		        attributes, enrollment);
		
		return mapper.writeValueAsString(ene);
	}
	
	private static String getMaleOptionUID() {
		return getGlobalProperty(Dhis2TrackerConstants.GP_OPTION_MALE_UID);
	}
	
	private static String getFemaleOptionUID() {
		return getGlobalProperty(Dhis2TrackerConstants.GP_OPTION_FEMALE_UID);
	}
	
	public static String getUrl() {
		return getGlobalProperty(Dhis2TrackerConstants.GP_URL);
	}
	
	public static String getUsername() {
		return getGlobalProperty(Dhis2TrackerConstants.GP_USERNAME);
	}
	
	public static String getPassword() {
		return getGlobalProperty(Dhis2TrackerConstants.GP_PASSWORD);
	}
	
	public static String getOrgUnitUID() {
		return getGlobalProperty(Dhis2TrackerConstants.GP_ORG_UNIT_UID);
	}
	
	public static String getTrackedEntityTypeUID() {
		return getGlobalProperty(Dhis2TrackerConstants.GP_TRACKED_ENTITY_TYPE_UID);
	}
	
	public static String getProgramUID() {
		return getGlobalProperty(Dhis2TrackerConstants.GP_PROGRAM_UID);
	}
	
	public static String getFirstnameUID() {
		return getGlobalProperty(Dhis2TrackerConstants.GP_ATTRIB_FIRSTNAME_UID);
	}
	
	public static String getMiddlenameUID() {
		return getGlobalProperty(Dhis2TrackerConstants.GP_ATTRIB_MIDDLENAME_UID);
	}
	
	public static String getLastnameUID() {
		return getGlobalProperty(Dhis2TrackerConstants.GP_ATTRIB_LASTNAME_UID);
	}
	
	public static String getGenderUID() {
		return getGlobalProperty(Dhis2TrackerConstants.GP_ATTRIB_GENDER_UID);
	}
	
	public static String getBirthdateUID() {
		return getGlobalProperty(Dhis2TrackerConstants.GP_ATTRIB_BIRTHDATE_UID);
	}
	
	public static String getPersonIdUID() {
		return getGlobalProperty(Dhis2TrackerConstants.GP_ATTRIB_PERSON_ID_UID);
	}
	
	public static String getDateOfDiagnosisUID() {
		return getGlobalProperty(Dhis2TrackerConstants.GP_ATTRIB_DATE_OF_HIV_DIAGNOSIS_UID);
	}
	
	public static String getCaseReportEncounterTypeName() {
		String mapping = getGlobalProperty(Dhis2TrackerConstants.GP_CONCEPT_MAPPING_PUBLIC_HEALTH_CR);
		if (StringUtils.isBlank(mapping)) {
			throw new APIException(
			        Dhis2TrackerConstants.GP_CONCEPT_MAPPING_PUBLIC_HEALTH_CR + " global property value is required");
		}
		
		String[] fields = StringUtils.split(mapping, ":");
		if (fields.length != 2) {
			throw new APIException("Invalid value for the " + Dhis2TrackerConstants.GP_CONCEPT_MAPPING_PUBLIC_HEALTH_CR
			        + " global property");
		}
		
		final String code = fields[1];
		final String source = fields[0];
		Concept concept = Context.getConceptService().getConceptByMapping(code, source);
		if (concept == null) {
			throw new APIException("No concept found with a mapping to source: " + source + " and code: " + code);
		}
		
		return concept.getName().getName();
	}
	
	/**
	 * Convenience method that gets the value of the specified global property name
	 *
	 * @param propertyName the global property name
	 * @return the global property value
	 */
	private static String getGlobalProperty(String propertyName) {
		return Context.getAdministrationService().getGlobalProperty(propertyName);
	}
	
}
