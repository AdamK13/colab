package kocot.klass.app;


import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;


import java.util.List;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import kocot.klass.BridgeManager;
import kocot.klass.auxiliary.LiveDataResponse;
import kocot.klass.structures.Message;

public class MessagesModel extends AndroidViewModel {


    private BridgeManager bridgeManager;

    public MessagesModel(Application application) {

        super(application);

        bridgeManager = BridgeManager.getInstance(getApplication());

    }




    public LiveData<List<Message>> getLiveMessages(String group){

        return bridgeManager.getLiveMessages(group);

    }




    public LiveData<LiveDataResponse> sendMessage(String group,String messageContent){

        return bridgeManager.sendMessage(group,messageContent);

    }

    public boolean isUserAdmin(){

        return bridgeManager.isUserAdminLocal(bridgeManager.getCurrentGroup());

    }

    public Single<Boolean> isConversationOpen(){

        return bridgeManager.isConversationOpen(bridgeManager.getCurrentGroup());


    }


}
