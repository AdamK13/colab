package kocot.klass.app;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import kocot.klass.BridgeManager;
import kocot.klass.auxiliary.LiveDataResponse;

public class SetupModel extends AndroidViewModel {

    public static final int STATUS_HOLD = 0;
    public static final int STATUS_PROCEED = 1;
    private BridgeManager bridgeManager;
    private MutableLiveData<Integer> observableStatus;


    public SetupModel(@NonNull Application application){
        super(application);

        bridgeManager = BridgeManager.getInstance(getApplication());
        bridgeManager.lazyInit();

        observableStatus = new MutableLiveData<>();

    }

    public LiveData<LiveDataResponse> createGroup(String groupName, String groupPassword){

        return bridgeManager.createGroup(groupName,groupPassword);

    }

    public LiveData<LiveDataResponse> joinGroup(String groupName, String groupPassword){

        return bridgeManager.joinGroup(groupName, groupPassword);

    }

    public boolean validateGroupName(String groupName){

        return groupName.matches("^[a-zA-Z0-9]{3,10}$");

    }

    public boolean validateGroupPassword(String groupPassword){

        return groupPassword.length() > 5 && groupPassword.length() < 51;

    }


    public LiveData<Integer> observeStatus(){

        return observableStatus;

    }


    public void postStatus(int status){

        observableStatus.postValue(status);

    }

    public void setGroup(String group){
        bridgeManager.setGroup(group);
    }

    public boolean isInternetAccessible(){
        return bridgeManager.isInternetAccessibleNow();
    }

}
