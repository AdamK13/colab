package kocot.klass.structures;

import androidx.annotation.NonNull;
import androidx.browser.trusted.sharing.ShareTarget;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.util.ArrayList;
import java.util.HashMap;

public class User {


    private String email;

    private String name;

    private ArrayList<String> groups;


    public User(){

    }

    public User(String email, String name){

        this.email = email;
        this.name = name;

        this.groups = new ArrayList<>();

    }


    public HashMap<String,Object> makeUserMap(){

        HashMap<String,Object> map = new HashMap<>();

        map.put("email",email);
        map.put("name",name);
        map.put("groups",groups);


        return map;

    }


    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ArrayList<String> getGroups() {
        return groups;
    }

    public void setGroups(ArrayList<String> groups) {
        this.groups = groups;
    }
}
