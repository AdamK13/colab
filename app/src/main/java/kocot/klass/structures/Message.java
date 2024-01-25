package kocot.klass.structures;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import com.google.firebase.Timestamp;

import org.apache.commons.codec.digest.DigestUtils;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Entity(tableName = "messages")
public class Message {

    @PrimaryKey @NonNull
    private String messageID;
    @ColumnInfo(name = "sender")
    private String username;
    @ColumnInfo(name = "senderUID")
    private String uid;
    @ColumnInfo(name = "timestampSeconds")
    private long timestampSeconds;
    @ColumnInfo(name = "messageGroup")
    private String group;
    @ColumnInfo(name = "content")
    private String content;


    public Message(){

    }

    public Message(String username, String uid,long timestampSeconds, String content, String group){

        this.username = username;
        this.uid = uid;
        this.content = content;
        this.timestampSeconds = timestampSeconds;
        this.group = group;


        messageID = DigestUtils.sha1Hex(content+uid+group);


    }

    public Message(Map<String,Object> documentData){

        this.content = (String) documentData.get("content");
        this.group = (String) documentData.get("group");
        this.messageID = (String) documentData.get("messageID");
        Timestamp timestamp = (Timestamp) documentData.get("timestampSeconds");
        this.timestampSeconds = timestamp.getSeconds();
        this.uid = (String) documentData.get("uid");
        this.username = (String) documentData.get("username");

    }

    public String getUsername() {
        return username;
    }

    public String getUid() {
        return uid;
    }


    public String getContent() {
        return content;
    }

    public String getGroup(){
        return group;
    }

    public long getTimestampSeconds(){
        return timestampSeconds;
    }

    public String getMessageID(){
        return messageID;
    }
    public void setMessageID(@NonNull String messageID) {
        this.messageID = messageID;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }


    public void setGroup(String group) {
        this.group = group;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public void setTimestampSeconds(long timestampSeconds){
        this.timestampSeconds = timestampSeconds;
    }

    @Override
    public boolean equals(Object obj){
        if (!(obj instanceof Message)){
            return false;
        }

        Message message = (Message) obj;

        return this.messageID.equals(message.messageID);
    }

}
