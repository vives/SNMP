import java.io.IOException;
import java.util.Vector;

import org.snmp4j.CommunityTarget;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.TransportMapping;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.event.ResponseListener;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.Address;
import org.snmp4j.smi.GenericAddress;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.VariableBinding;
import org.snmp4j.transport.DefaultUdpTransportMapping;

/**
        * @ Description snmp4j test
        * @ Author cuisuqiang
        * @ Version 1.0
        * @ Since
*/
public class SnmpUtil {
     Snmp snmp = null;
    private Address targetAddress = null;

    public void initComm () throws IOException {
        // Set the Agent's IP and port
        targetAddress = GenericAddress.parse ("udp: 127.0.0.1/161");
        TransportMapping transport = new DefaultUdpTransportMapping ();
        snmp = new Snmp(transport);
        transport.listen ();
    }

    public ResponseEvent sendPDU (PDU pdu) throws IOException {
        // Set the target
        CommunityTarget target = new CommunityTarget ();
        target.setCommunity (new OctetString ("public"));
        target.setAddress (targetAddress);
        // Communication unsuccessful retry count N +1
        target.setRetries (2);
        // Time-out
        target.setTimeout (2 * 1000);
        // SNMP version
        target.setVersion (SnmpConstants.version2c);

        // Set the listener object
        ResponseListener listener = new ResponseListener () {
            public void onResponse (ResponseEvent event) {
                System.out.println ("----------> Begins an asynchronous parsing <------------");
                readResponse (event);
            }
        };
        // Send packets
        snmp.send(pdu, target, null, listener);
        return null;
    }

    public void getPDU() throws IOException {
        // PDU object
        PDU pdu = new PDU();
        pdu.add (new VariableBinding (new OID ("1.2.3.4.5.6")));
        // Type of operation
        pdu.setType (PDU.GET);
        ResponseEvent revent = sendPDU (pdu);
        if (null != revent) {
            readResponse (revent);
        }
    }

    @ SuppressWarnings ("unchecked")
    public void readResponse (ResponseEvent respEvnt) {
        // Resolve Response.
        System.out.println ("------------> parse Response <-------------");
        if (respEvnt != null && respEvnt.getResponse()!= null) {
            Vector <VariableBinding> recVBs = (Vector<VariableBinding>) respEvnt.getResponse().getVariableBindings ();
            for (int i = 0; i <recVBs.size (); i ++) {
                VariableBinding recVB = recVBs.elementAt (i);
                System.out.println (recVB.getOid() + ":" +recVB.getVariable ().toString ());
            }
        }
    }

    public static void main (String [] args) {
        try {
            SnmpUtil util = new SnmpUtil ();
            util.initComm();
            util.getPDU();
        } catch (IOException e) {
            e.printStackTrace ();

        }
    }
}