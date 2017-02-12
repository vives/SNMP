import org.snmp4j.TransportMapping;
import org.snmp4j.agent.*;
import org.snmp4j.agent.mo.MOTableRow;
import org.snmp4j.agent.mo.snmp.*;
import org.snmp4j.agent.security.MutableVACM;
import org.snmp4j.mp.MPv3;
import org.snmp4j.security.SecurityLevel;
import org.snmp4j.security.SecurityModel;
import org.snmp4j.security.USM;
import org.snmp4j.smi.*;
import org.snmp4j.transport.TransportMappings;

import java.io.File;
import java.io.IOException;

public class SNMPAgent extends BaseAgent {
	private String address;

	public SNMPAgent(String address) throws IOException {
		super(new File("conf.agent"), new File("bootCounter.agent"),
		      new CommandProcessor(new OctetString(MPv3.createLocalEngineID())));
		this.address = address;
	}

	@Override
	protected void addCommunities(SnmpCommunityMIB communityMIB) {
		Variable[] com2sec =
				new Variable[] { new OctetString("public"), new OctetString("cpublic"), getAgent().getContextEngineID(),
				                 new OctetString("public"), new OctetString(), new Integer32(StorageType.nonVolatile),
				                 new Integer32(RowStatus.active) };
		MOTableRow row = communityMIB.getSnmpCommunityEntry()
		                             .createRow(new OctetString("public2public").toSubIndex(true), com2sec);
		communityMIB.getSnmpCommunityEntry().addRow((SnmpCommunityMIB.SnmpCommunityEntryRow) row);
	}

	@Override
	protected void addNotificationTargets(SnmpTargetMIB arg0, SnmpNotificationMIB arg1) {
	}

	@Override
	protected void addUsmUser(USM arg0) {
	}

	@Override
	protected void addViews(VacmMIB vacm) {
		vacm.addGroup(SecurityModel.SECURITY_MODEL_SNMPv2c, new OctetString("cpublic"), new OctetString("v1v2group"),
		              StorageType.nonVolatile);

		vacm.addAccess(new OctetString("v1v2group"), new OctetString("public"), SecurityModel.SECURITY_MODEL_ANY,
		               SecurityLevel.NOAUTH_NOPRIV, MutableVACM.VACM_MATCH_EXACT, new OctetString("fullReadView"),
		               new OctetString("fullWriteView"), new OctetString("fullNotifyView"), StorageType.nonVolatile);
		vacm.addViewTreeFamily(new OctetString("fullReadView"), new OID("1.3"), new OctetString(),
		                       VacmMIB.vacmViewIncluded, StorageType.nonVolatile);

	}

	@Override
	protected void unregisterManagedObjects() {
	}

	@Override
	protected void registerManagedObjects() {
	}

	protected void initTransportMappings() throws IOException {
		transportMappings = new TransportMapping[1];
		Address addr = GenericAddress.parse(address);
		TransportMapping tm = TransportMappings.getInstance().createTransportMapping(addr);
		transportMappings[0] = tm;
	}

	public void start() throws IOException {
		init();
		addShutdownHook();
		getServer().addContext(new OctetString("public"));
		finishInit();
		run();
		sendColdStartNotification();
	}

	public void registerManagedObject(ManagedObject mo) {
		try {
			server.register(mo, null);
		} catch (DuplicateRegistrationException ex) {
			throw new RuntimeException(ex);
		}
	}

	public void unregisterManagedObject(MOGroup moGroup) {
		moGroup.unregisterMOs(server, getContext(moGroup));
	}

}