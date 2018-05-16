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

import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static org.junit.Assert.assertEquals;
import static org.openmrs.module.dhis2tracker.Dhis2TrackerConstants.CONTENT_TYPE_JSON;
import static org.openmrs.module.dhis2tracker.Dhis2TrackerConstants.DATE_FORMATTER;
import static org.openmrs.module.dhis2tracker.Dhis2TrackerConstants.HEADER_ACCEPT;
import static org.openmrs.module.dhis2tracker.Dhis2TrackerConstants.HEADER_CONTENT_TYPE;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.Date;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpStatus;
import org.junit.Rule;
import org.junit.Test;
import org.openmrs.GlobalProperty;
import org.openmrs.Patient;
import org.openmrs.PersonName;
import org.openmrs.api.APIException;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.context.Context;
import org.openmrs.test.BaseModuleContextSensitiveTest;
import org.openmrs.util.OpenmrsClassLoader;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit.WireMockRule;

public class Dhis2HttpClientTest extends BaseModuleContextSensitiveTest {
	
	private static final Integer DHIS2_PORT = getAvailablePort();
	
	private static final String SUCCESS_RESPONSE_JSON = "success_response.json";
	
	private static final String FAILURE_RESPONSE_JSON = "failure_response.json";
	
	private Dhis2HttpClient dhis2HttpClient = Dhis2HttpClient.newInstance();
	
	@Rule
	public WireMockRule wireMockRule = new WireMockRule(DHIS2_PORT);
	
	public static Integer getAvailablePort() {
		
		for (int i = 1024; i < 49151; i++) {
			try {
				new ServerSocket(i).close();
				return i;
			}
			catch (IOException e) {
				//Port is not available for use
			}
		}
		
		//Really! No port is available?
		throw new APIException("No available port found");
	}
	
	public static void setDhis2Port(Integer port) {
		AdministrationService as = Context.getAdministrationService();
		GlobalProperty gp = as.getGlobalPropertyObject(Dhis2TrackerConstants.GP_URL);
		gp.setPropertyValue(StringUtils.replaceOnce(gp.getPropertyValue(), "{{PORT}}", port.toString()));
		as.saveGlobalProperty(gp);
	}
	
	public static String getResponse(boolean success) throws IOException {
		String filename = success ? SUCCESS_RESPONSE_JSON : FAILURE_RESPONSE_JSON;
		return IOUtils.toString(OpenmrsClassLoader.getInstance().getResourceAsStream(filename));
	}
	
	public static void createPostStub(String resource, boolean withSuccessResponse) throws IOException {
		
		final int sc = withSuccessResponse ? HttpStatus.SC_OK : HttpStatus.SC_INTERNAL_SERVER_ERROR;
		
		WireMock.stubFor(
		    WireMock.post(WireMock.urlEqualTo("/" + resource)).withHeader(HEADER_ACCEPT, containing(CONTENT_TYPE_JSON))
		            .withHeader(HEADER_CONTENT_TYPE, containing(CONTENT_TYPE_JSON)).withRequestBody(containing(""))
		            .withBasicAuth("fake user", "fake password").willReturn(WireMock.aResponse().withStatus(sc)
		                    .withHeader("Content-Type", CONTENT_TYPE_JSON).withBody(getResponse(withSuccessResponse))));
	}
	
	@Test
	public void registerAndEnroll_shouldRegisterAndEnrollThePatientWithDhis2Tracker() throws Exception {
		executeDataSet("moduleTestData-initial.xml");
		final String expectedUid = "z2v7tDgvurD";
		Patient p = new Patient();
		p.addName(new PersonName("Horacio", "Tom", "Hornblower"));
		p.setGender("M");
		p.setBirthdate(DATE_FORMATTER.parse("1980-04-20"));
		Date incidenceDate = DATE_FORMATTER.parse("2018-04-20");
		setDhis2Port(DHIS2_PORT);
		createPostStub(Dhis2HttpClient.RESOURCE_REGISTER_AND_ENROLL, true);
		
		String uid = dhis2HttpClient.registerAndEnroll(p, incidenceDate);
		assertEquals(expectedUid, uid);
	}
	
}
