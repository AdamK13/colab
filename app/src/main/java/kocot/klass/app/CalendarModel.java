package kocot.klass.app;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import java.util.List;

import io.reactivex.rxjava3.core.Single;
import kocot.klass.BridgeManager;
import kocot.klass.structures.CalendarEvent;
import kocot.klass.auxiliary.LiveDataResponse;

public class CalendarModel extends AndroidViewModel {

    private BridgeManager bridgeManager;

    public CalendarModel(@NonNull Application application) {
        super(application);
        bridgeManager = BridgeManager.getInstance(getApplication());
    }

    public LiveData<List<CalendarEvent>> getLiveEvents(String group){

        return bridgeManager.getLiveEvents(group);

    }

    public LiveData<LiveDataResponse> createEvent(String group, long time, String title, String description){

        return bridgeManager.createEvent(group,time,title,description);

    }

    public List<CalendarEvent> getEventsForPeriod(String group, long from, long to){

        return bridgeManager.getEventsForPeriod(group,from,to);
    }

    public boolean isUserAdmin(){

        return bridgeManager.isUserAdminLocal(bridgeManager.getCurrentGroup());

    }
    public Single<Boolean> isConversationOpen(){

        return bridgeManager.isConversationOpen(bridgeManager.getCurrentGroup());

    }





}
