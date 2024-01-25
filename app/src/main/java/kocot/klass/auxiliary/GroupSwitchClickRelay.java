package kocot.klass.auxiliary;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;


public class GroupSwitchClickRelay {

    MutableLiveData<String> relay;
    private static GroupSwitchClickRelay instance;

    private GroupSwitchClickRelay(){
        this.relay = new MutableLiveData<>();
    }

    public static GroupSwitchClickRelay getInstance(){

        if(instance == null){
            instance = new GroupSwitchClickRelay();
        }
        return instance;

    }

    public void relayClick(String group){
        relay.postValue(group);
    }

    public LiveData<String> getRelay(){
        return relay;
    }


}
