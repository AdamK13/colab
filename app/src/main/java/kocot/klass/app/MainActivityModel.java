package kocot.klass.app;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import java.util.ArrayList;

import io.reactivex.rxjava3.core.Observable;
import kocot.klass.BridgeManager;
import kocot.klass.auxiliary.LiveDataResponse;

public class MainActivityModel extends AndroidViewModel {

    private BridgeManager bridgeManager;

    public MainActivityModel(@NonNull Application application) {

        super(application);
        bridgeManager = BridgeManager.getInstance(getApplication());

    }

    public Observable<LiveDataResponse> init(){
        return bridgeManager.init();
    }

    public void setGroup(@Nullable String group) {

        bridgeManager.setGroup(group);

    }

    public String getCurrentGroup(){

        return bridgeManager.getCurrentGroup();

    }

    public boolean groupAvailable(String group){

        return bridgeManager.groupAvailable(group);

    }

    public ArrayList<String> getGroups(){
        return bridgeManager.getGroups();
    }

    public LiveData<Boolean> isAdmin(){

        String group = getCurrentGroup();
        return bridgeManager.isUserAdminLive(group);

    }


    public void saveAdminStatus(Boolean status){
        bridgeManager.saveAdminStatus(getCurrentGroup(),status);
    }

    public boolean isInternetAccessible(){
        return bridgeManager.isInternetAccessibleNow();
    }

    public boolean isUserSignedIn(){
        return bridgeManager.isUserSignedIn();
    }


}
