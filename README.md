# Micro Focus Fortify WebInspect Enterprise plugin

The _Micro Focus Fortify WebInspect Enteprise_ plugin allows you to execute dynamic application security 
testing as part of a Deployment Automation workflow.

This plugin is a work in progress but it is intended to provide the following steps:

* [x] **Create Scan from URL** - Create a new simple scan from a URL
* [x] **Create Scan from Template** - Create a new simple scan from a template
* [x] **Create Scan from Settings File** - Create a new simple scan from an uploaded settings file
* [x] **Get Scan Status** - Gets the status of a previously initiated scan
* [x] **Create Scan Report** - Create a HTML report of a previously initiated scan.  

Note: this plugin is designed to be used when [WebInspect Enterprise](https://www.microfocus.com/en-us/products/webinspect-dynamic-analysis-dast/overview) 
has been integrated with [Software Security Center](https://www.microfocus.com/en-us/products/software-security-assurance-sdlc/overview).
 It is recommended that a specific user is created in for executing the integration.
 
### Installing the plugin
 
Download the latest version from the _release_ directory and install into Deployment Automation from the 
**Administration\Automation\Plugins** page.

### Using the plugin

The plugin provides three ways/steps to execute a scan. For each you can provide the name of the Application,
Application Version and Security Policy to use. The plugin will attempt to validate each of these.
For the **Create Scan from URL** step you simply need to provide the URL of the running application to scan. For
the **Create Scan from Template** step you need to have created a Scan Template in the WebInspect Enterprise Web Console and
know its Id (this will be present in the browsers URL field when you navigate to it). For the
**Create Scan from Settings File** step you need to have downloaded an XML Scan Settings file and (preferably) stored it
as a file in a [Component Version](http://help.serena.com/doc_center/sra/ver6_3/sda_help/ConcCompVer.html) in Deployment Automation.
                  
You will also need to create three Deployment Automation 
[System Properties](http://help.serena.com/doc_center/sra/ver6_3/sda_help/sra_adm_sys_properties.html)
called `wie.serverUrl` that refers to your WebInspect Enterprise URL (e.g. "https://server-name/WIE") and also
 `wie.username` and `wie.password` that refers to the credentials of the user that is going to run the scan..

### Building the plugin

To build the plugin you will need to clone the following repositories (at the same level as this repository):

 - [mavenBuildConfig](https://github.com/sda-community-plugins/mavenBuildConfig)
 - [plugins-build-parent](https://github.com/sda-community-plugins/plugins-build-parent)
 - [air-plugin-build-script](https://github.com/sda-community-plugins/air-plugin-build-script)
 
 and then compile using the following command
 ```
   mvn clean package
 ```  

This will create a _.zip_ file in the `target` directory when you can then install into Deployment Automation
from the **Administration\Automation\Plugins** page.

If you have any feedback or suggestions on this template then please contact me using the details below.

Kevin A. Lee

kevin.lee@microfocus.com

**Please note: this plugins is provided as a "community" plugin and is not supported by Micro Focus in any way**.
