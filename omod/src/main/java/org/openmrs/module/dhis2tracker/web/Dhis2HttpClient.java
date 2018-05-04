/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.dhis2tracker.web;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.io.IOException;

public class Dhis2HttpClient {

    protected Log log = LogFactory.getLog(getClass());

    private static final String DHIS2_URL = "http://localhost/";

    private Dhis2HttpClient() {
    }

    public static Dhis2HttpClient newInstance() {
        return new Dhis2HttpClient();
    }

    public boolean post(String resource, Object data) throws IOException {
        log.debug("Posting data to DHIS2");

        CloseableHttpClient httpclient = HttpClients.createDefault();
        try {
            HttpPost post = new HttpPost(DHIS2_URL + resource);
            CloseableHttpResponse response = httpclient.execute(post, null, null);

            try {
            } finally {
                response.close();
            }
        } finally {
            httpclient.close();
        }

        return false;
    }
}
