package kocot.klass;

import static android.content.Context.MODE_PRIVATE;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.util.Pair;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.annotations.Nullable;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.observers.DisposableObserver;
import io.reactivex.rxjava3.observers.DisposableSingleObserver;
import kocot.klass.localDB.LocalDatabaseManager;
import kocot.klass.structures.CalendarEvent;
import kocot.klass.auxiliary.LiveDataResponse;
import kocot.klass.structures.Message;
import kocot.klass.structures.Project;
import kocot.klass.structures.User;

public final class BridgeManager {

    private static final String sharedPrefGroups = "groupList";
    private static final String sharedPrefUser = "user";
    private static final String sharedPrefLastGroup = "lastGroup";
    private static final String sharedPrefNewGroup = "newGroup";
    private static BridgeManager instance;

    private static SharedPreferences groupPreferences;
    private static SharedPreferences userPreferences;

    private String currentGroup = null;
    private Boolean conversationOpen = null;
    public User currentUser = null;
    private final FirebaseManager fireManager;
    private final LocalDatabaseManager localManager;

    private final Context context;


    private BridgeManager(Context context){

        fireManager = FirebaseManager.getInstance();
        localManager = LocalDatabaseManager.getInstance(context);
        this.context = context;
        groupPreferences = context.getSharedPreferences(sharedPrefGroups,MODE_PRIVATE);
        userPreferences = context.getSharedPreferences(sharedPrefUser,MODE_PRIVATE);


    }

    public static BridgeManager getInstance(Context context){

        if(instance == null){
            instance = new BridgeManager(context);
        }

        return instance;

    }

    public boolean isInternetAccessibleInit(){

        ConnectivityManager connectivityManager = context.getSystemService(ConnectivityManager.class);
        Network currentNetwork = connectivityManager.getActiveNetwork();
        if(currentNetwork == null){
            connectivityManager.addDefaultNetworkActiveListener(new ConnectivityManager.OnNetworkActiveListener() {
                @Override
                public void onNetworkActive() {
                    init();
                }
            });
            return false;
        }
        NetworkCapabilities caps = connectivityManager.getNetworkCapabilities(currentNetwork);

        if(!caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)){

            ConnectivityManager.NetworkCallback callback = new ConnectivityManager.NetworkCallback(){

                @Override
                public void onAvailable(@NonNull Network network){
                    super.onAvailable(network);
                    init();
                }
            };

        }

        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
    }

    public boolean isInternetAccessibleNow(){
        ConnectivityManager connectivityManager = context.getSystemService(ConnectivityManager.class);
        Network currentNetwork = connectivityManager.getActiveNetwork();
        if(currentNetwork == null){
            return false;
        }
        NetworkCapabilities caps = connectivityManager.getNetworkCapabilities(currentNetwork);
        if(caps == null){
            return false;
        }
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
    }

    public boolean isUserSignedIn(){
        return fireManager.isUserSignedIn();
    }



    public Observable<LiveDataResponse> init(){

        return Observable.create(emitter -> {

            getCurrentUser().subscribe(new DisposableSingleObserver<User>() {
                @Override
                public void onSuccess(@NonNull User user) {
                    emitter.onNext(new LiveDataResponse(true,"USER",user));
                }

                @Override
                public void onError(@NonNull Throwable e) {
                    if(e.getMessage().equals("CONNECTION")){
                        emitter.onNext(new LiveDataResponse(true,"USER",null));
                    }
                    emitter.onNext(new LiveDataResponse(false,"USER",null));
                }
            });
            updateGroupsData().subscribe(new DisposableSingleObserver<LiveDataResponse>() {
                @Override
                public void onSuccess(@NonNull LiveDataResponse liveDataResponse) {
                    emitter.onNext(liveDataResponse);
                }

                @Override
                public void onError(@NonNull Throwable e) {
                    emitter.onNext(new LiveDataResponse(false,"GROUPS",e.getMessage()));

                }
            });

        });
    }

    public void lazyInit(){

        if(!isInternetAccessibleNow()){
            return;
        }

        getCurrentUser().subscribe().dispose();
        updateGroupsData().subscribe().dispose();


    }

    private Single<User> getCurrentUser(){

        return Single.create(emitter -> {

            if(!isInternetAccessibleInit()){

                emitter.onError(new Throwable("CONNECTION"));
            }

            fireManager.getCurrentUser().subscribe(new DisposableSingleObserver<User>() {
                @Override
                public void onSuccess(@NonNull User user) {
                    currentUser = user;
                    Set<String> set = new HashSet<>();
                    set.add("USERNAME:"+user.getName());
                    set.add("EMAIL:"+user.getEmail());
                    userPreferences.edit().putStringSet(sharedPrefUser,set).apply();
                    emitter.onSuccess(user);
                    dispose();
                }

                @Override
                public void onError(@NonNull Throwable e) {
                    emitter.onError(e);
                    dispose();
                }
            });

        });




    }


    private Single<LiveDataResponse> updateGroupsData(){

        return Single.create(emitter -> {
            if(!isInternetAccessibleInit()){
                emitter.onSuccess(new LiveDataResponse(true,"GROUPS","initOffline"));
                return;
            }

            fireManager.getUserGroups().subscribe(new DisposableSingleObserver<ArrayList<String>>() {
                @Override
                public void onSuccess(@NonNull ArrayList<String> strings) {
                    Set<String> currentSet = new HashSet<>(strings);
                    Set<String> oldSet = groupPreferences.getStringSet(sharedPrefGroups,null);
                    updateLocalGroups(currentSet,oldSet);

                    emitter.onSuccess(new LiveDataResponse(true,"GROUPS","initReady"));

                    observeLastMessages();
                    observeLastEvents();
                    observeLastProjects();
                    dispose();
                }

                @Override
                public void onError(@NonNull Throwable e) {

                    emitter.onError(e);
                    dispose();
                }
            });

        });



    }

    public LiveData<LiveDataResponse> getGroupMembers(String groupName){
        return fireManager.getMembers(groupName);
    }

    public LiveData<LiveDataResponse> removeGroupMember(String groupName, String uidToRemove){
        return fireManager.removeGroupMember(groupName,uidToRemove);
    }

    private void observeLastMessages() {


        for(String group : getGroups()){

            fireManager.observeLastMessage(group)
                    .subscribe(new DisposableObserver<String>() {
                        @Override
                        public void onNext(@NonNull String s) {
                            if(s.equals("none")){
                                return;
                            }
                            if(s.equals(">>DELETED")){
                                groupDeleted(group);
                                leaveGroup(group);
                                return;
                            }
                            String lastLocalMessage = localManager.getLastGroupMessageID(group);
                            if(lastLocalMessage!=s) {
                                updateMessages(group);
                            }
                        }

                        @Override
                        public void onError(@NonNull Throwable e) {
                            if(isInternetAccessibleInit()){
                                observeLastMessages();
                            }
                            dispose();
                        }

                        @Override
                        public void onComplete() {
                            //will never complete
                        }
                    });

        }

    }

    private void observeLastEvents(){


        for(String group : getGroups()){

            fireManager.observeLastEvent(group)
                    .subscribe(new DisposableObserver<String>() {
                        @Override
                        public void onNext(@NonNull String s) {
                            if(s.equals("none")){
                                return;
                            }
                            if(s.equals(">>DELETED")){
                                groupDeleted(group);
                                leaveGroup(group);
                                return;
                            }
                            String lastLocalEvent = localManager.getLastGroupEventId(group);
                            if(lastLocalEvent!=s) {
                                updateEvents(group);
                            }
                        }

                        @Override
                        public void onError(@NonNull Throwable e) {
                            if(isInternetAccessibleInit()){
                                observeLastEvents();
                            }
                            dispose();
                        }

                        @Override
                        public void onComplete() {
                            //will never complete
                        }
                    });

        }

    }

    private void observeLastProjects(){

        for(String group : getGroups()){

            fireManager.observeLastProject(group)
                    .subscribe(new DisposableObserver<String>() {
                        @Override
                        public void onNext(@NonNull String s) {
                            if(s.equals("none")){
                                return;
                            }
                            if(s.equals(">>DELETED")){
                                groupDeleted(group);
                                leaveGroup(group);
                                return;
                            }
                            String lastLocalProject = localManager.getLastGroupProjectID(group);
                            if(lastLocalProject==null || !lastLocalProject.equals(s)) {
                                updateProjects(group);
                            }
                        }

                        @Override
                        public void onError(@NonNull Throwable e) {
                            if(isInternetAccessibleInit()){
                                observeLastProjects();
                            }
                            dispose();
                        }

                        @Override
                        public void onComplete() {
                            //will never complete
                        }
                    });

        }

    }


    private void updateMessages(String group){

        fireManager.getAllMessages(group).subscribe(new DisposableSingleObserver<ArrayList<Message>>() {
            @Override
            public void onSuccess(@NonNull ArrayList<Message> messages) {
                if(messages == null){
                    dispose();
                    return;
                }
                localManager.updateMessages(messages);
                dispose();
            }

            @Override
            public void onError(@NonNull Throwable e) {
                dispose();
            }
        });

    }

    private void updateEvents(String group){

        fireManager.getAllEvents(group).subscribe(new DisposableSingleObserver<ArrayList<CalendarEvent>>() {
            @Override
            public void onSuccess(@NonNull ArrayList<CalendarEvent> events) {
                if(events == null){
                    dispose();
                    return;
                }
                localManager.updateEvents(events);
                dispose();
            }

            @Override
            public void onError(@NonNull Throwable e) {
                dispose();
            }
        });

    }

    private void updateProjects(String group){

        fireManager.getAllProjects(group).subscribe(new DisposableSingleObserver<ArrayList<Project>>() {
            @Override
            public void onSuccess(@NonNull ArrayList<Project> projects) {
                if(projects == null){
                    dispose();
                    return;
                }
                localManager.updateProjects(group,projects);
                dispose();
            }

            @Override
            public void onError(@NonNull Throwable e) {
                dispose();
            }
        });

    }

    public void forceUpdateProject(String projectID){

        fireManager.getSingleProject(getCurrentGroup(),projectID)
                .subscribe(new DisposableSingleObserver<Project>() {
                    @Override
                    public void onSuccess(@NonNull Project project) {
                        localManager.forceUpdateProject(project);
                        dispose();
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        dispose();
                    }
                });

    }

    private void groupDeleted(String group){

        localManager.deleteAll(group);
        Set<String> set = groupPreferences.getStringSet(sharedPrefGroups,null);
        if(set != null){
            Set<String> editSet = new HashSet<>(set);
            editSet.remove(group);
            groupPreferences.edit().putStringSet(sharedPrefGroups,editSet).apply();

        }

    }




    public LiveData<List<Message>> getLiveMessages(String groupName){

        return localManager.getLiveMessages(groupName);

    }

    public LiveData<List<CalendarEvent>> getLiveEvents(String groupName){

        return localManager.getLiveEvents(groupName);

    }

    public LiveData<List<Project>> getLiveProjects(){

        return localManager.getLiveProjects(getCurrentGroup());

    }

    public List<CalendarEvent> getEventsForPeriod(String group, long from, long to){
        return localManager.getEventsForPeriod(group,from,to);
    }


    public LiveData<LiveDataResponse> sendMessage(String group, String messageContent){

        return fireManager.sendMessage(group, messageContent);

    }

    public LiveData<LiveDataResponse> createEvent(String group, long time, String title, String description){

        return fireManager.createEvent(group,time,title,description);

    }

    public ArrayList<String> getGroups(){

        Set<String> set = groupPreferences.getStringSet(sharedPrefGroups,null);
        if(set != null){
            return new ArrayList<>(set);
        }
        return new ArrayList<>();

    }

    public LiveData<LiveDataResponse> isUserReady(){

        if(isInternetAccessibleInit()){
            return fireManager.isUserReady();
        } else {
            MutableLiveData<LiveDataResponse> offlineResponse = new MutableLiveData<>();
            Set<String> set = userPreferences.getStringSet(sharedPrefUser,null);
            if(set == null){
                offlineResponse.postValue(new LiveDataResponse(false,"NOUSER",null));
            } else {
                if(getGroups().isEmpty()){
                    offlineResponse.postValue(new LiveDataResponse(false,"SETUP"));
                }else {
                    offlineResponse.postValue(new LiveDataResponse(true,"OFFLINE",null));
                }

            }
            return offlineResponse;

        }

    }

    public LiveData<Boolean> isUserAdminLive(String group){

        if(groupPreferences.contains(group+"Admin")){
            return new MutableLiveData<>(groupPreferences.getBoolean(group+"Admin",false));
        }
        return fireManager.isUserAdmin(group);

    }

    public Boolean isUserAdminLocal(String group){

        return groupPreferences.getBoolean(group+"Admin",false);

    }

    public Single<Boolean> isConversationOpen(String group){

        return Single.create(emitter -> {

            if(this.conversationOpen != null){
                emitter.onSuccess(this.conversationOpen);
            } else {
                fireManager.isConversationOpen(group)
                        .subscribe(new DisposableSingleObserver<Boolean>() {
                            @Override
                            public void onSuccess(@NonNull Boolean conversationOpen) {
                                BridgeManager.this.conversationOpen = conversationOpen;
                                emitter.onSuccess(conversationOpen);
                                dispose();
                            }

                            @Override
                            public void onError(@NonNull Throwable e) {
                                emitter.onError(e);
                                dispose();
                            }
                        });
            }

        });

    }

    public void saveAdminStatus(String group, Boolean status){
        groupPreferences.edit().putBoolean(group+"Admin",status).apply();
    }


    public String getCurrentGroup(){
        return this.currentGroup;
    }

    public LiveData<LiveDataResponse> getGroupSettings(){

        if(currentGroup == null){
            return new MutableLiveData<>(new LiveDataResponse(false,"NOGROUP"));
        }

        return fireManager.getGroupSettings(currentGroup);

    }

    private void updateLocalGroups(Set<String> currentSet, Set<String> oldSet){

        if(oldSet != null){
            Set<String> tempOld = new HashSet<>(oldSet);
            Set<String> tempNew = new HashSet<>(currentSet);
            tempNew.removeAll(tempOld);
            if(tempNew.size()>0){
                groupPreferences.edit().putString(sharedPrefNewGroup,(String) tempNew.toArray()[0]).apply();
            }

            oldSet.removeAll(currentSet);
            for(String group : oldSet) {
                groupDeleted(group);
            }
        }

        groupPreferences.edit().putStringSet(sharedPrefGroups,currentSet).apply();

    }

    public void setGroup(@Nullable String group){
        ArrayList<String> groups = getGroups();
        if(groups.isEmpty()){
            return;
        }
        if(group == null || !groups.contains(group)){
            String newGroup =
                    groupPreferences.getString(sharedPrefNewGroup,null);
            String lastGroup =
                    groupPreferences.getString(sharedPrefLastGroup,null);
            lastGroup = groups.contains(lastGroup) ? lastGroup : null;
            newGroup = groups.contains(newGroup) ? newGroup : null;
            if(newGroup != null){
                this.currentGroup = newGroup;
                groupPreferences.edit().putString(sharedPrefNewGroup,null).apply();
            } else {
                this.currentGroup = lastGroup == null ? groups.get(0) : lastGroup;
            }

        } else {
            this.currentGroup = group;
        }
        groupPreferences.edit().putString(sharedPrefLastGroup,this.currentGroup).apply();

    }

    public boolean groupAvailable(@Nullable String group){

        if(group == null){
            return false;
        }

        return getGroups().contains(group);

    }



    public LiveData<LiveDataResponse> changeGroupLockMode(Boolean lock){

        return fireManager.changeGroupLockMode(currentGroup, lock);

    }

    public LiveData<LiveDataResponse> changeGroupConversationMode(Boolean mode){

        return fireManager.changeGroupConversationMode(currentGroup, mode);

    }

    public LiveData<LiveDataResponse> changeGroupPassword(String password){
        return fireManager.changeGroupPassword(currentGroup,password);
    }

    public LiveData<LiveDataResponse> createGroup(String groupName, String groupPassword){

        return fireManager.createGroup(groupName, groupPassword);

    }

    public LiveData<LiveDataResponse> joinGroup(String groupName, String groupPassword){

        return fireManager.joinGroup(groupName, groupPassword);

    }

    public LiveData<LiveDataResponse> createProject(String title, String content, ArrayList<String> membersUIDs){

        return fireManager.createProject(currentGroup,title,content,membersUIDs);

    }

    public LiveData<LiveDataResponse> deleteProject(String group, String projectID){

        return fireManager.deleteProject(group,projectID);

    }

    public LiveData<LiveDataResponse> editProjectMembers(String group, String projectID, ArrayList<String> newMembers){

        return fireManager.editProjectMembers(group, projectID, newMembers);

    }

    public LiveData<LiveDataResponse> reLogin(String password){

        return fireManager.reLogin(password);

    }

    public LiveData<LiveDataResponse> changeAccountPassword(String passwordNew){

        return fireManager.changeAccountPassword(passwordNew);

    }

    public void updateGroupProjects(String group){
        updateProjects(group);
    }

    public String getOwnUID(){
        return fireManager.getOwnUID();
    }

    public Pair<String,String> getOwnUsernameEmail(){

        Set<String> set = userPreferences.getStringSet(sharedPrefUser,null);
        String username = null;
        String email = null;
        if(set != null){
            for(String s : set){
                if(s.contains("EMAIL:"))
                    email = s.split(":")[1];
                if(s.contains("USERNAME:"))
                    username = s.split(":")[1];
            }
        }
        return new Pair<>(username,email);

    }
    public void logout(){
        fireManager.logout();
        groupPreferences.edit().clear().apply();
        userPreferences.edit().clear().apply();
        localManager.purge();
    }

    public LiveData<LiveDataResponse> leaveGroup(String group){

        MutableLiveData<LiveDataResponse> response = new MutableLiveData<>();

        fireManager.leaveGroup(group).subscribe(new DisposableSingleObserver<LiveDataResponse>() {
            @Override
            public void onSuccess(@NonNull LiveDataResponse liveDataResponse) {
                if(liveDataResponse.isSuccessful()){
                    updateGroupsData().subscribe(new DisposableSingleObserver<LiveDataResponse>() {
                        @Override
                        public void onSuccess(@NonNull LiveDataResponse liveDataResponseInternal) {
                            response.postValue(liveDataResponse);
                            setGroup(null);
                            dispose();
                        }

                        @Override
                        public void onError(@NonNull Throwable e) {
                            response.postValue(new LiveDataResponse(false,"Something went wrong, try restarting the app"));
                            dispose();
                        }
                    });

                }

            }

            @Override
            public void onError(@NonNull Throwable e) {
                response.postValue(new LiveDataResponse(false,e.getMessage()));
                dispose();
            }
        });

        return response;

    }

    public LiveData<LiveDataResponse> deleteGroup(String group){

        MutableLiveData<LiveDataResponse> response = new MutableLiveData<>();

        fireManager.deleteGroup(group).subscribe(new DisposableSingleObserver<LiveDataResponse>() {
            @Override
            public void onSuccess(@NonNull LiveDataResponse liveDataResponse) {
                response.postValue(liveDataResponse);
            }

            @Override
            public void onError(@NonNull Throwable e) {
                response.postValue(new LiveDataResponse(false,e.getMessage()));
            }
        });

        return response;

    }

    public LiveData<LiveDataResponse> deleteAccount(){

        MutableLiveData<LiveDataResponse> response = new MutableLiveData<>();

        fireManager.deleteAccount().subscribe(new DisposableSingleObserver<LiveDataResponse>() {
            @Override
            public void onSuccess(@NonNull LiveDataResponse liveDataResponse) {
                response.postValue(liveDataResponse);
                dispose();
            }

            @Override
            public void onError(@NonNull Throwable e) {
                response.postValue(new LiveDataResponse(false,e.getMessage()));
                dispose();
            }
        });

        return response;

    }





}
