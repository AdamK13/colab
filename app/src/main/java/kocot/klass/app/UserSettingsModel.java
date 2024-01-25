package kocot.klass.app;

import android.app.Application;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import java.util.ArrayList;
import java.util.List;

import kocot.klass.BridgeManager;
import kocot.klass.auxiliary.LiveDataResponse;

public class UserSettingsModel extends AndroidViewModel {

    private BridgeManager bridgeManager = BridgeManager.getInstance(getApplication());
    public UserSettingsModel(@NonNull Application application) {
        super(application);
    }

    public ArrayList<String> getGroups(){
        return bridgeManager.getGroups();
    }

    public Boolean isUserAdmin(String group){
        return bridgeManager.isUserAdminLocal(group);
    }

    public LiveData<LiveDataResponse> changeAccountPassword(String newPassword){

        return bridgeManager.changeAccountPassword(newPassword);

    }

    public void switchToAnyGroup(){
        bridgeManager.setGroup(null);
    }


    public Pair<String,String> getUsernameEmail(){
        return bridgeManager.getOwnUsernameEmail();
    }

    public LiveData<LiveDataResponse> reLogin(String password){

        return bridgeManager.reLogin(password);

    }
    public void logout(){
        bridgeManager.logout();
    }

    public LiveData<LiveDataResponse> leaveGroup(String group){
        return bridgeManager.leaveGroup(group);
    }

    public LiveData<LiveDataResponse> deleteGroup(String group){
        return bridgeManager.deleteGroup(group);
    }

    public boolean isInternetAccessible(){
        return bridgeManager.isInternetAccessibleNow();
    }

    public LiveData<LiveDataResponse> deleteAccount(){
        return bridgeManager.deleteAccount();
    }
}
