package kocot.klass.app;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import kocot.klass.BridgeManager;
import kocot.klass.auxiliary.LiveDataResponse;

public class GroupAdminActivityModel extends AndroidViewModel {

    private final BridgeManager bridgeManager;

    public GroupAdminActivityModel(@NonNull Application application) {
        super(application);
        bridgeManager = BridgeManager.getInstance(application);
    }

    public LiveData<LiveDataResponse> getGroupSettings(){

        String group = bridgeManager.getCurrentGroup();
        if(group == null){
            return new MutableLiveData<>(new LiveDataResponse(false,"NOGROUP"));
        }

        return bridgeManager.getGroupSettings();

    }

    public LiveData<LiveDataResponse> getGroupMembers(){
        return bridgeManager.getGroupMembers(getCurrentGroup());
    }

    public LiveData<LiveDataResponse> removeGroupMember(String uidToRemove){
        return bridgeManager.removeGroupMember(getCurrentGroup(),uidToRemove);
    }

    public boolean validateGroupPassword(String groupPassword){

        return groupPassword.length() > 5 && groupPassword.length() < 51;

    }

    public String getCurrentGroup(){
        return bridgeManager.getCurrentGroup();
    }

    public String getOwnUID(){
        return bridgeManager.getOwnUID();
    }

    public LiveData<LiveDataResponse> lockGroup(){
        return bridgeManager.changeGroupLockMode(true);
    }

    public LiveData<LiveDataResponse> unlockGroup(){
        return bridgeManager.changeGroupLockMode(false);
    }

    public LiveData<LiveDataResponse> setOpenConversationMode(){
        return bridgeManager.changeGroupConversationMode(false);
    }

    public LiveData<LiveDataResponse> setOwnerOnlyConversationMode(){
        return bridgeManager.changeGroupConversationMode(true);
    }

    public LiveData<LiveDataResponse> changePassword(String password){
        return bridgeManager.changeGroupPassword(password);
    }

}
