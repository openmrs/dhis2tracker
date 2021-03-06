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

import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Message;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Encounter;
import org.openmrs.api.context.Context;
import org.openmrs.api.context.Daemon;
import org.openmrs.event.EventListener;
import org.openmrs.module.DaemonToken;

public class CaseReportEventListener implements EventListener {
	
	protected Log log = LogFactory.getLog(getClass());
	
	private DaemonToken daemonToken;
	
	public CaseReportEventListener(DaemonToken daemonToken) {
		this.daemonToken = daemonToken;
	}
	
	/**
	 * @see EventListener#onMessage(Message)
	 */
	@Override
	public void onMessage(final Message message) {
		log.debug("Received encounter created event");
		
		Daemon.runInDaemonThread(new Runnable() {
			
			/**
			 * @see Runnable#run()
			 */
			@Override
			public void run() {
				
				try {
					//TODO in theory we want to save this event to the DB
					//before we submit it for tracking purposes and also just in case the
					//submission fails we can try again later.
					//saveCaseReport();
					ProcessorResult result = processMessage(message);
					log.info("Case report processor result: " + result);
				}
				catch (Exception e) {
					log.error("An error occurred while processing case report encounter", e);
				}
				
			}
			
		}, daemonToken);
		
	}
	
	/**
	 * Processes the specified JMS message
	 *
	 * @param message the message to process
	 * @return a {@link ProcessorResult}
	 * @throws JMSException
	 */
	public ProcessorResult processMessage(Message message) throws JMSException {
		log.debug("Processing JMS message");
		MapMessage mm = (MapMessage) message;
		String encUuid = mm.getString("uuid");
		Encounter encounter = Context.getEncounterService().getEncounterByUuid(encUuid);
		String caseReportEncType = Dhis2Utils.getCaseReportEncounterTypeName();
		String encTypeName = encounter.getEncounterType().getName();
		if (!caseReportEncType.equals(encTypeName)) {
			log.debug("Ignoring non case report encounter with encounter type: " + encTypeName);
			return ProcessorResult.IGNORED;
		}
		log.debug("Processing case report encounter with encounter type: " + encTypeName);
		
		return EncounterProcessor.newInstance().process(encounter);
	}
	
}
