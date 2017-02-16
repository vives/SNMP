import com.sun.deploy.util.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.snmp4j.PDU;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.VariableBinding;

import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * Created by root on 2/7/17.
 */
public class jsonParser {
    private void addPDU(String oidValues) {
        List<String> oidsList = null;
        String Oid="";
            try {
                JSONObject object = new JSONObject(oidValues);
                Iterator keys = object.keys();
                while (keys.hasNext()) {
                    String oid = (String) keys.next();
                    String value = object.getString(oid);
                    oidsList = Arrays.asList(value.split(","));
                    String UpdateValue=oidsList.get(0);
                    String Type=oidsList.get(1);
                    System.out.println("OID " + oid + ", UpdateValue " + UpdateValue + ", Type " + Type);
                }
            }catch (JSONException e){
                e.printStackTrace();
            }
    }
    public String createPDU(String oids) throws IOException {
        List<String> oidsList = null;
        String Oid="";
            oidsList = Arrays.asList(oids.split(","));
        if (oidsList != null) {
            for (String oid : oidsList) {
               Oid = oids;
            }
        }
       return  Oid;
    }
    public static void main(String args[]){

        String jsonEm= "{\".1.3.6.1.2.1.1.1.0\":\"XYZ,String\",\".1.3.6.1.2.1.1.1.1\":\"1,Integer\",\".1.3.6.1.2.1.3.1.1\":\"WSO2,Octa\"}";
        jsonParser jp=new jsonParser();
        jp.addPDU(jsonEm);

    }
}
