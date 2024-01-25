package kocot.klass.app;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.core.util.Pair;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import io.reactivex.rxjava3.core.Single;
import kocot.klass.BridgeManager;
import kocot.klass.auxiliary.LiveDataResponse;
import kocot.klass.structures.Project;

public class ProjectsModel extends AndroidViewModel {

    private BridgeManager bridgeManager;
    public ProjectsModel(@NonNull Application application) {
        super(application);
        bridgeManager = BridgeManager.getInstance(getApplication());
    }

    public LiveData<LiveDataResponse> getGroupMembers(){

        return bridgeManager.getGroupMembers(bridgeManager.getCurrentGroup());

    }

    public String getOwnUID(){

        return bridgeManager.getOwnUID();

    }

    public LiveData<LiveDataResponse> createProject(String title, String content, ArrayList<String> memberUIDs){

        return bridgeManager.createProject(title,content,memberUIDs);

    }

    public LiveData<LiveDataResponse> deleteProject(String group, String projectID){

        return bridgeManager.deleteProject(group,projectID);

    }

    public LiveData<LiveDataResponse> editProjectMembers(String group, String projectID, ArrayList<String> newMembers){

        return bridgeManager.editProjectMembers(group, projectID, newMembers);

    }

    public void updateProjects(String group){

        bridgeManager.updateGroupProjects(group);
    }

    public void updateProjects(){

        bridgeManager.updateGroupProjects(bridgeManager.getCurrentGroup());

    }

    public void forceUpdateProject(String projectID){

        bridgeManager.forceUpdateProject(projectID);

    }

    public LiveData<List<Project>> getLiveProjects(){

        return bridgeManager.getLiveProjects();

    }

    public Boolean isUserAdmin(){
        return bridgeManager.isUserAdminLocal(bridgeManager.getCurrentGroup());
    }

    public Single<Boolean> isConversationOpen(){

        return bridgeManager.isConversationOpen(bridgeManager.getCurrentGroup());

    }

}
