package org.wso2.carbon.esb.connector;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.util.AXIOMUtil;
import org.apache.axiom.soap.SOAPBody;
import org.apache.commons.lang.StringUtils;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseException;
import org.json.JSONException;
import org.json.JSONObject;
import org.snmp4j.CommunityTarget;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.Target;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.UdpAddress;
import org.snmp4j.smi.VariableBinding;
import org.wso2.carbon.connector.core.AbstractConnector;
import org.wso2.carbon.connector.core.ConnectException;
import org.wso2.carbon.connector.core.Connector;

import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.util.Iterator;

public class SNMPSet extends AbstractConnector implements Connector {
	private static Snmp snmp;

	@Override
	public void connect(MessageContext messageContext) throws ConnectException {
		String ipAddress = (String) messageContext.getProperty(SNMPConstants.IP_ADDRESS);
		String port = (String) messageContext.getProperty(SNMPConstants.PORT);
		String setoids = (String) messageContext.getProperty(SNMPConstants.SET_OIDS);
		String snmpVersion = (String) messageContext.getProperty(SNMPConstants.SNMP_VERSION);
		String community = (String) messageContext.getProperty(SNMPConstants.COMMUNITY);
		String retries = (String) messageContext.getProperty(SNMPConstants.RETRIES);
		String timeout = (String) messageContext.getProperty(SNMPConstants.TIMEOUT);
		try {
			// Create TransportMapping and Listen
			snmp = (Snmp) messageContext.getProperty(SNMPConstants.SNMP);
			// Create the PDU object
			PDU pdu = new PDU();
			addPDU(setoids, pdu,messageContext);
			pdu.setType(PDU.SET);
			ResponseEvent response = snmp.send(pdu, getTarget(community, ipAddress, port, snmpVersion, retries, timeout));
			if (response != null)
			{
				if (log.isDebugEnabled()) {
					log.debug("Got Snmp Set Response from Agent");
				}
				PDU responsePDU = response.getResponse();

				if (responsePDU != null)
				{
					int errorStatus = responsePDU.getErrorStatus();
					int errorIndex = responsePDU.getErrorIndex();
					String errorStatusText = responsePDU.getErrorStatusText();

					if (errorStatus == PDU.noError)
					{
						OMElement element;
						if (log.isDebugEnabled()) {
							log.debug("SNMP Set response.");
						}
						String responseMessage = responsePDU.getVariableBindings().toString();
						if (log.isDebugEnabled()) {
							log.debug("SNMP Set response :" + responseMessage);
						}
						String result = SNMPConstants.START_TAG + "OIDs successfully set." + SNMPConstants.END_TAG;
						element = transformMessages(result);
						preparePayload(messageContext, element);
					}
					else
					{
						throw new SynapseException("Request Failed:" + "Status = " + errorStatus +
						                           " Index=" + errorIndex + " Status Text=" + errorStatusText);
					}
				}
				else
				{
					throw new SynapseException("Response PDU is null.");
				}
			}
			else
			{
				log.error("Agent Timeout occurred.");
			}

		} catch (Exception e) {
			handleException("IO error occur " + e.getMessage(), e, messageContext);
		} finally {
			try {
				snmp.close();
			} catch (IOException e) {
				handleException("Error while closing the SNMP " + e.getMessage(), e, messageContext);
			}
		}
	}

	/**
	 * This method is capable of handling multiple OIDs
	 *
	 * @param oidValues set of OIDs and message pairs
	 * @return pdu
	 */
	private void addPDU(String oidValues, PDU pdu,MessageContext messageContext) {
		if (StringUtils.isNotEmpty(oidValues)) {
			try {
				JSONObject object = new JSONObject(oidValues);
				Iterator keys = object.keys();
				while (keys.hasNext()) {
					String oid = (String) keys.next();
					String value = object.getString(oid);
					pdu.add(new VariableBinding(new OID(oid), new OctetString(value)));
				}
			}catch (JSONException e){
				handleException("Error while adding the OIDs and values into PDU" + e.getMessage(),e,messageContext);
			}
		}
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
	 * Prepare payload
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
