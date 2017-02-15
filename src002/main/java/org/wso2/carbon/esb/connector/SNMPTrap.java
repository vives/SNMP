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
import org.apache.synapse.MessageContext;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.VariableBinding;
import org.wso2.carbon.connector.core.AbstractConnector;
import org.wso2.carbon.connector.core.ConnectException;
import org.wso2.carbon.connector.core.Connector;

import javax.xml.stream.XMLStreamException;
import java.io.IOException;

public class SNMPTrap extends AbstractConnector implements Connector {
    private static Snmp snmp;

    @Override
    public void connect(MessageContext messageContext) throws ConnectException {
        String trapOids = ((String) messageContext.getProperty(SNMPConstants.TRAP_OIDS));

        try {
            //Create Transport Mapping
            snmp = (Snmp) messageContext.getProperty(SNMPConstants.SNMP);
            //Create PDU
            PDU pdu = new PDU();
            // To specify the system up time
//            pdu.add(new VariableBinding(SnmpConstants.sysUpTime,
//                    new OctetString(new Date().toString())));
            // variable binding for Enterprise Specific objects, Severity (should be defined in MIB
            // file)
            addPDU(trapOids, pdu);
            pdu.setType(PDU.INFORM);
            //Send the PDU
            ResponseEvent response = snmp.send(pdu, SNMPUtils.getTarget(messageContext));
            // Process the Response
            if (response != null) {
                if (log.isDebugEnabled()) {
                    log.debug("Got response from Agent.");
                }

                System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!   " + response.toString());
                PDU responsePDU = response.getResponse();
                System.out.println("##############################3  " + responsePDU.toString());
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
                        String result = SNMPConstants.START_TAG + "Sending the Trap to " +
                                messageContext.getProperty(SNMPConstants.HOST) + " on Port " +
                                messageContext.getProperty(SNMPConstants.PORT) +
                                " with " + responseMessage + SNMPConstants.END_TAG;
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
        } catch (XMLStreamException e) {
            handleException("Error occur when constructing OMElement" + e.getMessage(), e,
                    messageContext);
        } catch (IOException e) {
            handleException("Error in Sending the Trap: " + e.getMessage(), e, messageContext);
        } finally {
            try {
               snmp.close();
            } catch (IOException e) {
                handleException("Error in closing the snmp: " + e.getMessage(), e, messageContext);
            }
        }
    }
    /**
     * This method is capable of handling multiple OIDs
     *
     * @param oidValues set of OIDs and value pairs
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
}