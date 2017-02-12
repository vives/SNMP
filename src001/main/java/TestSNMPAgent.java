import org.snmp4j.smi.OID;

import java.io.IOException;

public class TestSNMPAgent {

	static final OID sysDescr = new OID(".1.3.6.1.2.1.1.1.0");
	static final OID sysDescr1 = new OID(".1.3.6.1.2.1.1.5.0");

	public static void main(String[] args) throws IOException {

		TestSNMPAgent client = new TestSNMPAgent("udp:127.0.0.1/162");

		client.init(sysDescr,"xxxxxx");

	}

	SNMPAgent agent = null;

	/**
	 * 20
	 * This is the client which we have created earlier
	 * 21
	 */

	SNMPManager client = null;

	String address = null;

	/**
	 * 27
	 * Constructor
	 * 28
	 * <p>
	 * 29
	 *
	 * @param add 30
	 */

	public TestSNMPAgent(String add) {

		address = add;

	}

	public void init(OID oid,Object val) throws IOException {

		agent = new SNMPAgent("0.0.0.0/2001");

		agent.start();

		// Since BaseAgent registers some MIBs by default we need to unregister

		// one before we register our own sysDescr. Normally you would

		// override that method and register the MIBs that you need

		agent.unregisterManagedObject(agent.getSnmpv2MIB());

		// Register a system description, use one from you product environment

		// to test with

		//agent.registerManagedObject(MOCreator.createReadOnly(sysDescr, "This Description is set By ShivaSoft"));
		agent.registerManagedObject(MOCreator.createWritable(oid, val));

		// Setup the client to use our newly started agent

		client = new SNMPManager("udp:127.0.0.1/2001");

		client.start();

		// Get back Value which is set

	   System.out.println(client.getAsString(sysDescr));

	}
}
