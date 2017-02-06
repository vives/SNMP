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
import org.apache.axiom.om.util.AXIOMUtil;
import org.apache.axiom.soap.SOAPBody;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.snmp4j.CommunityTarget;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.Target;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.UdpAddress;
import org.snmp4j.smi.VariableBinding;
import org.wso2.carbon.connector.core.AbstractConnector;
import org.wso2.carbon.connector.core.ConnectException;
import org.wso2.carbon.connector.core.Connector;

import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.util.Date;
import java.util.Iterator;

public class SendMessage extends AbstractConnector implements Connector {
    private static Snmp snmp;

    @Override
    public void connect(MessageContext messageContext) throws ConnectException {
        String ipAddress = (String) messageContext.getProperty(org.wso2.carbon.esb.connector.SNMPConstants.IP_ADDRESS);
        String port = (String) messageContext.getProperty(org.wso2.carbon.esb.connector.SNMPConstants.PORT);
        String oidValue = (String) messageContext.getProperty(org.wso2.carbon.esb.connector.SNMPConstants.OID_VALUE);
        String snmpVersion = (String) messageContext.getProperty(SNMPConstants.SNMP_VERSION);
        String community = (String) messageContext.getProperty(SNMPConstants.COMMUNITY);
        String retries = (String) messageContext.getProperty(SNMPConstants.RETRIES);
        String timeout = (String) messageContext.getProperty(SNMPConstants.TIMEOUT);
        try {
            //Create Transport Mapping
            snmp = (Snmp) messageContext.getProperty(SNMPConstants.SNMP);
            //Create PDU
            PDU pdu = new PDU();
            // To specify the system up time
            pdu.add(new VariableBinding(SnmpConstants.sysUpTime, new OctetString(new Date().toString())));
            // variable binding for Enterprise Specific objects, Severity (should be defined in MIB file)
            addPDU(oidValue, pdu);
            pdu.setType(PDU.INFORM);
            //Send the PDU
            ResponseEvent response = snmp.send(pdu, getTarget(community, ipAddress, port, snmpVersion, retries, timeout));
            // Process the Response
            if (response != null) {
                if (log.isDebugEnabled()) {
                    log.debug("Got response from Agent.");
                }
                PDU responsePDU = response.getResponse();
                if (responsePDU != null) {
                    int errorStatus = responsePDU.getErrorStatus();
                    int errorIndex = responsePDU.getErrorIndex();
                    String errorStatusText = responsePDU.getErrorStatusText();
                    if (errorStatus == PDU.noError) {
                        OMElement element;
                        if (log.isDebugEnabled()) {
                            log.debug("SNMP Get response.");
                        }
                        String responseMessage = responsePDU.getVariableBindings().toString();
                        String result = SNMPConstants.START_TAG + "Sending V2 Trap to " + ipAddress + " on Port " + port +
                                "  Response: " + responseMessage + SNMPConstants.END_TAG;
                        element = transformMessages(result);
                        preparePayload(messageContext, element);
                    } else {
                        throw new SynapseException("Request Failed:" + "Status = " + errorStatus +
                                " Index=" + errorIndex + " Status Text=" + errorStatusText);
                    }
                } else {
                    throw new SynapseException("Response PDU is null.");
                }
            } else {
                log.error("Agent Timeout occurred.");
            }
        } catch (XMLStreamException e) {
            handleException("Error occur when constructing OMElement" + e.getMessage(), e, messageContext);
        } catch (IOException e) {
            handleException("Error in Sending V2 Trap to " + ipAddress + " on Port " +
                    port + e.getMessage(), e, messageContext);
        } finally {
            try {
                stop();
            } catch (IOException e) {
                handleException("Error in Sending V2 Trap to " + ipAddress + " on Port " +
                        port + e.getMessage(), e, messageContext);
            }
        }
    }

    /**
     * Since snmp4j relies on asynch req/resp we need a listener for responses which should be closed.
     */
    private void stop() throws IOException {
        snmp.close();
    }

    /**
     * Create Target Address object
     *
     * @return target
     */
    private static Target getTarget(String community, String ipAddress, String port, String version,
                                    String retries, String timeout) {
        CommunityTarget target = new CommunityTarget();
        target.setCommunity(new OctetString(community));
        target.setAddress(new UdpAddress(ipAddress + SNMPConstants.COMBINER + port));
        target.setVersion(Integer.parseInt(version));
        target.setRetries(Integer.parseInt(retries));
        target.setTimeout(Integer.parseInt(timeout));
        return target;
    }

    /**
     * This method is capable of handling multiple OIDs
     *
     * @param oidValues set of OIDs and message pairs
     * @return pdu
     */
    private PDU addPDU(String oidValues, PDU pdu) throws IOException {
        JSONObject jsonObject;
        try {
            JSONArray jsonArray = new JSONArray(oidValues);
            for (int i = 0; jsonArray.length() > i; ++i) {
                jsonObject = jsonArray.getJSONObject(i);
                String oid = jsonObject.getString(SNMPConstants.OID);
                String value = jsonObject.getString(SNMPConstants.VALUE);
                pdu.add(new VariableBinding(new OID(oid), new OctetString(value)));
            }
        } catch (JSONException e) {
            handleException("Error while handling JSON object.", (MessageContext) e);
        }
        return pdu;
    }

    /**
     * Prepare pay load
     *
     * @param messageContext The message context that is processed by a handler in the handle method
     * @param element        OMElement
     */

    private void preparePayload(MessageContext messageContext, OMElement element) {
        SOAPBody soapBody = messageContext.getEnvelope().getBody();
        for (Iterator itr = soapBody.getChildElements(); itr.hasNext(); ) {
            OMElement child = (OMElement) itr.next();
            child.detach();
        }
        for (Iterator itr = element.getChildElements(); itr.hasNext(); ) {
            OMElement child = (OMElement) itr.next();
            soapBody.addChild(child);
        }
    }

    /**
     * Create a OMElement
     *
     * @param output output
     * @return return resultElement
     */
    private OMElement transformMessages(String output) throws XMLStreamException {
        OMElement resultElement;
        resultElement = AXIOMUtil.stringToOM(output);
        return resultElement;
    }
}