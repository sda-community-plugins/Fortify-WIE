/**
 * Helper class for interacting with the WebInspect Enterprise API
 */
package com.serena.air.plugin.wie

import com.google.gson.annotations.JsonAdapter
import com.serena.air.StepFailedException
import com.serena.air.http.HttpBaseClient
import com.serena.air.http.HttpResponse
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovy.json.JsonBuilder
import org.apache.http.HttpEntity
import org.apache.http.HttpHeaders
import org.apache.http.NameValuePair
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpPost
import org.apache.http.client.methods.HttpPut
import org.apache.http.client.utils.URIBuilder
import org.apache.http.HttpStatus
import org.apache.http.conn.HttpHostConnectException
import org.apache.http.entity.ContentType
import org.apache.http.entity.StringEntity
import org.apache.http.message.BasicNameValuePair
import org.apache.http.entity.mime.MultipartEntityBuilder
import org.apache.log4j.Logger

import java.nio.charset.StandardCharsets
import java.nio.file.Files

class WIEHelper extends HttpBaseClient {
    private static final Logger logger = Logger.getLogger(WIEHelper.class)

    // enumerations for uploading temporary files
    public static final int WORKFLOW_MACRO = 0
    public static final int LOGIN_MACRO = 1
    public static final int URL_LIST = 2
    public static final int WEBFORM_VALUES = 3
    public static final int WSDL_FILE = 4
    public static final int SCAN_SETTINGS_XML = 5

    // random boundary for file upload
    protected static final String BOUNDARY = "vV9olNqRj00PC4OIlM7";

    // token requested and saved for authentication
    def fortifyToken

    boolean debug = false

    WIEHelper(String serverUrl, String username, String password) {
        super(serverUrl, username, password)
    }

    @Override
    protected String getFullServerUrl(String serverUrl) {
         return serverUrl + "/REST"
    }

    /**
     * Retrieve Fortify Authentication Token
     */
    def login() {
        def jsonBody = JsonOutput.toJson([username: username, password: password])
        URIBuilder builder = getUriBuilder("/api/v1/auth")
        HttpPost method = new HttpPost(builder.build())
        HttpEntity body = new StringEntity(jsonBody.toString(), ContentType.APPLICATION_JSON)
        method.entity = body
        HttpResponse response = execMethod(method)
        def json = new JsonSlurper().parseText(response.body)
        if (json?.responseCode != HttpStatus.SC_OK) {
            throw new StepFailedException("Unable to login: " + json?.Message)
        }
        if (!json.keySet().contains("data")) {
            throw new StepFailedException("Could not retrieve authorization token:\n" + json?.Message)
        } else {
            fortifyToken = json?.data
        }
    }

    /*
     * Get status of WIE as parseable JSON response
     */
    def getStatusJson() {
        HttpResponse response = execGet("/api/v1/status", null)
        checkStatusCode(response.code)
        if (response.code != HttpStatus.SC_OK) {
            return null
        }
        return new JsonSlurper().parseText(response.body)
    }

    // Security Groups

    /*
     * Get all WIE security groups as parseable JSON response
     */
    def getSecurityGroupsJson() {
        HttpResponse response = execGet("/api/v1/securityGroups", null)
        checkStatusCode(response.code)
        if (response.code != HttpStatus.SC_OK) {
            throw new StepFailedException("Unable to retrieve security groups")
        }
        return new JsonSlurper().parseText(response.body)
    }

    /*
     * Get a specific WIE security group as parseable JSON response
     */
    def getSecurityGroupByIdJson(String groupId) {
        HttpResponse response = execGet("/api/v1/securityGroups/${groupId}", null)
        checkStatusCode(response.code)
        if (response.code != HttpStatus.SC_OK) {
            throw new StepFailedException("Unable to retrieve security group \"${groupId}\"")
        }
        return new JsonSlurper().parseText(response.body)
    }

    /*
     * Get a specific security group (by name) as parseable JSON response
     */
    def getSecurityGroupByNameJson(String groupName) {
        def group = null
        def groupsJson = this.getSecurityGroupsJson()
        groupsJson?.data.each {
            if (new String(it?.name).equals(groupName)) { group = it }
        }
        if (group) { return group } else {
            throw new StepFailedException("Could not find security group \"${groupName}\"")
        }
    }

    // Policies

    /*
     * Get all WIE policies as parseable JSON response
     */
    def getPoliciesJson() {
        HttpResponse response = execGet("/api/v1/policies", null)
        checkStatusCode(response.code)
        if (response.code != HttpStatus.SC_OK) {
            throw new StepFailedException("Unable to retrieve policies")
        }
        return new JsonSlurper().parseText(response.body)
    }

    /*
    * Get a specific WIE policy as parseable JSON response
    */
    def getPolicyByIdJson(String policyId) {
        HttpResponse response = execGet("/api/v1/policies/${policyId}", null)
        checkStatusCode(response.code)
        if (response.code != HttpStatus.SC_OK) {
            throw new StepFailedException("Unable to retrieve policy \"${policyId}\"")
        }
        return new JsonSlurper().parseText(response.body)
    }

    /*
     * Get a specific policy (by name) as parseable JSON response
     */
    def getPolicyByNameJson(String policyName) {
        def policy = null
        def policiesJson = this.getPoliciesJson()
        policiesJson?.data.each {
            if (new String(it?.name).equals(policyName)) { policy = it }
        }
        if (policy) { return policy } else {
            throw new StepFailedException("Could not find policy \"${policyName}\"")
        }
    }

    // Projects (Applications)

    /*
    * Get all WIE projects (applications) as parseable JSON response
    */
    def getProjectsJson() {
        HttpResponse response = execGet("/api/v1/projects", null)
        checkStatusCode(response.code)
        if (response.code != HttpStatus.SC_OK) {
            throw new StepFailedException("Unable to retrieve projects")
        }
        return new JsonSlurper().parseText(response.body)
    }

    /*
     * Get a specific WIE project as parseable JSON response
     */
    def getProjectByIdJson(String projectId) {
        HttpResponse response = execGet("/api/v1/projects/${projectId}", null)
        checkStatusCode(response.code)
        if (response.code != HttpStatus.SC_OK) {
            throw new StepFailedException("Unable to retrieve project \"${projectId}\"")
        }
        return new JsonSlurper().parseText(response.body)
    }

    /*
     * Get a specific project (by name) as parseable JSON response
     */
    def getProjectByNameJson(String projectName) {
        def project = null
        def projectsJson = this.getProjectsJson()
        projectsJson?.data.each {
            if (new String(it?.name).equals(projectName)) { project = it }
        }
        if (project) { return project } else {
            throw new StepFailedException("Could not find project \"${projectName}\"")
        }
    }

    // Project (Application) Versions

    /*
    * Get all WIE project (application) versions as parseable JSON response
    */
    def getProjectVersionsJson() {
        HttpResponse response = execGet("/api/v2/projectVersions", null)
        checkStatusCode(response.code)
        if (response.code != HttpStatus.SC_OK) {
            throw new StepFailedException("Unable to retrieve project versions")
        }
        return new JsonSlurper().parseText(response.body)
    }

    /*
     * Get a specific WIE project version as parseable JSON response
     */
    def getProjectVersionByIdJson(String projectVersionId) {
        HttpResponse response = execGet("/api/v2/projectVersions/${projectVersionId}", null)
        checkStatusCode(response.code)
        if (response.code != HttpStatus.SC_OK) {
            throw new StepFailedException("Unable to retrieve project version \"${projectVersionId}\"")
        }
        return new JsonSlurper().parseText(response.body)
    }

    /*
     * Get a specific project (by name) as parseable JSON response
     */
    def getProjectVersionByNameJson(String projectVersionName) {
        def projectVersion = null
        def projectVersionsJson = this.getProjectVersionsJson()
        projectVersionsJson?.data.each {
            if (new String(it?.name).equals(projectVersionName)) { projectVersion = it }
        }
        if (projectVersion) { return projectVersion } else {
            throw new StepFailedException("Could not find project version \"${projectVersionName}\"")
        }
    }

    /*
 * Get a specific project (by name) as parseable JSON response
 */
    def getProjectVersionByProjectAndNameJson(String projectName, String projectVersionName) {
        def projectVersion = null
        def projectVersionsJson = this.getProjectVersionsJson()
        projectVersionsJson?.data.each {
            if (new String(it?.project.name).equals(projectName)) {
                if (new String(it?.name).equals(projectVersionName)) {
                    projectVersion = it
                }
            }
        }
        if (projectVersion) { return projectVersion } else {
            throw new StepFailedException("Could not find project version \"${projectVersionName}\" in project \"${projectName}\"")
        }
    }

    // Sensors

    /*
     * Get all WIE sensors as parseable JSON response
     */
    def getSensorsJson() {
        HttpResponse response = execGet("/api/v1/sensors", null)
        checkStatusCode(response.code)
        if (response.code != HttpStatus.SC_OK) {
            throw new StepFailedException("Unable to retrieve sensors")
        }
        return new JsonSlurper().parseText(response.body)
    }

    /*
     * Get a specific WIE sensor as parseable JSON response
     */
    def getSensorByIdJson(String sensorId) {
        HttpResponse response = execGet("/api/v1/sensors/${sensorId}", null)
        checkStatusCode(response.code)
        if (response.code != HttpStatus.SC_OK) {
            throw new StepFailedException("Unable to retrieve sensor \"${sensorId}\"")
        }
        return new JsonSlurper().parseText(response.body)
    }

    /*
     * Get a specific sensor (by name) as parseable JSON response
     */
    def getSensorByNameJson(String sensorName) {
        def sensor = null
        def sensorsJson = this.getSensorsJson()
        sensorsJson?.data.each {
            if (new String(it?.name).equals(sensorName)) { sensor = it }
        }
        if (sensor) { return sensor } else {
            throw new StepFailedException("Could not find sensor \"${sensorName}\"")
        }
    }

    // Scans

    /*
     * Create a new simple scan from a URL
     */
    def createScanFromUrl(String scanName, String scanSiteId, String scanSensorId, String scanUrl, int scanPriority) {
        def url = "/api/v2/scans"
        debug("POST Request URL: " + getFullServerUrl() + url)

        List<String> scanUrls = new ArrayList<String>()
        scanUrls.add(scanUrl)
        JsonBuilder projectVersionJson = new JsonBuilder()
        projectVersionJson {
            siteId scanSiteId
        }
        JsonBuilder overridesJson = new JsonBuilder()
        overridesJson {
            priority scanPriority
            if (scanSensorId) sensorId scanSensorId
            if (scanPolicyId) policyId scanPolicyId
            startMethod "url"
            startUrls scanUrls
        }
        JsonBuilder scanJson = new JsonBuilder()
        scanJson {
            name scanName
            priority scanPriority
            if (scanSensorId) {
                sensorId scanSensorId
            }
            projectVersion projectVersionJson.content
            overrides overridesJson.content
        }
        debug("Request Payload: " + scanJson.toString())

        HttpResponse response = execPost(url, scanJson)
        checkStatusCode(response.code)

        def json = new JsonSlurper().parseText(response.body)
        debug("Response Body: " + json.toString())
        if (json?.responseCode != 201) {
            throw new StepFailedException("Unable to start scan: " + response.toString())
        }
        def scanId = json?.data?.id
        if (scanId == null || scanId.length() == 0) {
            throw new StepFailedException("Could not retrieve scan id")
        }
        return scanId
    }

    /*
     * Create a new simple scan from a template
     */
    def createScanFromTemplate(String scanName, String scanSiteId, String scanSensorId, String scanTemplId, String scanPolicyId, int scanPriority) {
        def url = "/api/v2/scans"
        debug("POST Request URL: " + getFullServerUrl() + url)

        JsonBuilder projectVersionJson = new JsonBuilder()
        projectVersionJson {
            siteId scanSiteId
        }
        JsonBuilder overridesJson = new JsonBuilder()
        overridesJson {
            priority scanPriority
            if (scanSensorId) sensorId scanSensorId
            if (scanPolicyId) policyId scanPolicyId
        }
        JsonBuilder scanJson = new JsonBuilder()
        scanJson {
            name scanName
            projectVersion projectVersionJson.content
            scanTemplateId scanTemplId
            overrides overridesJson.content
        }
        debug("Request Payload: " + scanJson.toString())

        HttpResponse response = execPost(url, scanJson)
        checkStatusCode(response.code)

        def json = new JsonSlurper().parseText(response.body)
        debug("Response Body: " + json.toString())
        if (json?.responseCode != 201) {
            throw new StepFailedException("Unable to start scan: " + response.toString())
        }
        def scanId = json?.data?.id
        if (scanId == null || scanId.length() == 0) {
            throw new StepFailedException("Could not retrieve scan id")
        }
        return scanId
    }

    /*
     * Create a new simple scan from an uploaded settings file
     */
    def createScanFromSettingsFile(String scanName, String scanSiteId, String scanSensorId, String scanSettingsFileId, String scanPolicyId, int scanPriority) {
        def url = "/api/v2/scans"
        debug("POST Request URL: " + getFullServerUrl() + url)

        JsonBuilder projectVersionJson = new JsonBuilder()
        projectVersionJson {
            siteId scanSiteId
        }
        JsonBuilder overridesJson = new JsonBuilder()
        overridesJson {
            priority scanPriority
            if (scanSensorId) sensorId scanSensorId
            if (scanPolicyId) policyId scanPolicyId
        }
        JsonBuilder scanJson = new JsonBuilder()
        scanJson {
            name scanName
            projectVersion projectVersionJson.content
            fileID scanSettingsFileId
            overrides overridesJson.content
        }
        debug("Request Payload: " + scanJson.toString())

        HttpResponse response = execPost(url, scanJson)
        checkStatusCode(response.code)

        def json = new JsonSlurper().parseText(response.body)
        debug("Response Body: " + json.toString())
        if (json?.responseCode != 201) {
            throw new StepFailedException("Unable to start scan: " + response.toString())
        }
        def scanId = json?.data?.id
        if (scanId == null || scanId.length() == 0) {
            throw new StepFailedException("Could not retrieve scan id")
        }
        return scanId
    }

    /*
     * Get the status of a scan
     */
    def scanStatus(String scanId) {
        def url = "/api/v2/scans/${scanId}"
        debug("GET Request URL: " + getFullServerUrl() + url)

        HttpResponse response = execGet(url, null)
        checkStatusCode(response.code)

        def json = new JsonSlurper().parseText(response.body)
        debug("Response Body: " + json.toString())
        if (json?.responseCode != HttpStatus.SC_OK) {
            throw new StepFailedException("Unable to retrieve scan status")
        }
        def scanStateText = json?.data?.scanStateText
        if (scanStateText == null || scanStateText.length() == 0) {
            throw new StepFailedException("Could not retrieve scan state")
        }
        return scanStateText
    }

    /*
     * Get the results of a completed scan as parseable JSON response
     */
    def scanResultsJson(String scanId) {
        def url = "/api/v2/scans/${scanId}"
        debug("GET Request URL: " + getFullServerUrl() + url)

        HttpResponse response = execGet(url, null)
        checkStatusCode(response.code)

        def json = new JsonSlurper().parseText(response.body)
        debug("Response Body: " + json.toString())
        if (json?.responseCode != HttpStatus.SC_OK) {
            throw new StepFailedException("Unable to retrieve scan status")
        }
        return json
    }


    // TempFiles

    /*
     * Upload a file into WIE
     */
    def uploadFile(String theFileName, File theFile, int theType) {
        if (!theFile.exists()) throw new StepFailedException("Cannot find file: " + theFile.canonicalFile)

        // create temporary file container
        def url = "/api/v2/tempFile"
        debug("POST Request URL: " + getFullServerUrl() + url)

        JsonBuilder tempFileJson = new JsonBuilder()
        tempFileJson {
            filename theFileName
            fileType theType
        }
        debug("Request Payload: " + tempFileJson.toString())

        HttpResponse response = execPost(url, tempFileJson)
        checkStatusCode(response.code)

        def json = new JsonSlurper().parseText(response.body)
        debug("Response Body: " + json.toString())
        if (json?.responseCode != 201) {
            throw new StepFailedException("Unable to create temporary file: " + response.toString())
        }
        def tempFileId = json?.data?.fileID
        if (tempFileId == null || tempFileId.length() == 0) {
            throw new StepFailedException("Could not retrieve temporary file id: " + response.toString())
        }

        // upload contents into temporary file container
        url = "/api/v2/tempFile/${tempFileId}/fileData"
        debug("POST Request URL: " + getFullServerUrl() + url)

        byte[] fileContent = Files.readAllBytes(theFile.toPath());
        URIBuilder uriBuilder = getUriBuilder(url)
        HttpPost method = new HttpPost(uriBuilder.build())

        HttpEntity body = MultipartEntityBuilder.create()
                .setCharset(StandardCharsets.UTF_8)
                .setBoundary(BOUNDARY)
                .addBinaryBody(
                        "data",
                        fileContent,
                        ContentType.create("text/xml", StandardCharsets.UTF_8),
                        theFileName
                )
                .build()
        method.entity = body
        method.addHeader(HttpHeaders.AUTHORIZATION, fortifyToken)
        response = execMethod(method)
        json = new JsonSlurper().parseText(response.body)
        debug("Response Body: " + json.toString())

        if (json?.responseCode != HttpStatus.SC_OK) {
            throw new StepFailedException("Unable to upload file: " + response.toString())
        }

        return tempFileId

    }

    //
    // private methods
    //

    private static def fNull(def value) {
        return (value == null ? "0" : value)
    }

    private debug(def message) {
        if (debug) println "{DEBUG} ${message}"
    }

    private error(def message) {
        println "[ERROR] ${message}"
    }

    private info(def message) {
        println "[INFO] ${message}"
    }


    //
    // HTTP methods
    //

    private HttpResponse execMethod(def method) {
        try {
            return exec(method)
        } catch (UnknownHostException e) {
            throw new StepFailedException("Unknown host: ${e.message}")
        } catch (HttpHostConnectException ignore) {
            throw new StepFailedException('Connection refused!')
        }
    }

    private HttpResponse execGet(def url, List<NameValuePair> params) {
        URIBuilder builder = getUriBuilder(url.toString())
        debug("WIE GET: ${url}")
        if (params) {
            debug("PARAMETERS: " + params.toString())
            builder.addParameters(params)
        }
        HttpGet method = new HttpGet(builder.build())
        method.addHeader(HttpHeaders.AUTHORIZATION, fortifyToken)
        return execMethod(method)
    }

    private HttpResponse execPost(def url, def json) {
        URIBuilder builder = getUriBuilder(url.toString())
        debug("WIE POST: ${url}")
        HttpPost method = new HttpPost(builder.build())
        method.addHeader(HttpHeaders.AUTHORIZATION, fortifyToken)
        if (json) {
            HttpEntity body = new StringEntity(json.toString(), ContentType.APPLICATION_JSON)
            method.entity = body
        }
        return execMethod(method)
    }

    private HttpResponse execPut(def url, def json) {
        URIBuilder builder = getUriBuilder(url.toString())
        debug("WIE PUT: ${url}")
        HttpPut method = new HttpGet(builder.build())
        method.addHeader(HttpHeaders.AUTHORIZATION, fortifyToken)
        if (json) {
            HttpEntity body = new StringEntity(json.toString(), ContentType.APPLICATION_JSON)
            method.entity = body
        }
        return execMethod(method)
    }

}
