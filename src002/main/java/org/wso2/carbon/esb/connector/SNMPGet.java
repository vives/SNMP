/*
 * Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wso2.carbon.esb.connector;

import org.apache.axiom.om.OMElement;
import org.apache.commons.lang.StringUtils;
import org.apache.synapse.MessageContext;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.smi.Integer32;
import org.wso2.carbon.connector.core.AbstractConnector;
import org.wso2.carbon.connector.core.ConnectException;
import org.wso2.carbon.connector.core.Connector;

import javax.xml.stream.XMLStreamException;
import java.io.IOException;

public class SNMPGet extends AbstractConnector implements Connector {
    private static Snmp snmp;

    @Override
    public void connect(MessageContext messageContext) throws ConnectException {
        String oids = (String) messageContext.getProperty(SNMPConstants.OIDs);
        String requestId = (String) messageContext.getProperty(SNMPConstants.REQUEST_ID);
        try {
            // Create TransportMapping and Listen
            snmp = (Snmp) messageContext.getProperty(SNMPConstants.SNMP);
            // Create the PDU object
            PDU pdu = new PDU();
            SNMPUtils.addOids(oids, pdu);
            pdu.setType(PDU.GET);
            //Set the request ID for this PDU
            if (StringUtils.isNotEmpty(requestId)) {
                pdu.setRequestID(new Integer32(Integer.parseInt(requestId)));
            }
            //Sending data to Agent
            if (log.isDebugEnabled()) {
                log.debug("Sending Request to Agent.");
            }
            ResponseEvent response = snmp.send(pdu, SNMPUtils.getTarget(messageContext));
            // Process Agent Response
            if (response != null) {
                if (log.isDebugEnabled()) {
                    log.debug("Got Response from Agent.");
                }
                PDU responsePDU = response.getResponse();
                if (responsePDU != null) {
                    int errorStatus = responsePDU.getErrorStatus();
                    int errorIndex = responsePDU.getErrorIndex();
                    String errorStatusText = responsePDU.getErrorStatusText();
                    if (errorStatus == PDU.noError) {
                        OMElement element;
                        if (log.isDebugEnabled()) {
                            log.debug("SNMP Get Response.");
                        }
                        String responseMessage = responsePDU.getVariableBindings().toString();
                        String result = SNMPConstants.START_TAG + responseMessage + SNMPConstants.END_TAG;
                        element = SNMPUtils.transformMessages(result);
                        SNMPUtils.preparePayload(messageContext, element);
                    } else {
                        handleException(
                                "Request Failed:" + "Status = " + errorStatus + ", Index = " +
                                errorIndex + ", Status Text = " + errorStatusText, messageContext);
                    }
                } else {
                    handleException("Response PDU is null.",messageContext);
                }
            } else {
                handleException("Agent Timeout occurred.",messageContext);
            }
        } catch (IOException e) {
            handleException("Error while processing the SNMPGet: " + e.getMessage(), e, messageContext);
        } catch (XMLStreamException e) {
            handleException("Error while building the message: " + e.getMessage(), e, messageContext);
        } finally {
            try {
                snmp.close();
            } catch (IOException e) {
                handleException("Error while closing the SNMP: " + e.getMessage(), e, messageContext);
            }
        }
    }
}
