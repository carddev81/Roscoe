/**
 * 
 */
package com.omo.free.roscoe;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;

import org.simplejavamail.outlookmessageparser.OutlookMessageParser;
import org.simplejavamail.outlookmessageparser.model.OutlookAttachment;
import org.simplejavamail.outlookmessageparser.model.OutlookFileAttachment;
import org.simplejavamail.outlookmessageparser.model.OutlookMessage;
import org.simplejavamail.outlookmessageparser.model.OutlookMsgAttachment;
import org.simplejavamail.outlookmessageparser.model.OutlookRecipient;

import gov.doc.isu.gtv.managers.PropertiesMgr;
import gov.doc.isu.gtv.util.FileUtil;

/**
 * The OutlookFileParser class handles parsing .msg files into .eml files.  API used for doing the work is named outlook-message-parser-1.7.5.jar.  Do not have any documentation on its use.
 * 
 * @author Richard Salas
 */
public class OutlookFileParser {

    private static final String MY_CLASS_NAME = "com.omo.free.roscoe.MsgToEmlParser";
    private static Logger myLogger = Logger.getLogger(MY_CLASS_NAME);

    private static final String EMAIL_SUFFIX = "@oa.mo.gov";

    private List<File> attachments;
    private OutlookMessage outlookMsg;
    private File emlFile;
    private boolean recursion;

    /**
     * Creates an instance of the OutlookFileParser using the given file
     * 
     * @param msgFile the msg file to convert
     * @throws IOException can occur when converting outlook email file
     */
    public OutlookFileParser(File msgFile) throws IOException {
        myLogger.entering(MY_CLASS_NAME, "OutlookFileParser", msgFile);

        this.outlookMsg = new OutlookMessageParser().parseMsg(msgFile);
        this.emlFile = Paths.get(msgFile.getCanonicalFile().getParentFile().getAbsolutePath(), msgFile.getName().replaceFirst("\\.msg", ".eml")).toFile();// email file here
        this.attachments = new ArrayList<>();

        myLogger.exiting(MY_CLASS_NAME, "OutlookFileParser");
    }// end method

    /**
     * Creates an instance of the OutlookFileParser using the given parameters
     * 
     * @param attachmentMsgFile the msg file to fill for sending as attachment
     * @param outlookMsg the outlook message 
     * @throws IOException can occur when converting outlook email file
     */    
    public OutlookFileParser(File attachmentMsgFile, OutlookMessage outlookMsg) throws IOException {
        myLogger.entering(MY_CLASS_NAME, "OutlookFileParser", outlookMsg);

        this.outlookMsg = outlookMsg;
        this.emlFile = attachmentMsgFile;// attachment file is the message 
        this.attachments = new ArrayList<>();
        this.recursion = true;
        
        myLogger.exiting(MY_CLASS_NAME, "OutlookFileParser");
    }// end method

    /**
     * Creates an instance of the OutlookFileParser using the given file and the recurse flag.  The recurse flag is used for determining whether or not the email ID is saved within an email.
     * @param attachmentFile the msg file to convert
     * @param recurse determines whether or not to save the email ID.
     * @throws IOException can occur when converting outlook email file
     */
    public OutlookFileParser(File attachmentFile, boolean recurse) throws IOException {
        this(attachmentFile);
        myLogger.entering(MY_CLASS_NAME, "OutlookFileParser", new Object[]{attachmentFile, recurse});
        this.recursion = recurse;
        myLogger.exiting(MY_CLASS_NAME, "OutlookFileParser");
    }//end constructor

    /**
     * Creates the Thunderbird type email message.  (eml)
     */
    public void createEmlMsg() {
        myLogger.entering(MY_CLASS_NAME, "createEmlMsg");

        FileOutputStream fos = null;

        Message message = null;
        MimeBodyPart content = null;
        try{
            myLogger.info("Parsing message file into " + emlFile.getName());
            //weird mulitparts sectioning here
            message = setAndGetMessageWithHeaders();//setting headers
            // create the message part
            Multipart root = new MimeMultipart("mixed");//mixed for all emails...no matter what...
            content = setAndGetEmailContent();//get the content html or text don't matter

            //need to add related multipart probably because of inline images that may exist...can add further conditional here
            Multipart subInlineImgs = new MimeMultipart("related");
            subInlineImgs.addBodyPart(content);

            addInlineAttachments(subInlineImgs);//add the inline images if they exist.

            MimeBodyPart bpCon1 = new MimeBodyPart();//container to hold inline text and images
            bpCon1.setContent(subInlineImgs);

            Multipart subAltContainer = new MimeMultipart("alternative");
            subAltContainer.addBodyPart(bpCon1);

            MimeBodyPart finalContainer = new MimeBodyPart();//
            finalContainer.setContent(subAltContainer);

            root.addBodyPart(bpCon1);

            addAttachments(root);

            message.setContent(root);
            // store file
            // message.setHeader("Content-Type", "text/html");
            fos = new FileOutputStream(emlFile);
            message.writeTo(fos);
        }catch(Exception e){
            myLogger.log(Level.SEVERE, "Exception occurred while trying to parse the message file is named " + emlFile.getName() + "; error message is: " + e.getMessage(), e);
        }finally{
            if(fos != null){
                try{
                    fos.close();
                }catch(IOException e){
                    myLogger.log(Level.SEVERE, "Exception occurred while trying to close fileoutputstream the message file is named " + emlFile.getName() + "; error message is: " + e.getMessage(), e);
                }//end try...catch
            }// end if

            //clean up after it is finished
            deleteAttachments();
        }// end try...catch

        myLogger.exiting(MY_CLASS_NAME, "createEmlMsg");
    }// end method

    /**
     * Deletes leftover attachments that are not used any more.
     */
    private void deleteAttachments() {
        myLogger.entering(MY_CLASS_NAME, "deleteAttachments");
        if(this.attachments.isEmpty()){//delete 
            myLogger.info("No attachments to delete.");
        }else{
            Iterator<File> it = this.attachments.iterator();
            File attachment = null;
            while(it.hasNext()){
                attachment = it.next();
                myLogger.info("Deleting file leftover file attachments named " + attachment.getName() + "; deleted=" + attachment.delete());
            }//end while
        }//end if

        myLogger.exiting(MY_CLASS_NAME, "deleteAttachments");
    }//end method

    /**
     * Adds inline images to the given Multipart parameter.
     * 
     * @param inlineMultipart the multipart to add inline images to
     * @throws Exception if error occurs while adding images 
     */
    private void addInlineAttachments(Multipart inlineMultipart) throws Exception{
        myLogger.entering(MY_CLASS_NAME, "addInlineAttachments", inlineMultipart);

        List<OutlookAttachment> outlookAttachments = outlookMsg.getOutlookAttachments();
        OutlookFileAttachment outlookAttachment = null;
        
        DataSource source = null;

        for(int i = 0, j = outlookAttachments.size(); i < j;i++){
            MimeBodyPart attachment = new MimeBodyPart();
            if(outlookAttachments.get(i) instanceof OutlookFileAttachment){
                outlookAttachment = (OutlookFileAttachment) outlookAttachments.get(i);
                if(outlookAttachment.getContentId() != null && outlookAttachment.getMimeTag().contains("image")){//is there a content id?
                    myLogger.info("content id exists, must be an inline image setting it as inline: " + outlookAttachment.getContentId());
                    attachment.setContentID(outlookAttachment.getContentId());
                    attachment.setDisposition(MimeBodyPart.INLINE);

                    source = new ByteArrayDataSource(outlookAttachment.getData(), outlookAttachment.getMimeTag());///may move to this later...
                    attachment.setDataHandler(new DataHandler(source));
                    attachment.setFileName(outlookAttachment.getLongFilename().replaceFirst("\\.msg", ".eml"));
                    inlineMultipart.addBodyPart(attachment);

                }//end if
            }//end if
        }//end for
        myLogger.entering(MY_CLASS_NAME, "addInlineAttachments");
    }//end method

    /**
     * Adds attachments to the give Multipart parameter.
     * 
     * @param multipart the Multipart to add attachments to
     * @throws Exception if error occurs while adding images to multipart
     */
    private void addAttachments(Multipart multipart) throws Exception {
        myLogger.entering(MY_CLASS_NAME, "addAttachments", multipart);

        List<OutlookAttachment> outlookAttachments = outlookMsg.getOutlookAttachments();
        OutlookFileAttachment outlookFileAttachment = null;
        OutlookMsgAttachment outlookMsgAttachment = null;
        DataSource source = null;
        String attachmentFilePath = null;
        File attachmentFile = null;
        for(int i = 0, j = outlookAttachments.size(); i < j;i++){
            MimeBodyPart attachment = new MimeBodyPart();
            
            if(outlookAttachments.get(i) instanceof OutlookMsgAttachment){
                outlookMsgAttachment = (OutlookMsgAttachment) outlookAttachments.get(i);
                attachmentFilePath = PropertiesMgr.getProperties().getProperty("roscoe.attachmentDir") + "/" + String.valueOf(outlookMsgAttachment.getOutlookMessage().getSubject()) + ".eml";// wrapped up the subject in a null check...
                // FileUtil.writeFile(outlookFileAttachment.getData(), attachmentFilePath);// have to write it...
                attachmentFile = new File(attachmentFilePath.replace(":", " "));
                // before attaching make sure that the attachment is not another .msg file...if it is then convert it as well.
                // enhancement...will bee the if check below...
                new OutlookFileParser(attachmentFile, outlookMsgAttachment.getOutlookMessage()).createEmlMsg();// recursion here
                source = new FileDataSource(attachmentFile);
                attachment.setDataHandler(new DataHandler(source));
                attachment.setFileName(attachmentFile.getName());
                multipart.addBodyPart(attachment);
                this.attachments.add(attachmentFile);
            }else{
                outlookFileAttachment = (OutlookFileAttachment) outlookAttachments.get(i);
                if(outlookFileAttachment.getContentId() == null || !outlookFileAttachment.getMimeTag().contains("image")){
                    attachmentFilePath = PropertiesMgr.getProperties().getProperty("roscoe.attachmentDir") + "/" + outlookFileAttachment.getLongFilename();
                    FileUtil.writeFile(outlookFileAttachment.getData(), attachmentFilePath);// have to write it...

                    attachmentFile = new File(attachmentFilePath);
                    // before attaching make sure that the attachment is not another .msg file...if it is then convert it as well.
                    // enhancement...will bee the if check below...
                    if(outlookFileAttachment.getLongFilename().endsWith(".msg")){// need to convert the message and add as attachment.
                        new OutlookFileParser(attachmentFile, true).createEmlMsg();// recursion here
                        this.attachments.add(attachmentFile);
                        attachmentFile = new File(PropertiesMgr.getProperties().getProperty("roscoe.attachmentDir") + "/" + outlookFileAttachment.getLongFilename().replaceFirst("\\.msg", ".eml"));
                        // convert attachments...hold here...doing something cool here!!!!
                    }// end if

                    // source = new ByteArrayDataSource(outlookAttachment.getData(), outlookAttachment.getMimeTag());//will use this at a later time...maybe
                    source = new FileDataSource(attachmentFile);
                    attachment.setDataHandler(new DataHandler(source));
                    attachment.setFileName(outlookFileAttachment.getLongFilename().replaceFirst("\\.msg", ".eml"));
                    multipart.addBodyPart(attachment);

                    // add the attachment File to list
                    this.attachments.add(attachmentFile);
                }
            }// end if
        }//end for

        myLogger.exiting(MY_CLASS_NAME, "addAttachments", multipart);
    }//end method

    /**
     * Sets and returns the MimeBodyPart with the email body contents.
     * @return content the body of the email
     * @throws MessagingException
     */
    private MimeBodyPart setAndGetEmailContent() throws MessagingException {
        myLogger.entering(MY_CLASS_NAME, "setAndGetContent");

        MimeBodyPart content = new MimeBodyPart();
        String body = null;
        if(outlookMsg.getBodyHTML() == null && outlookMsg.getBodyText() == null){
            
            body = sanitizeMsg(outlookMsg.getConvertedBodyHTML().replace("Microsoft Exchange Server;converted from html;", ""));
        }else{
            body = outlookMsg.getBodyHTML() == null ? sanitizeMsg(outlookMsg.getBodyText()) : sanitizeMsg(outlookMsg.getBodyHTML());
        }//end if...else

        content.setText(body);

        if(outlookMsg.getBodyHTML() == null && outlookMsg.getBodyText() == null && outlookMsg.getConvertedBodyHTML() != null){
            content.setContent(body, "text/html");
        }else if(outlookMsg.getBodyHTML() == null){//what is it?
            content.setContent(body, "text/plain");
        }else{
            content.setContent(body, "text/html");
        }// end if

        content.setHeader("Message-ID", outlookMsg.getMessageId());

        myLogger.exiting(MY_CLASS_NAME, "setAndGetEmailContent", content);
        return content;
    }//end method

    /**
     * Prints header information.
     */
    public void printHeader() {
        myLogger.entering(MY_CLASS_NAME, "printHeader");

        System.out.println(emlFile.getName() + "=========================================================");
        System.out.println("Display Bcc:" + outlookMsg.getDisplayBcc());
        System.out.println("Display Cc:" + outlookMsg.getDisplayCc());
        System.out.println("Display To:" + outlookMsg.getDisplayTo());
        System.out.println("Subject:" + outlookMsg.getSubject());// TODO
        System.out.println("Cc: " + outlookMsg.getCcRecipients());
        System.out.println("Recipients: " + outlookMsg.getRecipients());
        System.out.println("To Recipients: " + outlookMsg.getToRecipients());
        
        List<OutlookRecipient> toRecipents = outlookMsg.getToRecipients();
        for(OutlookRecipient or : toRecipents){
            System.out.println(or.getName() + " " + or.getAddress());
        }//end method

        //System.out.println("Headers: " + outlookMsg.getHeaders());
        System.out.println("Message-ID: " + outlookMsg.getMessageId());//TODO I may need to keep the message id intact
        System.out.println("Date: " + outlookMsg.getDate());
        System.out.println("From: " + outlookMsg.getFromName());
        System.out.println("From email: " + outlookMsg.getFromEmail());
        System.out.println("=========================================================");

        myLogger.exiting(MY_CLASS_NAME, "printHeader");
    }//end method

    /**
     * Sets and returns Message with message headers such as subject, to, from, ect.
     * @return message the Message with message headers
     * @throws Exception can occur when constructing a Message
     */
    private Message setAndGetMessageWithHeaders() throws Exception {
        myLogger.entering(MY_CLASS_NAME, "setAndGetMessageWithHeaders");

        Message message = new MimeMessage(Session.getInstance(System.getProperties()));
        //setting email From: 
        String fromName = this.outlookMsg.getFromName();
        String fromAddress = this.outlookMsg.getFromEmail();
        
        if(fromAddress == null){//setting from address
            if(fromName.contains(", ")){//split
                String[] names = fromName.split(", ");
                fromAddress = names[0] + "." + names[1] + EMAIL_SUFFIX;
            }else{//just replace the space with a period
                fromAddress = fromName.replace(" ", ".") + EMAIL_SUFFIX;
            }//end if...else
        }//end if

        InternetAddress from = new InternetAddress();
        from.setPersonal(fromName, "UTF-8");
        from.setAddress(fromAddress);
        
        
        message.setFrom(from);

        //setting email To and Cc: 
        InternetAddress[] to = parseRecipients(outlookMsg.getToRecipients());
        InternetAddress[] cc = parseRecipients(outlookMsg.getCcRecipients());

        message.setRecipients(Message.RecipientType.TO, to);
        if(cc != null && cc.length > 0){
            message.setRecipients(Message.RecipientType.CC, cc);
        }//end if

        if(outlookMsg.getMessageId() != null && !"".equals(outlookMsg.getMessageId().trim()) && !this.recursion){
            message.addRecipient(Message.RecipientType.CC, new InternetAddress("RTSEMAILID"+outlookMsg.getMessageId().replaceAll("(\\>|\\<)", ""), "RTSEMAILID"));
        }else{
            System.out.println("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~NO MESSAGE ID~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
        }//end if...else

        message.setSentDate(outlookMsg.getDate());
        message.setSubject(outlookMsg.getSubject());
        //message.addHeader("Message-ID", outlookMsg.getMessageId());
        message.setHeader("Message-ID", outlookMsg.getMessageId());

        myLogger.exiting(MY_CLASS_NAME, "setAndGetMessageWithHeaders", message);
        return message;
    }//end method

    /**
     * Parses reciepient lists into an array of InternetAddress's 
     * 
     * @param recipients the array of InternetAddress's
     * @return array of InternetAddress's
     * @throws Exception can occur while trying to parse a list of recipients
     */
    private InternetAddress[] parseRecipients(List<OutlookRecipient> recipients) throws UnsupportedEncodingException {
        myLogger.entering(MY_CLASS_NAME, "parseRecipients", recipients);
        
        List<InternetAddress> addresses = new ArrayList<>();
        Iterator<OutlookRecipient> recipentsIt = recipients.iterator();
        OutlookRecipient recipient = null;
        InternetAddress addr = null;
        while(recipentsIt.hasNext()){
            recipient = recipentsIt.next();
            addr = new InternetAddress();
            addr.setAddress(recipient.getAddress());
            addr.setPersonal(recipient.getName(), "UTF-8");
            addresses.add(addr);
        }//end while

        myLogger.exiting(MY_CLASS_NAME, "parseRecipients", String.valueOf(addresses));
        return addresses.toArray(new InternetAddress[0]);
    }//end method

    /**
     * Sanitizes the message of weird characters.  TODO quick coding here...come back and rewrite
     * 
     * @param message
     * @return the sanitized message
     */
    private String sanitizeMsg(String message) {
        //replaceAll("[^\\p{ASCII}]", "")....remove all weird characters at the end.
        return message.replaceAll("�", "").replaceAll("(’|‘)", "'").replaceAll("–", "&ndash;").replaceAll("﻿", "").replaceAll("�", "'");
    }// end method

}// end class
