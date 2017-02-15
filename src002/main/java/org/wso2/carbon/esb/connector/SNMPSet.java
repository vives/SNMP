package org.wso2.carbon.esb.connector;

import org.apache.axiom.om.OMElement;
import org.apache.commons.lang.StringUtils;
import org.apache.synapse.MessageContext;
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
			//sending data to the Agent
			ResponseEvent response = snmp.send(pdu, SNMPUtils.getTarget(messageContext));
			if (response != null) {
				if (log.isDebugEnabled()) {
					log.debug("Got SNMPSet Response from the Agent.");
				}
				PDU responsePDU = response.getResponse();

				if (responsePDU != null) {
					int errorStatus = responsePDU.getErrorStatus();
					int errorIndex = responsePDU.getErrorIndex();
					String errorStatusText = responsePDU.getErrorStatusText();

					if (errorStatus == PDU.noError) {
						OMElement element;
						String responseMessage = responsePDU.getVariableBindings().toString();
						String result =
								SNMPConstants.START_TAG + "OIDs successfully updated with " +
								responseMessage + SNMPConstants.END_TAG;
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
			handleException("Error while processing the SNMPSet: " + e.getMessage(), e,
			                messageContext);
		} catch (XMLStreamException e) {
			handleException("Error while building the message: " + e.getMessage(), e,
			                messageContext);
		} finally {
			try {
				snmp.close();
			} catch (IOException e) {
				handleException("Error while closing the SNMP: " + e.getMessage(), e,
				                messageContext);
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
				handleException(
						"Error while adding the OIDs and values into the PDU." + e.getMessage(), e,
						messageContext);
			}
		} else {
			handleException("The updates values are empty.", messageContext);
		}
	}
}