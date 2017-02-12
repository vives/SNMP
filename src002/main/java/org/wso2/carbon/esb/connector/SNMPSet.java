package org.wso2.carbon.esb.connector;

import org.apache.axiom.om.OMElement;
import org.apache.commons.lang.StringUtils;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.smi.*;
import org.wso2.carbon.connector.core.AbstractConnector;
import org.wso2.carbon.connector.core.ConnectException;
import org.wso2.carbon.connector.core.Connector;

import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

public class SNMPSet extends AbstractConnector implements Connector {
    private static Snmp snmp;

    @Override
    public void connect(MessageContext messageContext) throws ConnectException {
        String updateOids = (String) messageContext.getProperty(SNMPConstants.UPDATE_OIDS);
        try {
            // Create TransportMapping and Listen
            snmp = (Snmp) messageContext.getProperty(SNMPConstants.SNMP);
            // Create the PDU object
            PDU pdu = new PDU();
            addPDU(updateOids, pdu, messageContext);
            pdu.setType(PDU.SET);
            ResponseEvent response = snmp.send(pdu, SNMPUtils.getTarget(messageContext));
            if (response != null) {
                if (log.isDebugEnabled()) {
                    log.debug("Got Snmp Set Response from Agent");
                }
                PDU responsePDU = response.getResponse();

                if (responsePDU != null) {
                    int errorStatus = responsePDU.getErrorStatus();
                    int errorIndex = responsePDU.getErrorIndex();
                    String errorStatusText = responsePDU.getErrorStatusText();

                    if (errorStatus == PDU.noError) {
                        OMElement element;
                        if (log.isDebugEnabled()) {
                            log.debug("SNMP Set response.");
                        }
                        String responseMessage = responsePDU.getVariableBindings().toString();
                        if (log.isDebugEnabled()) {
                            log.debug("SNMP Set response :" + responseMessage);
                        }
                        Vector<? extends VariableBinding> vbs = responsePDU.getVariableBindings();
                        for (VariableBinding vb : vbs) {
                            log.info("@@@@@@@@@@@@@@@@@@@@@@@@@@@@2 " + vb.getVariable().getSyntaxString());
                        }

                        String result = SNMPConstants.START_TAG + "OIDs successfully set with " +
                                responseMessage + SNMPConstants.END_TAG;
                        log.info(responseMessage);
                        element = SNMPUtils.transformMessages(result);
                        SNMPUtils.preparePayload(messageContext, element);
                    } else {
                        throw new SynapseException("Request Failed:" + "Status = " + errorStatus +
                                " Index = " + errorIndex + " Status Text = " + errorStatusText);
                    }
                } else {
                    log.warn("SNMP Timeout occured.");
                    throw new SynapseException("Response PDU is null.");
                }
            } else {
                throw new SynapseException("Agent Timeout occurred.");
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

    /**
     * Add the OIDs and values into PDU
     *
     * @param updateOids set of OIDs, it's values and data type
     * @return pdu
     */
    private void addPDU(String updateOids, PDU pdu, MessageContext messageContext) {
        if (StringUtils.isNotEmpty(updateOids)) {
            JSONObject jsonObject;
            try {
                JSONArray jsonArray = new JSONArray(updateOids);
                for (int i = 0; jsonArray.length() > i; ++i) {
                    jsonObject = jsonArray.getJSONObject(i);
                    String oid = jsonObject.getString(SNMPConstants.OID);
                    String UpdateValue = jsonObject.getString(SNMPConstants.VALUE);
                    String type = jsonObject.getString(SNMPConstants.TYPE);
                    if (type.equalsIgnoreCase(SNMPConstants.STRING) ||
                            type.equalsIgnoreCase(SNMPConstants.OCTETSTRING)) {
                        pdu.add(new VariableBinding(new OID(oid), new OctetString(UpdateValue)));
                    } else if (type.equalsIgnoreCase(SNMPConstants.INTEGER)) {
                        pdu.add(new VariableBinding(new OID(oid),
                                new Integer32(Integer.parseInt(UpdateValue))));
                    } else if ((type.equalsIgnoreCase(SNMPConstants.IPADDRESS))) {
                        pdu.add(new VariableBinding(new OID(oid), new IpAddress(UpdateValue)));
                    } else if ((type.equalsIgnoreCase(SNMPConstants.OID))) {
                        pdu.add(new VariableBinding(new OID(oid), new OID(UpdateValue)));
                    } else if ((type.equalsIgnoreCase(SNMPConstants.COUNTER32))) {
                        pdu.add(new VariableBinding(new OID(oid),
                                new Counter32(Integer.parseInt(UpdateValue))));
                    } else if ((type.equalsIgnoreCase(SNMPConstants.COUNTER64))) {
                        pdu.add(new VariableBinding(new OID(oid),
                                new Counter64(Integer.parseInt(UpdateValue))));
                    }
                }
            } catch (JSONException e) {
                handleException("Error while adding the OIDs and values into the PDU.", e,
                        messageContext);
            }
        } else {
            handleException("The snmpSet values are empty.", messageContext);
        }
    }
}