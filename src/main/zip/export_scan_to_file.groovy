// --------------------------------------------------------------------------------
// Export a scan to file as FPR, Scan or XML format
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
String scanDirName = props.notNull("scanDirName")
String scanFileName = props.notNull("scanFileName")
String scanFormat = props.notNull("scanFormat")
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
println "Scan Directory Name: ${scanDirName}"
println "Scan File Name: ${scanFileName}"
println "Scan Output Format: ${scanFormat}"
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

    wieClient.exportScanResults(scanId, scanFormat as WIEExportType, scanDirName, scanFileName)

    println "Successfully created export file: " + scanDirName + File.separatorChar + scanFileName

} catch (StepFailedException e) {
    println "ERROR: ${e.message}"
    System.exit 1
}

//
// An exit with a zero value means the plugin step execution will be deemed successful.
//
System.exit(0)
