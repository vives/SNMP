package org.wso2.carbon.esb.connector;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.util.AXIOMUtil;
import org.apache.axiom.soap.SOAPBody;
import org.apache.synapse.MessageContext;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.snmp4j.CommunityTarget;
import org.snmp4j.PDU;
import org.snmp4j.Target;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.UdpAddress;
import org.snmp4j.smi.VariableBinding;

import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class SNMPUtils {
    private static final Log log = LogFactory.getLog(SNMPUtils.class);

    /**
     * Create Target Address object
     *
     * @return target
     */
    public static Target getTarget(MessageContext messageContext) {
        String host = (String) messageContext.getProperty(SNMPConstants.HOST);
        String port = (String) messageContext.getProperty(SNMPConstants.PORT);
        String snmpVersion = (String) messageContext.getProperty(SNMPConstants.SNMP_VERSION);
        String community = (String) messageContext.getProperty(SNMPConstants.COMMUNITY);
        String retries = (String) messageContext.getProperty(SNMPConstants.RETRIES);
        String timeout = (String) messageContext.getProperty(SNMPConstants.TIMEOUT);

        CommunityTarget target = new CommunityTarget();
        target.setCommunity(new OctetString(community));
        target.setAddress(new UdpAddress(host + SNMPConstants.COMBINER + port));
        if(StringUtils.isEmpty(snmpVersion)){
            target.setVersion(SnmpConstants.version2c);
        } else {
            target.setVersion(Integer.parseInt(snmpVersion));
        }
        if (StringUtils.isEmpty(retries)) {
            target.setRetries(Integer.parseInt(SNMPConstants.DEFAULT_RETRIES));
        }else{
            target.setRetries(Integer.parseInt(retries));
        }
        if (StringUtils.isEmpty(timeout)) {
            target.setTimeout(Integer.parseInt(SNMPConstants.DEFAULT_TIMEOUT));
        }else{
            target.setTimeout(Integer.parseInt(timeout));
        }
        return target;
    }

    /**
     * Add the multiple OIDs into PDU
     *
     * @param oids set of OIDs
     * @return pdu
     */
    public static PDU addOids(String oids, PDU pdu) throws IOException {
        List<String> oidsList = null;
        if (StringUtils.isNotEmpty(oids)) {
            oidsList = Arrays.asList(oids.split(SNMPConstants.COMMA));
        }
        if (oidsList != null) {
            for (String oid : oidsList) {
                pdu.add(new VariableBinding(new OID(oid)));
            }
        }
        return pdu;
    }

    /**
     * Prepare payload
     *
     * @param messageContext The message context that is processed by a handler in the handle method
     * @param element        OMElement
     */

    public static void preparePayload(org.apache.synapse.MessageContext messageContext,
                                      OMElement element) {
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
    public static OMElement transformMessages(String output) throws XMLStreamException {
        OMElement resultElement;
        resultElement = AXIOMUtil.stringToOM(output);
        return resultElement;
    }
}
