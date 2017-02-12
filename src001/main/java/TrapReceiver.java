import org.snmp4j.CommandResponder;
import org.snmp4j.CommandResponderEvent;
import org.snmp4j.CommunityTarget;
import org.snmp4j.MessageDispatcher;
import org.snmp4j.MessageDispatcherImpl;
import org.snmp4j.MessageException;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.log.LogFactory;
import org.snmp4j.mp.MPv1;
import org.snmp4j.mp.MPv2c;
import org.snmp4j.mp.StateReference;
import org.snmp4j.mp.StatusInformation;
import org.snmp4j.security.Priv3DES;
import org.snmp4j.security.SecurityProtocols;
import org.snmp4j.smi.*;
import org.snmp4j.tools.console.SnmpRequest;

import org.snmp4j.transport.AbstractTransportMapping;
import org.snmp4j.transport.DefaultTcpTransportMapping;
import org.snmp4j.transport.DefaultUdpTransportMapping;
import org.snmp4j.util.MultiThreadedMessageDispatcher;
import org.snmp4j.util.ThreadPool;

import java.io.IOException;

public class TrapReceiver implements CommandResponder {
	public TrapReceiver()
	{
	}

	public static void main(String[] args)
	{
		TrapReceiver snmp4jTrapReceiver = new TrapReceiver();
		try
		{
			snmp4jTrapReceiver.listen(new UdpAddress("127.0.0.1/162"));
		}
		catch (IOException e)
		{
			System.err.println("Error in Listening for Trap");
			System.err.println("Exception Message = " + e.getMessage());
		}
	}

	/**
	 * This method will listen for traps and response pdu's from SNMP agent.
	 */
	public synchronized void listen(TransportIpAddress address) throws IOException
	{
		AbstractTransportMapping transport;
		if (address instanceof TcpAddress)
		{
			transport = new DefaultTcpTransportMapping((TcpAddress) address);
		}
		else
		{
			transport = new DefaultUdpTransportMapping((UdpAddress) address);
		}

		ThreadPool threadPool = ThreadPool.create("DispatcherPool", 10);
		MessageDispatcher mtDispatcher = new MultiThreadedMessageDispatcher(threadPool, new MessageDispatcherImpl());

		// add message processing models
		mtDispatcher.addMessageProcessingModel(new MPv1());
		mtDispatcher.addMessageProcessingModel(new MPv2c());

		// add all security protocols
		SecurityProtocols.getInstance().addDefaultProtocols();
		SecurityProtocols.getInstance().addPrivacyProtocol(new Priv3DES());

		//Create Target
		CommunityTarget target = new CommunityTarget();
		target.setCommunity( new OctetString("public"));

		Snmp snmp = new Snmp(mtDispatcher, transport);
		snmp.addCommandResponder((CommandResponder) this);

		transport.listen();
		System.out.println("Listening on " + address);

		try
		{
			this.wait();
		}
		catch (InterruptedException ex)
		{
			Thread.currentThread().interrupt();
		}
	}

	/**
	 * This method will be called whenever a pdu is received on the given port specified in the listen() method
	 */
	public synchronized void processPdu(CommandResponderEvent cmdRespEvent)
	{
		/*SNMPAgent agent = null;
		SNMPManager client = null;
		client = new SNMPManager("udp:127.0.0.1/2001");*/
		/*try {
			client.start();
		} catch (IOException e) {
			e.printStackTrace();
		}*/
		//final OID sysDescr = new OID(".1.3.6.1.2.1.1.1.0");
		System.out.println("Received PDU...");
		PDU pdu = cmdRespEvent.getPDU();

		System.out.println(pdu);
		if (pdu != null)
		{
			System.out.println("Trap Type = " + pdu.getType());
			System.out.println("Variable Bindings = " + pdu.getVariableBindings());
			System.out.println("!!!!!!!!!!!! " + pdu.getVariableBindings().get(0).getOid() + "      " +  pdu.getVariableBindings().get(0).getVariable());

			int pduType = pdu.getType();

			if(pduType==PDU.SET){
				try {
					 final OID sysDescr = new OID(".1.3.6.1.2.1.1.1.0");
					/*agent = new SNMPAgent("0.0.0.0/2001");
					agent.start();
					agent.unregisterManagedObject(agent.getSnmpv2MIB());
					agent.registerManagedObject(MOCreator.createReadOnly(sysDescr, "This Description is set By ShivaSoft"));*/

					TestSNMPAgent client = new TestSNMPAgent("udp:127.0.0.1/162");

					//client.init(sysDescr,pdu.getVariableBindings().get(0).getVariable());
					//System.out.println("Size     " + pdu.getVariableBindings().size());

					for(int i=0;i<pdu.getVariableBindings().size();i++) {
						//String value = pdu.getVariableBindings().get(i).getVariable().toString();
						client.init(pdu.getVariableBindings().get(i).getOid(), pdu.getVariableBindings().get(i).getVariable().toString());
					}

					//System.out.println(client.getAsString(sysDescr));
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if(pduType==PDU.GET){
				try {
					TestSNMPAgent client = new TestSNMPAgent("udp:127.0.0.1/162");
					client.init(pdu.getVariableBindings().get(0).getOid(), "XYZ");

					/*SNMPManager manager = new SNMPManager("udp:127.0.0.1/2001");
					manager.start();
					System.out.println(manager.getAsString(pdu.getVariableBindings().get(0).getOid()));*/

				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if ((pduType != PDU.TRAP) && (pduType != PDU.V1TRAP) && (pduType != PDU.REPORT)
			    && (pduType != PDU.RESPONSE))
			{
				pdu.setErrorIndex(0);
				pdu.setErrorStatus(0);
				pdu.setType(PDU.RESPONSE);
				StatusInformation statusInformation = new StatusInformation();
				StateReference ref = cmdRespEvent.getStateReference();
				try
				{
					System.out.println(cmdRespEvent.getPDU());
					cmdRespEvent.getMessageDispatcher().returnResponsePdu(cmdRespEvent.getMessageProcessingModel(),
					                                                      cmdRespEvent.getSecurityModel(), cmdRespEvent.getSecurityName(), cmdRespEvent.getSecurityLevel(),
					                                                      pdu, cmdRespEvent.getMaxSizeResponsePDU(), ref, statusInformation);
				}
				catch (MessageException ex)
				{
					System.err.println("Error while sending response: " + ex.getMessage());
					LogFactory.getLogger(SnmpRequest.class).error(ex);
				}
			}
		}
	}
}
