// --------------------------------------------------------------------------------
// Create a HTML report of a previously initiated scan
// --------------------------------------------------------------------------------

import com.serena.air.plugin.wie.*

import com.serena.air.StepFailedException
import com.serena.air.StepPropertiesHelper
import com.urbancode.air.AirPluginTool

//
// Create some variables that we can use throughout the plugin step.
// These are mainly for checking what operating system we are running on.
//
final def PLUGIN_HOME = System.getenv()['PLUGIN_HOME']
final String lineSep = System.getProperty('line.separator')
final String osName = System.getProperty('os.name').toLowerCase(Locale.US)
final String pathSep = System.getProperty('path.separator')
final boolean windows = (osName =~ /windows/)
final boolean vms = (osName =~ /vms/)
final boolean os9 = (osName =~ /mac/ && !osName.endsWith('x'))
final boolean unix = (pathSep == ':' && !vms && !os9)

//
// Initialise the plugin tool and retrieve all the properties that were sent to the step.
//
final def  apTool = new AirPluginTool(this.args[0], this.args[1])
final def  props  = new StepPropertiesHelper(apTool.getStepProperties(), true)

//
// Set a variable for each of the plugin steps's inputs.
// We can check whether a required input is supplied (the helper will fire an exception if not) and
// if it is of the required type.
//
File workDir = new File('.').canonicalFile
String wieServerUrl = props.notNull('serverUrl')
String wieUsername = props.notNull('username')
String wiePassword = props.notNull('password')
String scanId = props.notNull('scanId')
String reportFileName = props.optional("reportFileName", "scanResults.html")
boolean debugMode = props.optionalBoolean("debugMode", false)

println "----------------------------------------"
println "-- STEP INPUTS"
println "----------------------------------------"

//
// Print out each of the property values.
//
println "Working directory: ${workDir.canonicalPath}"
println "WIE Server URL: ${wieServerUrl}"
println "WIE Username: ${wieUsername}"
println "WIE Password: ${wiePassword.replaceAll(".", "*")}"
println "Scan Id: ${scanId}"
println "Report Filename: ${reportFileName}"
println "Debug mode value: ${debugMode}"
if (debugMode) { props.setDebugLoggingMode() }

println "----------------------------------------"
println "-- STEP EXECUTION"
println "----------------------------------------"

def scanStatus = "Pending"
def scanResults

try {
    WIEHelper wieClient = new WIEHelper(wieServerUrl, wieUsername, wiePassword)

    wieClient.setPreemptiveAuth()
    wieClient.setSSL()
    wieClient.setDebug(debugMode)
    wieClient.login()

    scanResults = wieClient.scanResultsJson(scanId)?.data
    scanStatus = scanResults?.scanStateText
    def scanSiteId = scanResults?.projectVersion?.siteId
    def wieConsoleUrl = "${wieServerUrl}/WebConsole/ApplicationDetails.aspx?SiteID=${scanSiteId}"

    println("Creating report for scan id: \"${scanResults?.name}\"")
    println("See: ${wieConsoleUrl}")

    long startTime = new Date().getTime();
    wieClient.debug("Start UNIX time is: ${startTime}")

    println "Scan is: ${scanStatus}"

    def sb = new StringBuilder()
    sb << """
<style type='text/css'>
    body { padding: 10px; 	background: #F5F5F5; font-family: Arial; }
    div { 
        background: #fff; box-shadow: 4px 4px 10px 0px rgba(119, 119, 119, 0.3);
        -moz-box-shadow: 4px 4px 10px 0px rgba(119, 119, 119, 0.3);
        -webkit-box-shadow: 4px 4px 10px 0px rgba(119, 119, 119, 0.3);
        margin-bottom: 20px; }
    a { color: #337ab7; text-decoration: underline; }
    a:hover { text-decoration: none; }
    span { font-weight: bold; }
    span.failed { color: #df1e1e; }
    span.success { color: #32ad12; }
    span.critical { color: #df1e1e; }
    span.high { color: #df1e1e; }
    span.medium { color: yellow; }
    span.low { color: blue; }
    table { border-collapse: collapse; width: 100%; color: #333; }
    table td, table th { border: 1px solid #ddd; }
    table th { padding: 10px; text-align: center; background: #eee; font-size: 16px; }
    table td { padding: 7px; font-size: 12px; } " +
    table tr th:first-child { max-width: 200px; min-width: 200px; }
    table tr td:first-child { width: 200px; text-align: right; font-weight: bold;}
    table tr td:first-child:after {content: ':' }
</style>
"""
    sb << "<div> <table> <tr> <th colspan='2'>Scan Name: ${scanResults?.name} </th> </tr>"
    sb << "<tr> <td> Scan Id </td> <td> ${scanId} </td> </tr>"
    sb << "<tr> <td> Scan URI </td> <td> ${scanResults?.settingsDetail?.startURI} </td> </tr>"
    sb << "<tr> <td> Scan Policy </td> <td> ${scanResults?.settingsDetail?.policyName} </td> </tr>"
    sb << "<tr> <td> Scan Type </td> <td> ${scanResults?.settingsDetail?.scanType} </td> </tr>"
    sb << "<tr> <td> Sensor </td> <td> ${scanResults?.sensor?.name} </td> </tr>"
    sb << "<tr> <td> Aplication </td> <td> ${scanResults?.project?.name} ${scanResults?.projectVersion?.name} </td> </tr>"
    sb << "<tr> <td> Priority </td> <td> ${scanResults?.priority} </td> </tr>"
    sb << "<tr> <td> Scan Start Time </td> <td> ${scanResults?.scanStartTime} </td> </tr>"
    sb << "<tr> <td> Scan End Time </td> <td> ${scanResults?.scanEndTime} </td> </tr>"
    sb << "<tr> <td> Status </td> <td> <span class=" + (("Complete".equalsIgnoreCase(scanResults?.scanStateText)) ? "success >" : "failed >") +  scanResults?.scanStateText + "</td> </tr>"
    sb << "<tr> <td> Published Status </td> <td> ${scanResults?.publishedStateText} </td> </tr>"
    sb << "<tr> <td> Critical Vulnerability Count </td> <td> <span class=\"failed\">" + wieClient.fNull(scanResults?.scanStatistics?.criticalCount) + " </span></td> </tr>"
    sb << "<tr> <td> High Vulnerability Count </td> <td> <span class=\"failed\"> " + wieClient.fNull(scanResults?.scanStatistics?.highCount) + " <span> </td> </tr>"
    sb << "<tr> <td> Medium Vulnerability Count </td> <td> " + wieClient.fNull(scanResults?.scanStatistics?.mediumCount) + " </td> </tr>"
    sb << "<tr> <td> Low Vulnerability Count </td> <td> " + wieClient.fNull(scanResults?.scanStatistics?.lowCount) + " </td> </tr>"
    sb << "<tr> <td> Information Count </td> <td> " + wieClient.fNull(scanResults?.scanStatistics?.infoCount) + " </td> </tr>"
    sb << "<tr> <td> Best Practice Count </td> <td> " + wieClient.fNull(scanResults?.scanStatistics?.bpCount) + " </td> </tr>"
    sb << "<tr> <td> Run URL" + "</td> <td> <a href='${wieConsoleUrl}'  target=_blank>View Configuration in Web Console</a> </td> </tr>"
    sb << "<br>"
    sb << "</table> </div>"
    sb << "</body>"
    sb << "</html>"

    File f = new File(reportFileName)
    BufferedWriter bw = new BufferedWriter(new FileWriter(f))
    bw.write(sb.toString())
    bw.close()

    long finishTime = new Date().getTime();
    wieClient.debug("Finish UNIX time is: ${finishTime}")

} catch (StepFailedException e) {
    println "ERROR: ${e.message}"
    System.exit 1
}

println "----------------------------------------"
println "-- STEP OUTPUTS"
println "----------------------------------------"
println("Setting \"scanId\" output property to \"${scanId}\"")
apTool.setOutputProperty("scanId", scanId)
if (scanStatus != null) {
    println("Setting \"reportFileName\" output property to \"${reportFileName}\"")
    apTool.setOutputProperty("reportFileName", reportFileName)
}
apTool.storeOutputProperties()

//
// An exit with a zero value means the plugin step execution will be deemed successful.
//
System.exit(0)
