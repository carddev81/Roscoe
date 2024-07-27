/**
 *
 */
package com.omo.free.roscoe;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.logging.Level;
import java.util.logging.Logger;

import gov.doc.isu.gtv.core.AbstractApplication;
import gov.doc.isu.gtv.core.UserInterface;
import gov.doc.isu.gtv.exception.PrepareException;
import gov.doc.isu.gtv.managers.PropertiesMgr;
import gov.doc.isu.gtv.model.CustomProperties;
import gov.doc.isu.gtv.util.DateUtil;
import gov.doc.isu.gtv.util.FileUtil;

/**
 * This Roscoe class is the driving class for converting Outlook messages (msg) to Thunderbird messages (eml).
 *
 * @author Richard Salas
 */
public class Roscoe extends AbstractApplication {

    private static final long serialVersionUID = 1L;

    private static final String MY_CLASS_NAME = "com.omo.free.roscoe.Roscoe";
    private static Logger myLogger = Logger.getLogger(MY_CLASS_NAME);

    /**
     * Constructor that creates an instance of this class.
     *
     * @param args
     *        arguments that are passed into this method
     * @throws PrepareException
     *         exception thrown during Prep of application when PREPARE command has been passed into the application through the command line.
     */
    public Roscoe(String[] args) throws PrepareException {
        super(args);
    }// end constructor

    /**
     * This method is used for starting the Roscoe Application.
     *
     * @param args
     *        arguments passed into the application.
     * @throws UnsupportedEncodingException
     */
    public static void main(String[] args) throws UnsupportedEncodingException {
        long start = System.currentTimeMillis();
        try{
            new Roscoe(args);
            myLogger.info("Controller class for the application has completed. Running time is:" + " " + DateUtil.asTime(System.currentTimeMillis() - start));
        }catch(Exception e){
            System.err.println("Exception caught in main! Message is: " + e.getMessage());
            System.exit(1);
        }// end try/catch
    }// end method

    /**
     * This method will prepare to make sure resources are available to the application. And used by the application to initialize the application logger, (Email Logger).
     *
     * @throws Exception
     *         thrown while trying to set up application directories and other initialization
     */
    @Override
    protected void process() throws Exception {
        myLogger.entering(MY_CLASS_NAME, "process() method - initialize resources needed for application run method.");

        FileUtil.checkDirectories(PropertiesMgr.getProperties().getProperty("roscoe.attachmentDir"));

        myLogger.exiting(MY_CLASS_NAME, "process() method");
    }// end process

    /**
     * This method is the controlling method in this class.
     *
     * @throws Exception
     *         the exception
     */
    @Override
    protected void run() {
        myLogger.entering(MY_CLASS_NAME, "run");

        boolean openPrgm = getArguments().length > 0;// can fix this later...quick write...just

        if(new File(PropertiesMgr.getProperties().getProperty("roscoe.attachmentDir")).exists()){

            File[] emails = new File(".").listFiles((dir, name) -> name.endsWith("msg"));// list email messages.
            File email = null;
            if(emails.length > 0){
                for(int i = 0, j = emails.length;i < j;i++){// need to convert and then write eml files here...just need a simple bufferedwriter to do this!!! :)
                    try{
                        email = emails[i];
                        new OutlookFileParser(email).createEmlMsg();
                    }catch(Exception e){
                        myLogger.log(Level.SEVERE, "Exception occurred while trying to convert msg to eml.  Error message is: " + e.getMessage() + "; file name is: " + email.getName(), e);
                    }// end try catch
                }// end for

                // open emails using desktop

                if(openPrgm){
                    File[] emailsToOpen = new File(".").listFiles((dir, name) -> name.endsWith("eml"));// list email messages.
                    for(int i = 0, j = emailsToOpen.length;i < j;i++){
                        try{
                            Desktop.getDesktop().open(emailsToOpen[i]);
                        }catch(IOException e){
                            e.printStackTrace();
                        }// end try...catch
                    }// end for
                }//end if
            }else{
                myLogger.info("Roscoe found no .msg files to be converted");
            }// end if

        }// end if

        myLogger.exiting(MY_CLASS_NAME, "run");
    }// end method

    /**
     * This method is used to execute logic after the run() method has finished to send administrative email and to also clean up unused logs.
     *
     * @throws Exception
     *         thrown when something unexpected goes wrong
     */
    @Override
    protected void postprocess() throws Exception {
        myLogger.entering(MY_CLASS_NAME, "postprocess");
        cleanupLogs();
        myLogger.exiting(MY_CLASS_NAME, "postprocess");
    }// end method

    /**
     * {@inheritDoc}
     */
    @Override
    public String getApplicationName() {
        return "Roscoe";
    }// end getApplicationName

    /**
     * {@inheritDoc}
     */
    @Override
    protected CustomProperties getAdditionalApplicationProperties() {
        CustomProperties properties = new CustomProperties();
        properties.put("roscoe.attachmentDir", "./attachments", "Roscoes attachment directory.");
        return properties;
    }// end method

    /**
     * {@inheritDoc}
     */
    @Override
    protected CustomProperties getAdditionalLoggingProperties() {
        CustomProperties properties = new CustomProperties();
        properties.put("com.omo.free.roscoe.level", "INFO", "Logging level for driving class");
        return properties;
    }// end method

    @Override
    protected UserInterface getUserInterface() {
        return null;
    }// end method

    @Override
    protected String getEncryptionKey() {
        return "docsecretpassword";
    }// end method

}// end class
