package kocot.klass.structures;



public class Group {

    private String groupName;
    private String ownerUID;


    public Group(){

        ownerUID = null;
        groupName = null;

    }

    public Group( String ownerUID, String groupName) {

        this.ownerUID = ownerUID;
        this.groupName = groupName;
    }


    public String getOwnerUID() {
        return ownerUID;
    }

    public void setOwnerUID(String ownerUID) {
        this.ownerUID = ownerUID;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }


}
