/*
* Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
* WSO2 Inc. licenses this file to you under the Apache License,
* Version 2.0 (the "License"); you may not use this file except
* in compliance with the License.
* You may obtain a copy of the License at
*
*http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied. See the License for the
* specific language governing permissions and limitations
* under the License.
*/

package org.wso2.carbon.connector.integration.test.snmp;

import org.json.JSONException;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.connector.integration.test.base.ConnectorIntegrationTestBase;
import org.wso2.connector.integration.test.base.RestResponse;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Integration test class for file connector
 */
public class SnmpConnectorIntegrationTest extends ConnectorIntegrationTestBase {

    private final Map<String, String> esbRequestHeadersMap = new HashMap<String, String>();

    /**
     * Set up the environment.
     */
    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        init("snmp-connector-1.0.0");
        esbRequestHeadersMap.put("Accept-Charset", "UTF-8");
        esbRequestHeadersMap.put("Content-Type", "application/json");
        esbRequestHeadersMap.put("Accept", "application/json");
    }

    /**
     * Positive test case for snmpSet method with mandatory parameters.
     *
     * @throws JSONException
     * @throws IOException
     */
//    @Test(groups = {"wso2.esb"}, description = "snmp {snmpSet} integration test with mandatory parameters.")
//    public void testSnmpSetWithMandatoryParameters() throws IOException, JSONException {
//        esbRequestHeadersMap.put("Action", "urn:snmpSet");
//        RestResponse<JSONObject> esbRestResponse =
//                sendJsonRestRequest(proxyUrl, "POST", esbRequestHeadersMap, "snmpSet_mandatory.json");
//        Assert.assertEquals(esbRestResponse.getHttpStatusCode(), 200);
//        Assert.assertEquals(true, esbRestResponse.getBody().toString().contains("OIDs successfully set."));
//    }
//
//    /**
//     * Negative test case for snmpSet method.
//     *
//     * @throws JSONException
//     * @throws IOException
//     */
//    @Test(groups = { "wso2.esb" }, description = "snmp {snmpSet} integration test negative case.")
//    public void testSnmpSetWithNegativeCase() throws IOException, JSONException {
//        esbRequestHeadersMap.put("Action", "urn:snmpSet");
//        RestResponse<JSONObject> esbRestResponse =
//                sendJsonRestRequest(proxyUrl, "POST", esbRequestHeadersMap, "snmpSet_negative.json");
//        Assert.assertEquals(esbRestResponse.getHttpStatusCode(), 202);
//    }

    /**
     * Positive test case for snmpGet method with mandatory parameters.
     *
     * @throws JSONException
     * @throws IOException
     */
    @Test(groups = {"wso2.esb"}, description = "snmp {snmpGet} integration test with mandatory parameters.")
    public void testSnmpGetWithMandatoryParameters() throws IOException, JSONException {
        esbRequestHeadersMap.put("Action", "urn:snmpGet");
        RestResponse<JSONObject> esbRestResponse =
                sendJsonRestRequest(proxyUrl, "POST", esbRequestHeadersMap, "snmpGet_mandatory.json");
        Assert.assertEquals(esbRestResponse.getHttpStatusCode(), 200);
        Assert.assertEquals(true, esbRestResponse.getBody().toString().contains("Response"));
    }

        /**
     * Negative test case for snmpGet method.
     *
     * @throws JSONException
     * @throws IOException
     */
    @Test(groups = { "wso2.esb" }, description = "snmp {snmpGet} integration test negative case.")
    public void testSnmpGetWithNegativeCase() throws IOException, JSONException {
        esbRequestHeadersMap.put("Action", "urn:snmpGet");
        RestResponse<JSONObject> esbRestResponse =
                sendJsonRestRequest(proxyUrl, "POST", esbRequestHeadersMap, "snmpGet_negative.json");
        Assert.assertEquals(esbRestResponse.getHttpStatusCode(), 202);
    }
}