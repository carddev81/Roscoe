Roscoe DISTRIBUTION NOTES
Created By:  Richard Salas
=========================================================

Description
===============================================================
This application is used for converting Outlook messages 
(*.msg) to Thunderbird messages (*.eml).
Note that this application is temporary only and should be 
discontinued when the Brownie application is active (10/05/21).
===============================================================

Current files in this directory are REQUIRED (Roscoe)
===============================================================
RoscoeStart.bat
RoscoeStateEmailIDScriptRTS.xml
RoscoeStateEmailIDScriptRTSStarter.bat
===============================================================

===============================================================
RoscoeStart.bat
	This bat file is used for starting the Roscoe application.
	The bat file contains a jar command for launching the main
	class within the Roscoe.jar.  
===============================================================

===============================================================
RoscoeStateEmailIDScriptRTS.xml
RoscoeStateEmailIDScriptRTSStarter.bat
	The xml file is an ANT script and the bat file was created
	as a convenience for executing the ANT script.  This ANT
	script is required to be ran against a set of *.eml
	files before forwarding the messages to Admins.  This will 
	ensure that the state email threads are not broken.
===============================================================

===============================================================
STEP-BY-STEP INSTRUCTIONS (Common Scenario)
	1) State ITSD employee is asked to forward an email to an ISU employee.
	2) State ITSD employee saves email to USB drive in a folder named ISU.  This folder will contain the Roscoe.jar and RoscoeStart.bat.
	3) State ITSD employee plugs USB drive into machine connected to ISU network. 
	4) State ITSD employee locates and executes the RoscoeStart.bat.  This bat file will be located on the USB drive alongside the Roscoe.jar.
	5) State ITSD employee forwards email(s) to ISU employee(s).
	6) ISU employee replies to email.
	7) State ITSD employee saves email to USB drive in a folder named STATE.  This folder will contain the RoscoeStateEmailIDScriptRTS.xml and RoscoeStateEmailIDScriptRTSStarter.bat files
	8) State ITSD employee plugs USB drive into machine connected to State network.
	9) State ITSD employee locates and executes the RoscoeStateEmailIDScriptRTSStarter.bat.
	10) State ITSD employee forwards email(s) to ITSD employee(s).
===============================================================
