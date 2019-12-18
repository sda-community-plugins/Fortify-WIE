package test.com.serena.air.plugin.wie

import com.serena.air.plugin.wie.WIEHelper

def wieServerUrl = System.getenv("WIE_SERVER_URL")
def wieUser = System.getenv("WIE_USERNAME")
def wiePassword = System.getenv("WIE_PASSWORD")

WIEHelper wieClient = new WIEHelper(wieServerUrl, wieUser, wiePassword)
wieClient.setSSL()
wieClient.setDebug(true)
wieClient.login()

println wieClient.scanResultsJson("3e86abf4-a9cd-4e58-9349-0f900d113588")
System.exit(0)
//println wieClient.getStatusJson()
//println wieClient.getSecurityGroupsJson()
//println wieClient.getSecurityGroupByIdJson("8dc3fa89-9be0-4a8c-98d7-e88c836a17b5")
//println wieClient.getSecurityGroupByNameJson("Default Group")
//println wieClient.getPoliciesJson()
//println wieClient.getPolicyByIdJson("08cd4862-6334-4b0e-abf5-cb7685d0cde7")
//println wieClient.getPolicyIdByName("Standard")
//println wieClient.getPolicyByNameJson("All Checks")
//println wieClient.getProjectsJson()
//println wieClient.getProjectByIdJson("3")
//println wieClient.getProjectByNameJson("Java Web App")
//println wieClient.getProjectVersionsJson()
//println wieClient.getProjectVersionByIdJson("10003")
//println wieClient.getProjectVersionByNameJson("1.0")
//println wieClient.getProjectVersionSiteId("Java Web App", "1.0")
//println wieClient.getProjectVersionByProjectAndNameJson("Java Web App", "2.0")
//println wieClient.getSensorsJson()
//println wieClient.getSensorByIdJson("a03836b2-a2cd-4d7a-b443-557651a0fdd7")
//println wieClient.getSensorByNameJson("wins2016srg-WebInspect")
//println wieClient.getSensorIdByName("wins2016srg-WebInspect")
//println wieClient.getScanTemplatesJson()
//println wieClient.getScanTemplateByIdJson("c88e5487-926f-4729-bfa0-6265708dce3b")
//println wieClient.getScanTemplateNameById("c88e5487-926f-4729-bfa0-6265708dce3b")

def scanSiteId = wieClient.getProjectVersionSiteId("Java Web App", "1.0")
def scanSensorId = wieClient.getSensorIdByName("wins2016srg-WebInspect")
def scanUrl = "http://wins2016srg:8091/java-web-app/"
def scanTemplateId = "c88e5487-926f-4729-bfa0-6265708dce3b"
def wieConsoleUrl = "${wieServerUrl}/WebConsole/ApplicationDetails.aspx?SiteID=${scanSiteId}"
def scanPolicyId = wieClient.getPolicyIdByName("Standard")
def scanPriority = 2

String scanId = wieClient.createScanFromUrl("Test Scan from URL", scanSiteId, scanPolicyId, scanSensorId, scanUrl, scanPriority)
//String scanId = wieClient.createScanFromTemplate("Test Scan from Template", scanSiteId, scanPolicyId, scanSensorId, scanTemplateId, scanPriority)
//def scanSettingsFileId = wieClient.uploadFile("wisettings.xml", new File("C:\\Temp\\wisettings.xml"), wieClient.SCAN_SETTINGS_XML)
//println "Uploaded wiSettings as id: ${scanSettingsFileId}"
//String scanId = wieClient.createScanFromSettingsFile("Test Scan from Settings File", scanSiteId, scanPolicyId, scanSensorId, scanSettingsFileId, scanPriority)
println "Scan id is ${scanId}"
String scanStatus = wieClient.scanStatus(scanId)
while (scanStatus.equals("Pending") || scanStatus.equals("Starting") || scanStatus.equals("Running") || scanStatus.equals("Importing")) {
    println "Scan status: ${scanStatus} ... sleeping..."
    sleep(10000)
    scanStatus = wieClient.scanStatus(scanId)
}
def scanResults = wieClient.scanResultsJson(scanId)?.data
println "Scan is: ${scanResults?.scanStateText}"

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
sb << "<tr> <td> Priority </td> <td> ${scanResults?.sensor?.name} </td> </tr>"
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

String name = "scanResult"
File f = new File(name + ".html")
BufferedWriter bw = new BufferedWriter(new FileWriter(f))
bw.write(sb.toString())
bw.close()



