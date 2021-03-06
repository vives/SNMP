import org.snmp4j.CommunityTarget;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.TransportMapping;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.event.ResponseListener;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.*;
import org.snmp4j.transport.DefaultUdpTransportMapping;

import java.util.Vector;

public class SnmpTest {
	public static void main(String args[]){
		SnmpTest snmp = new SnmpTest();
		//snmp.snmpSet("127.0.0.1","private",".1.3.6.1.2.1.1",1);

		snmp.snmpGet("127.0.0.1","public","1.3.6.1.2.1.31.1.1.1.1");

	}

	public void snmpSet(String host, String community, String strOID, int Value) {
		host= host+"/"+"161";
		Address tHost = GenericAddress.parse(host);
		Snmp snmp;
		try {
			TransportMapping transport = new DefaultUdpTransportMapping();
			snmp = new Snmp(transport);
			transport.listen();
			CommunityTarget target = new CommunityTarget();
			target.setCommunity(new OctetString(community));
			target.setAddress(tHost);
			target.setRetries(2);
			target.setTimeout(5000);
			target.setVersion(SnmpConstants.version2c); //Set the correct SNMP version here
			PDU pdu = new PDU();
			//Depending on the MIB attribute type, appropriate casting can be done here
			pdu.add(new VariableBinding(new OID(strOID), new Integer32(Value)));
			pdu.setType(PDU.SET);
			ResponseListener listener = new ResponseListener() {
				public void onResponse(ResponseEvent event) {
					PDU strResponse;
					String result;
					((Snmp)event.getSource()).cancel(event.getRequest(), this);
					strResponse = event.getResponse();
					if (strResponse!= null) {
						result = strResponse.getErrorStatusText();
						System.out.println("Set Status is: "+result);
					}
				}};
			snmp.send(pdu, target, null, listener);
			snmp.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	public String snmpGet(String host, String community, String strOID) {
		String strResponse="";
		ResponseEvent response;
		Snmp snmp;
		try {
			OctetString community1 = new OctetString(community);
			host= host+"/"+"161";
			Address tHost = new UdpAddress(host);
			TransportMapping transport = new DefaultUdpTransportMapping();
			transport.listen();
			CommunityTarget comtarget = new CommunityTarget();
			comtarget.setCommunity(community1);
			comtarget.setVersion(SnmpConstants.version2c);
			comtarget.setAddress(tHost);
			comtarget.setRetries(2);
			comtarget.setTimeout(5000);
			PDU pdu = new PDU();
			pdu.add(new VariableBinding(new OID(strOID)));
			//pdu.add(new VariableBinding(new OID(strOID1)));
			pdu.setType(PDU.GETBULK);
			pdu.setMaxRepetitions(3);
			pdu.setNonRepeaters(0);
			snmp = new Snmp(transport);
			response = snmp.send(pdu,comtarget);
			if(response != null) {
				if(response.getResponse().getErrorStatusText().equalsIgnoreCase("Success")) {
					PDU pduresponse=response.getResponse();

					System.out.println(pduresponse.getVariableBindings().toString());
					Vector<? extends VariableBinding> vbs = pduresponse.getVariableBindings();
					for (VariableBinding vb : vbs) {

						//System.out.println(vb.getVariable().getSyntaxString());
					//System.out.println(vb + " ," + vb.getVariable().getSyntaxString());
					}

						//System.out.println(strResponse);

				}
			} else {
				System.out.println("Looks like a TimeOut occured ");
			}
			snmp.close();
		} catch(Exception e) {
			e.printStackTrace();
		}
		//System.out.println("Response="+strResponse);
		return strResponse;
	}
}