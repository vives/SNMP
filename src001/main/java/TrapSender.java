import org.snmp4j.*;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.*;
import org.snmp4j.transport.DefaultUdpTransportMapping;

import java.util.Date;

public class TrapSender
{
	public static final String  community  = "public";

	//  Sending Trap for sysLocation of RFC1213
	public static final String  trapOid          = ".1.3.6.1.2.1.1.6";

	public static final String  ipAddress      = "127.0.0.1";

	public static final int     port      = 163;

	public TrapSender()
	{
	}

	public static void main(String[] args)
	{
		TrapSender snmp4JTrap = new TrapSender();

    /* Sending V1 Trap */
		//snmp4JTrap.sendSnmpV1Trap();

    /* Sending V2 Trap */
		snmp4JTrap.sendSnmpV2Trap();
	}

	/**
	 * This methods sends the V1 trap to the Localhost in port 163
	 */
	public void sendSnmpV1Trap()
	{
		try
		{
			//Create Transport Mapping
			TransportMapping transport = new DefaultUdpTransportMapping();
			transport.listen();

			//Create Target
			CommunityTarget comtarget = new CommunityTarget();
			comtarget.setCommunity(new OctetString(community));
			comtarget.setVersion(SnmpConstants.version1);
			comtarget.setAddress(new UdpAddress(ipAddress + "/" + port));
			comtarget.setRetries(2);
			comtarget.setTimeout(5000);

			//Create PDU for V1
			PDUv1 pdu = new PDUv1();
			pdu.setType(PDU.V1TRAP);
			pdu.setEnterprise(new OID(trapOid));
			pdu.setGenericTrap(PDUv1.ENTERPRISE_SPECIFIC);
			pdu.setSpecificTrap(1);
			pdu.setAgentAddress(new IpAddress(ipAddress));

			//Send the PDU
			Snmp snmp = new Snmp(transport);
			System.out.println("Sending V1 Trap to " + ipAddress + " on Port " + port);
			snmp.send(pdu, comtarget);
			snmp.close();
		}
		catch (Exception e)
		{
			System.err.println("Error in Sending V1 Trap to " + ipAddress + " on Port " + port);
			System.err.println("Exception Message = " + e.getMessage());
		}
	}


	/**
	 * This methods sends the V2 trap to the Localhost in port 163
	 */
	public void sendSnmpV2Trap()
	{
		try
		{
			//Create Transport Mapping
			TransportMapping transport = new DefaultUdpTransportMapping();
			transport.listen();

			//Create Target
			CommunityTarget comtarget = new CommunityTarget();
			comtarget.setCommunity(new OctetString(community));
			comtarget.setVersion(SnmpConstants.version2c);
			comtarget.setAddress(new UdpAddress(ipAddress + "/" + port));
			comtarget.setRetries(2);
			comtarget.setTimeout(5000);

			//Create PDU for V2
			PDU pdu = new PDU();

			// need to specify the system up time
			pdu.add(new VariableBinding(SnmpConstants.sysUpTime, new OctetString(new Date().toString())));
			pdu.add(new VariableBinding(SnmpConstants.snmpTrapOID, new OID(trapOid)));
			pdu.add(new VariableBinding(SnmpConstants.snmpTrapAddress, new IpAddress(ipAddress)));

			// variable binding for Enterprise Specific objects, Severity (should be defined in MIB file)
			pdu.add(new VariableBinding(new OID(trapOid), new OctetString("Major")));
			pdu.setType(PDU.NOTIFICATION);

			//Send the PDU
			Snmp snmp = new Snmp(transport);
			System.out.println("Sending V2 Trap to " + ipAddress + " on Port " + port);
			snmp.send(pdu, comtarget);
			snmp.close();
		}
		catch (Exception e)
		{
			System.err.println("Error in Sending V2 Trap to " + ipAddress + " on Port " + port);
			System.err.println("Exception Message = " + e.getMessage());
		}
	}
}