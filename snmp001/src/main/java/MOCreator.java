import org.snmp4j.agent.mo.MOAccessImpl;

import org.snmp4j.agent.mo.MOScalar;

import org.snmp4j.smi.OID;

import org.snmp4j.smi.OctetString;

import org.snmp4j.smi.Variable;

/**
 * 10
 * This class creates and returns ManagedObjects
 * 11
 *
 * @author Shiva
 *         12
 *         <p>
 *         13
 */

public class MOCreator {

	public static MOScalar createReadOnly(OID oid, Object value) {

		return new MOScalar(oid,

		                    MOAccessImpl.ACCESS_READ_ONLY,

		                    getVariable(value));

	}
	public static MOScalar createWritable(OID oid,Object value ){

		return new MOScalar(oid,

		                    MOAccessImpl.ACCESS_READ_WRITE,

		                    getVariable(value));

	}

	private static Variable getVariable(Object value) {

		if (value instanceof String) {

			return new OctetString((String) value);

		}

		throw new IllegalArgumentException("Unmanaged Type: " + value.getClass());

	}

}

