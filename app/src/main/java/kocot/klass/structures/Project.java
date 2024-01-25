package kocot.klass.structures;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import com.google.firebase.Timestamp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

@Entity(tableName = "projects")
public class Project {

    @PrimaryKey @NonNull
    private String projectID;
    @ColumnInfo(name = "projectGroup")
    private String group;
    @ColumnInfo(name = "projectCreator")
    private String creatorUid;
    @ColumnInfo(name = "projectTitle")
    private String title;
    @ColumnInfo(name = "projectContent")
    private String content;
    @ColumnInfo(name = "projectCreation")
    private long created;
    @Ignore
    private ArrayList<String> projectMembers;
    @ColumnInfo(name = "projectMembers")
    private String projectMembersRoom;



    public Project (@NonNull String projectID, String group, String creatorUid, String title, String content, long created,
                    ArrayList<String> projectMembers){

        this.projectID = projectID;
        this.group = group;
        this.creatorUid = creatorUid;
        this.title = title;
        this.content = content;
        this.created = created;
        this.projectMembers = projectMembers;
        convertArrayListToRoom(projectMembers);


    }

    public Project(@NonNull String projectID, String group, String creatorUid, String title, String content, long created,
                   String projectMembersRoom){

        this.projectID = projectID;
        this.group = group;
        this.creatorUid = creatorUid;
        this.title = title;
        this.content = content;
        this.created = created;
        this.projectMembersRoom = projectMembersRoom;
        convertRoomToArrayList(projectMembersRoom);

    }

    public Project(Map<String, Object> rawProject){

        this.projectID = (String) rawProject.get("projectID");
        this.group = (String) rawProject.get("group");
        this.creatorUid = (String) rawProject.get("creatorUid");
        this.title = (String) rawProject.get("title");
        this.content = (String) rawProject.get("content");
        Timestamp timestamp = (Timestamp) rawProject.get("created");
        this.created = timestamp.getSeconds();
        this.projectMembers = (ArrayList<String>) rawProject.get("projectMembers");
        convertArrayListToRoom(this.projectMembers);

    }

    public Project(){

    }



    @NonNull
    public String getProjectID() {
        return projectID;
    }

    public void setProjectID(@NonNull String projectID) {
        this.projectID = projectID;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public String getCreatorUid() {
        return creatorUid;
    }

    public void setCreatorUid(String creatorUid) {
        this.creatorUid = creatorUid;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public long getCreated() {
        return created;
    }

    public void setCreated(long created) {
        this.created = created;
    }

    public ArrayList<String> getProjectMembers() {
        return projectMembers;
    }

    public void setProjectMembers(ArrayList<String> projectMembers) {
        this.projectMembers = projectMembers;
        convertArrayListToRoom(projectMembers);
    }

    public String getProjectMembersRoom() {
        return projectMembersRoom;
    }

    public void setProjectMembersRoom(String projectMembersRoom) {
        this.projectMembersRoom = projectMembersRoom;
        convertRoomToArrayList(projectMembersRoom);
    }

    public void convertArrayListToRoom(ArrayList<String> projectMembers){

        StringBuilder builder = new StringBuilder();

        for (String member : projectMembers){
            String s = member+",";
            builder.append(s);
        }

        this.projectMembersRoom = builder.toString();

    }

    public void convertRoomToArrayList(String projectMembers){

        String[] split = projectMembers.split(",");

        this.projectMembers = new ArrayList<>(Arrays.asList(split));

    }

    @Override
    public boolean equals(Object obj){
        if (!(obj instanceof Project)){
            return false;
        }

        Project project = (Project) obj;

        return this.projectID.equals(project.projectID);
    }

}
