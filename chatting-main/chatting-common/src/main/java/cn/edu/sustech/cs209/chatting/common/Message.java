package cn.edu.sustech.cs209.chatting.common;

import java.io.File;
import java.io.Serializable;
import java.time.LocalDateTime;

public class Message implements Serializable {

    private transient LocalDateTime timestamp;

    private String sentBy;

    private String sendTo;

    private String data;

    //文件
    private boolean isFile;
    private transient File file;
    //
    private String filename;
    private String fileType;
    private byte[] fileDate;
    public Message(String sentBy, String sendTo, String data) {
        this.timestamp = LocalDateTime.MIN;
        this.sentBy = sentBy;
        this.sendTo = sendTo;
        this.data = data;
    }
    public Message(LocalDateTime timestamp, String sentBy, String sendTo, String data) {
        this.timestamp = timestamp;
        this.sentBy = sentBy;
        this.sendTo = sendTo;
        this.data = data;
    }


    public Message(LocalDateTime timestamp, String sentBy, String sendTo, File file, boolean isFile){
        this.timestamp=timestamp;
        this.sendTo=sendTo;
        this.sentBy=sentBy;
        this.file=file;
        this.isFile=isFile;
        this.filename = file.getName();
    }

    public Message(LocalDateTime timestamp, String sentBy, String sendTo, String filename, String fileType, byte[] fileDate, boolean isFile){
        this.timestamp=timestamp;
        this.sendTo=sendTo;
        this.sentBy=sentBy;
        this.isFile=isFile;
        this.filename=filename;
        this.fileType=fileType;
        this.fileDate=fileDate;
    }
    public byte[] getFileDate() {
        return fileDate;
    }
    public String getFileType() {
        return fileType;
    }
    public boolean isFileType(){
        return isFile;
    }
    public String getFileName(){
        return filename;
    }
    public File getFile(){
        return file;
    }
    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public String getSentBy() {
        return sentBy;
    }

    public String getSendTo() {
        return sendTo;
    }

    public String getData() {
        return data;
    }
}
