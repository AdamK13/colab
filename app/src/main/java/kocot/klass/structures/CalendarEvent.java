package kocot.klass.structures;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.util.Map;

@Entity(tableName = "calendarEvents")
public class CalendarEvent {

    @PrimaryKey @NonNull
    private String eventID;
    @ColumnInfo (name = "eventGroup")
    private String group;
    @ColumnInfo (name = "eventCreator")
    private String creator;
    @ColumnInfo(name = "eventContent")
    private String content;
    @ColumnInfo(name = "eventTitle")
    private String title;
    @ColumnInfo(name = "eventTimestamp")
    private long timestampMillis;

    public CalendarEvent(String title, long timestampMillis, String creator, String content, String group, @NonNull String eventID){

        this.content = content;
        this.creator = creator;
        this.timestampMillis = timestampMillis;
        this.group = group;
        this.eventID = eventID;
        this.title = title;

    }

    public CalendarEvent(Map<String, Object> rawEvent){

        this.content = (String) rawEvent.get("content");
        this.group = (String) rawEvent.get("group");
        this.creator = (String) rawEvent.get("creatorName");
        this.timestampMillis = (long) rawEvent.get("timestamp");
        this.eventID = (String) rawEvent.get("eventID");
        this.title = (String) rawEvent.get("title");

    }

    public String getEventID() {
        return eventID;
    }

    public void setEventID(String eventID) {
        this.eventID = eventID;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public String getCreator() {
        return creator;
    }

    public void setCreator(String creator) {
        this.creator = creator;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public long getTimestampMillis() {
        return timestampMillis;
    }

    public void setTimestampMillis(long timestampMillis) {
        this.timestampMillis = timestampMillis;
    }

    public String getTitle(){
        return title;
    }

    public void setTitle(String title){
        this.title = title;
    }

    @Override
    public boolean equals(Object obj){
        if (!(obj instanceof CalendarEvent)){
            return false;
        }

        CalendarEvent event = (CalendarEvent) obj;

        return this.eventID.equals(event.eventID);
    }
}
