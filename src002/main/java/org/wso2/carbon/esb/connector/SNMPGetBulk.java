package org.wso2.carbon.esb.connector;

import org.apache.axiom.om.OMElement;
import org.apache.commons.lang.StringUtils;
import org.apache.synapse.MessageContext;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.smi.VariableBinding;
import org.wso2.carbon.connector.core.AbstractConnector;
import org.wso2.carbon.connector.core.ConnectException;
import org.wso2.carbon.connector.core.Connector;

import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.util.Vector;

public class SNMPGetBulk extends AbstractConnector implements Connector {
	private static Snmp snmp;

	@Override
	public void connect(MessageContext messageContext) throws ConnectException {
		String oids = (String) messageContext.getProperty(SNMPConstants.OIDs);
		String maxRepetition = (String) messageContext.getProperty(SNMPConstants.MAX_REPETITION);
		String nonRepeater = (String) messageContext.getProperty(SNMPConstants.NON_REPEATER);

		try {
			if (StringUtils.isNotEmpty(maxRepetition)) {
				// Create TransportMapping and Listen
				snmp = (Snmp) messageContext.getProperty(SNMPConstants.SNMP);
				// Create the PDU object
				PDU pdu = new PDU();
				SNMPUtils.addOids(oids, pdu);
				pdu.setType(PDU.GETBULK);

				//set the max-repetitions
				pdu.setMaxRepetitions(Integer.parseInt(maxRepetition));

				//set the nonrepeaters
				if (StringUtils.isNotEmpty(nonRepeater)) {
					pdu.setNonRepeaters(Integer.parseInt(nonRepeater));
				}
				if (log.isDebugEnabled()) {
					log.debug("Sending Request to the Agent.");
				}
				//sending data to Agent
				ResponseEvent response = snmp.send(pdu, SNMPUtils.getTarget(messageContext));
				// Process Agent Response
				if (response != null) {
					if (log.isDebugEnabled()) {
						log.debug("Got Response from the Agent.");
					}
					PDU responsePDU = response.getResponse();
					if (responsePDU != null) {
						int errorStatus = responsePDU.getErrorStatus();
						int errorIndex = responsePDU.getErrorIndex();
						String errorStatusText = responsePDU.getErrorStatusText();
						if (errorStatus == PDU.noError) {
							OMElement element;
							String result = "";
							Vector<? extends VariableBinding> vbs =
									responsePDU.getVariableBindings();
							for (VariableBinding vb : vbs) {
								result = result + vb + ", ";
							}
							result = result.trim().substring(0, result.trim().length() - 1);
							result = SNMPConstants.START_TAG + result + SNMPConstants.END_TAG;
							element = SNMPUtils.transformMessages(result);
							SNMPUtils.preparePayload(messageContext, element);
						} else {
							handleException(
									"Request Failed:" + "Status = " + errorStatus + ", Index = " +
									errorIndex + ", Status Text = " + errorStatusText, messageContext);
						}
					} else {
						handleException("Response PDU is null.", messageContext);
					}
				} else {
					handleException("Agent Timeout occurred.", messageContext);
				}
			} else {
				handleException("MaxRepetition value is null.", messageContext);
			}
		} catch (IOException e) {
			handleException("Error while processing the SNMPGetBulk: " + e.getMessage(), e,
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
}
