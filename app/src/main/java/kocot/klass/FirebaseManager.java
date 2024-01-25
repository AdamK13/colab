package kocot.klass;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;

import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;

import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.functions.HttpsCallableResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import kocot.klass.structures.CalendarEvent;
import kocot.klass.auxiliary.LiveDataResponse;
import kocot.klass.structures.Message;
import kocot.klass.structures.Project;
import kocot.klass.structures.User;

public final class FirebaseManager {


    private static final String GROUPS = "Groups";
    private static final String USERS = "Users";
    private static final String PROJECTS = "Projects";

    private static FirebaseManager instance;
    private FirebaseAuth fireAuth;
    private FirebaseFirestore fireStore;
    private FirebaseDatabase fireRealtime;
    private FirebaseFunctions fireFunctions;

    private FirebaseManager(){
        fireAuth = FirebaseAuth.getInstance();
        fireStore = FirebaseFirestore.getInstance();
        fireRealtime = FirebaseDatabase.getInstance("https://klass-kocot-default-rtdb.europe-west1.firebasedatabase.app");
        fireFunctions = FirebaseFunctions.getInstance("europe-central2");
    }

    public static FirebaseManager getInstance(){

        if(instance == null){
            instance = new FirebaseManager();
        }

        return instance;

    }


    public void sendEmailVerification(){

        if(fireAuth.getCurrentUser() != null)
            fireAuth.getCurrentUser().sendEmailVerification();

    }

    public boolean emailVerified(){

        if(fireAuth.getCurrentUser() != null)
            return fireAuth.getCurrentUser().isEmailVerified();

        return false;

    }

    public LiveData<LiveDataResponse> isUserReady() {

        MutableLiveData<LiveDataResponse> loggedInResult = new MutableLiveData<>();


        if(fireAuth.getCurrentUser() == null){
            loggedInResult.postValue(new LiveDataResponse(false,"NOUSER",null));
            return loggedInResult;
        }

        fireAuth.getCurrentUser().reload().addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if(task.getException() == null){
                    loggedInResult.postValue(new LiveDataResponse(true,"HOLD",null));
                    checkGroups(loggedInResult);
                } else {
                    logout();
                    loggedInResult.postValue(new LiveDataResponse(false,"NOUSER_RELOAD",task.getException()));


                }
            }
        });



        return loggedInResult;

    }

    private void checkGroups(MutableLiveData<LiveDataResponse> resultContainer){

        FirebaseUser user = fireAuth.getCurrentUser();

        if(user == null || !user.isEmailVerified()){
            resultContainer.postValue(new LiveDataResponse(false,"NOUSER"));
            return;
        }

        fireStore.collection(USERS).document(fireAuth.getUid())
                .get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        List<String> result = (List<String>) task.getResult().get("groups");
                        if(result != null && !result.isEmpty()){

                            resultContainer.postValue(new LiveDataResponse(true,"READY",null));

                        } else {

                            resultContainer.postValue(new LiveDataResponse(false,"SETUP",null));

                        }

                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        resultContainer.postValue(new LiveDataResponse(false,"NOUSER"));
                    }
                });

    }


    public void logout(){

        fireAuth.signOut();

    }

    public void resetPassword(String email){

        fireAuth.sendPasswordResetEmail(email);

    }


    public LiveData<LiveDataResponse> login(String email, String password){

        logout();


        MutableLiveData<LiveDataResponse> loginState = new MutableLiveData<>();
        LiveDataResponse response = new LiveDataResponse();

        fireAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {

                if(task.isSuccessful()){
                    response.setResponse(true," Signed in successfully ");
                    loginState.postValue(response);

                } else{

                    response.setResponse(false,task.getException().getMessage());
                    loginState.postValue(response);
                }

            }
        });

        return loginState;

    }




    public LiveData<LiveDataResponse> register(String email, String name, String password){

        MutableLiveData<LiveDataResponse> response = new MutableLiveData<>();
        logout();
        Map<String, Object> data = new HashMap<>();
        data.put("name", name);
        data.put("email",email);
        fireAuth.createUserWithEmailAndPassword(email,password)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if(!task.isComplete() || !task.isSuccessful()) {
                            response.postValue(new LiveDataResponse(false, task.getException().getMessage()));
                            return;
                        }
                        fireFunctions.getHttpsCallable("createUser").call(data)
                                .addOnCompleteListener(new OnCompleteListener<HttpsCallableResult>() {
                                    @Override
                                    public void onComplete(@NonNull Task<HttpsCallableResult> task) {
                                        if(!task.isComplete() || !task.isSuccessful()){
                                            fireAuth.getCurrentUser().delete();
                                            response.postValue(new LiveDataResponse(false,task.getException().getMessage()));
                                            return;
                                        }

                                        Map<String,Object> result = (Map<String, Object>) task.getResult().getData();
                                        Boolean successful = (Boolean) result.get("successful");
                                        if(successful != null && successful){
                                            response.postValue(new LiveDataResponse(true,null));
                                        } else {
                                            fireAuth.getCurrentUser().delete();
                                            String reason = (String) result.get("reason");
                                            response.postValue(new LiveDataResponse(false,reason));
                                        }

                                    }
                                });
                    }
                });


        return response;

    }




    private Task<Map<String, Object>> createGroupBackend(String groupName, String groupPassword){


        Map<String, Object> data = new HashMap<>();
        data.put("groupName", groupName);
        data.put("groupPassword",groupPassword);

        return fireFunctions.getHttpsCallable("createGroup")
                .call(data)
                .continueWith(new Continuation<HttpsCallableResult, Map<String, Object>>() {
                    @Override
                    public Map<String,Object> then(@NonNull Task<HttpsCallableResult> task) throws Exception {
                        Map data = (Map<String,Object>) task.getResult().getData();
                        return data;

                    }
                });
    }

    public LiveData<LiveDataResponse> createGroup(String groupName, String groupPassword){

        MutableLiveData<LiveDataResponse> response = new MutableLiveData<>();

        createGroupBackend(groupName,groupPassword).addOnCompleteListener(new OnCompleteListener<Map<String,Object>>() {
            @Override
            public void onComplete(@NonNull Task<Map<String,Object>> task) {
                Boolean successful = (Boolean) task.getResult().get("successful");
                if(successful){
                    response.postValue(new LiveDataResponse(true,null));
                } else {
                    Object reason =  task.getResult().get("reason");
                    response.postValue(new LiveDataResponse(false,reason.toString()));
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                response.postValue(new LiveDataResponse(false,e.getMessage()));
            }
        });


        return response;
    }

    private Task<Map<String, Object>> joinGroupBackend(String groupName, @Nullable String groupPassword){

        Map<String,Object> data = new HashMap<>();
        data.put("groupName",groupName);
        data.put("groupPassword",groupPassword);

        return fireFunctions.getHttpsCallable("joinGroup")
                .call(data)
                .continueWith(new Continuation<HttpsCallableResult, Map<String, Object>>() {
                    @Override
                    public Map<String, Object> then(@NonNull Task<HttpsCallableResult> task) throws Exception {
                        Map<String, Object> data = (Map<String, Object>) task.getResult().getData();
                        return data;
                    }
                });

    }

    public LiveData<LiveDataResponse> joinGroup(String groupName, @Nullable String groupPassword){

        MutableLiveData<LiveDataResponse> response = new MutableLiveData<>();

        joinGroupBackend(groupName,groupPassword).addOnCompleteListener(new OnCompleteListener<Map<String, Object>>() {
            @Override
            public void onComplete(@NonNull Task<Map<String, Object>> task) {
                Map<String, Object> result = task.getResult();
                Boolean successful = (Boolean) result.get("successful");
                if(successful){
                    response.postValue(new LiveDataResponse(true,null));
                } else {
                    String reason = (String) result.get("reason");
                    response.postValue(new LiveDataResponse(false,reason));
                }

            }
        });

        return response;
    }

    private Task<Map<String,Object>> getMembersBackend(String groupName){

        Map<String, Object> data = new HashMap<>();
        data.put("group",groupName);

        return fireFunctions.getHttpsCallable("getGroupMembers")
                .call(data).continueWith(new Continuation<HttpsCallableResult, Map<String, Object>>() {
                    @Override
                    public Map<String, Object> then(@NonNull Task<HttpsCallableResult> task) throws Exception {

                        Map<String, Object> result = (Map<String, Object>) task.getResult().getData();
                        return result;

                    }
                });

    }

    public LiveData<LiveDataResponse> getMembers(String groupName){

        MutableLiveData<LiveDataResponse> response = new MutableLiveData<>();

        getMembersBackend(groupName).addOnCompleteListener(new OnCompleteListener<Map<String, Object>>() {
            @Override
            public void onComplete(@NonNull Task<Map<String, Object>> task) {
                try {
                    Map<String, Object> result = task.getResult();
                    Boolean successful = (Boolean) result.get("successful");
                    if (successful) {
                        ArrayList<HashMap<String, String>> members = (ArrayList<HashMap<String, String>>) result.get("members");
                        response.postValue(new LiveDataResponse(true, null, members));
                    } else {
                        String reason = (String) result.get("reason");
                        response.postValue(new LiveDataResponse(false, reason));
                    }
                } catch (Exception e){
                    response.postValue(new LiveDataResponse(false, e.getMessage()));
                }
            }
        });

        return response;

    }

    private Task<Map<String, Object>> isConversationOpenBackend(String group){

        Map<String, Object> data = new HashMap<>();
        data.put("group",group);

        return fireFunctions.getHttpsCallable("isConversationOpen")
                .call(data).continueWith(new Continuation<HttpsCallableResult, Map<String, Object>>() {
                    @Override
                    public Map<String, Object> then(@NonNull Task<HttpsCallableResult> task) throws Exception {
                        return (Map<String, Object>) task.getResult().getData();
                    }
                });

    }

    public Single<Boolean> isConversationOpen(String group){

        return Single.create(emitter -> {
            isConversationOpenBackend(group).addOnCompleteListener(new OnCompleteListener<Map<String, Object>>() {
                @Override
                public void onComplete(@NonNull Task<Map<String, Object>> task) {
                    try{
                        Map<String, Object> result = task.getResult();
                        Boolean successful = (Boolean) result.get("successful");
                        if(successful != null && successful){
                            Boolean conversationOpen = (Boolean) result.get("result");
                            emitter.onSuccess(conversationOpen);
                        } else {
                            String reason = (String) result.get("reason");
                            emitter.onError(new Throwable(reason));
                        }

                    } catch (Exception e){
                        emitter.onError(e);
                    }
                }
            });
        });




    }

    private Task<Map<String, Object>> removeGroupMemberBackend(String groupName, String uidToRemove){

        Map<String, Object> data = new HashMap<>();
        data.put("group", groupName);
        data.put("uidToRemove", uidToRemove);


        return fireFunctions.getHttpsCallable("removeGroupMember")
                .call(data).continueWith(new Continuation<HttpsCallableResult, Map<String, Object>>() {
                    @Override
                    public Map<String, Object> then(@NonNull Task<HttpsCallableResult> task) throws Exception {
                        Map<String, Object> result = (Map<String, Object>) task.getResult().getData();
                        return result;
                    }
                });

    }

    public LiveData<LiveDataResponse> removeGroupMember(String groupName, String uidToRemove){

        MutableLiveData<LiveDataResponse> response = new MutableLiveData<>();

        removeGroupMemberBackend(groupName,uidToRemove).addOnCompleteListener(new OnCompleteListener<Map<String, Object>>() {
            @Override
            public void onComplete(@NonNull Task<Map<String, Object>> task) {
                Map<String, Object> result = task.getResult();
                Boolean successful = (Boolean) result.get("successful");
                if(successful){
                    response.postValue(new LiveDataResponse(true,null));
                } else {
                    String reason = (String) result.get("reason");
                    response.postValue(new LiveDataResponse(false, reason));
                }

            }
        });

        return response;

    }



    public Single<ArrayList<Message>> getAllMessages(String groupName){

        return Single.create(emitter -> {

            fireStore.collection("Groups").document(groupName)
                    .collection("Messages").get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                            if (task.isSuccessful()) {
                                if (!task.getResult().isEmpty()) {
                                    ArrayList<Message> messages = new ArrayList<>();

                                    for (QueryDocumentSnapshot document : task.getResult()) {

                                        Map<String, Object> rawMessage = document.getData();
                                        rawMessage.put("messageID",document.getId());
                                        messages.add(new Message(rawMessage));

                                    }
                                    emitter.onSuccess(messages);

                                } else {
                                    emitter.onSuccess(null);
                                }
                            } else {
                                emitter.onError(task.getException());
                            }
                        }
                    });
        });

    }

    public Single<ArrayList<CalendarEvent>> getAllEvents(String groupName){

        return Single.create(emitter -> {

            fireStore.collection("Groups").document(groupName)
                    .collection("Events").get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                            if(task.isSuccessful()){
                                if(!task.getResult().isEmpty()){
                                    ArrayList<CalendarEvent> events = new ArrayList<>();

                                    for (QueryDocumentSnapshot document : task.getResult()){

                                        Map<String,Object> rawEvent = document.getData();
                                        rawEvent.put("eventID",document.getId());
                                        events.add(new CalendarEvent(rawEvent));

                                    }
                                    emitter.onSuccess(events);
                                } else {
                                    emitter.onSuccess(null);
                                }
                            }else {
                                emitter.onError(task.getException());
                            }
                        }
                    });

        });

    }

    public Single<ArrayList<Project>> getAllProjects(String groupName){

        return Single.create(emitter -> {

            fireStore.collection("Groups").document(groupName)
                    .collection("Projects").get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<QuerySnapshot> task) {

                            if(task.isSuccessful()){
                                if(task.getResult() != null) {
                                    ArrayList<Project> projects = new ArrayList<>();

                                    for (QueryDocumentSnapshot document : task.getResult()) {

                                        Map<String, Object> rawProject = document.getData();
                                        rawProject.put("projectID", document.getId());
                                        projects.add(new Project(rawProject));

                                    }
                                    emitter.onSuccess(projects);
                                } else {
                                    emitter.onSuccess(null);
                                }
                            } else {
                                emitter.onError(task.getException());
                            }
                        }
                    });

        });

    }

    private Task<Map<String,Object>> sendMessageBackend(String groupName, String content){

        Map<String, Object> data = new HashMap<>();
        data.put("group", groupName);
        data.put("message",content);

        return fireFunctions.getHttpsCallable("sendMessage")
                .call(data).continueWith(new Continuation<HttpsCallableResult, Map<String, Object>>() {
                    @Override
                    public Map<String, Object> then(@NonNull Task<HttpsCallableResult> task) throws Exception {
                        return (Map<String, Object>) task.getResult().getData();
                    }
                });

    }

    public LiveData<LiveDataResponse> sendMessage(String groupName, String messageContent){

        MutableLiveData<LiveDataResponse> response = new MutableLiveData<>();

        sendMessageBackend(groupName,messageContent)
                .addOnCompleteListener(new OnCompleteListener<Map<String, Object>>() {
                    @Override
                    public void onComplete(@NonNull Task<Map<String, Object>> task) {
                        try {
                            Map map = task.getResult();

                            Boolean successful = (Boolean) map.get("successful");
                            String messageID = (String) map.get("messageID");

                            if(successful != null && successful && messageID != null){
                                response.postValue(new LiveDataResponse(true,"Message sent"));
                            } else {

                                response.postValue(new LiveDataResponse(false,"Failed to send message"));

                            }
                        }catch (Exception e){
                            response.postValue(new LiveDataResponse(false, e.getMessage().toString()));
                        }
                    }
                });

        return response;

    }


    private Task<Map<String, Object>> createEventBackend(String group, long time, String title, String content){

        Map<String, Object> data = new HashMap<>();
        data.put("group", group);
        data.put("timestamp", time);
        data.put("title", title);
        data.put("content", content);

        return fireFunctions.getHttpsCallable("createEvent")
                .call(data).continueWith(new Continuation<HttpsCallableResult, Map<String, Object>>() {
                    @Override
                    public Map<String, Object> then(@NonNull Task<HttpsCallableResult> task) throws Exception {
                        return (Map<String, Object>) task.getResult().getData();
                    }
                });

    }

    public LiveData<LiveDataResponse> createEvent(String group, long time, String title, String description){

        MutableLiveData<LiveDataResponse> response = new MutableLiveData<>();

        createEventBackend(group,time,title,description)
                .addOnCompleteListener(new OnCompleteListener<Map<String, Object>>() {
                    @Override
                    public void onComplete(@NonNull Task<Map<String, Object>> task) {

                        try {

                            Map map = task.getResult();
                            Boolean successful = (Boolean) map.get("successful");
                            String eventID = (String) map.get("eventID");

                            if(successful != null && successful && eventID != null){
                                response.postValue(new LiveDataResponse(true,"Event created"));
                            } else {

                                response.postValue(new LiveDataResponse(false,"Failed to create event"));

                            }

                        } catch (Exception e){

                            response.postValue(new LiveDataResponse(false, e.getMessage().toString()));

                        }


                    }
                });

        return response;

    }

    private Task<Map<String, Object>> createProjectBackend(String group, String title, String content, ArrayList<String> membersUIDs){

        Map<String,Object> data = new HashMap<>();
        data.put("group", group);
        data.put("title",title);
        data.put("members",membersUIDs);
        data.put("content",content);

        return fireFunctions.getHttpsCallable("createProject")
                .call(data).continueWith(new Continuation<HttpsCallableResult, Map<String, Object>>() {
                    @Override
                    public Map<String, Object> then(@NonNull Task<HttpsCallableResult> task) throws Exception {
                        return (Map<String, Object>) task.getResult().getData();
                    }
                });

    }

    public LiveData<LiveDataResponse> createProject(String group, String title, String content, ArrayList<String> membersUIDs){

        MutableLiveData<LiveDataResponse> response = new MutableLiveData<>();

        createProjectBackend(group,title,content,membersUIDs)
                .addOnCompleteListener(new OnCompleteListener<Map<String, Object>>() {
                    @Override
                    public void onComplete(@NonNull Task<Map<String, Object>> task) {
                        Map<String, Object> result = task.getResult();
                        Boolean successful = (Boolean) result.get("successful");
                        if(successful != null && successful){
                            response.postValue(new LiveDataResponse(true,null));
                        } else {
                            String reason = (String) result.get("reason");
                            response.postValue(new LiveDataResponse(false,reason));
                        }
                    }
                });

        return response;

    }

    private Task<Map<String, Object>> deleteProjectBackend(String group, String projectID){

        Map<String, Object> data = new HashMap<>();
        data.put("group", group);
        data.put("projectID",projectID);

        return fireFunctions.getHttpsCallable("deleteProject")
                .call(data).continueWith(new Continuation<HttpsCallableResult, Map<String, Object>>() {
                    @Override
                    public Map<String, Object> then(@NonNull Task<HttpsCallableResult> task) throws Exception {
                        return (Map<String, Object>) task.getResult().getData();
                    }
                });

    }

    public LiveData<LiveDataResponse> deleteProject(String group, String projectID){

        MutableLiveData<LiveDataResponse> response = new MutableLiveData<>();

        deleteProjectBackend(group,projectID).addOnCompleteListener(new OnCompleteListener<Map<String, Object>>() {
            @Override
            public void onComplete(@NonNull Task<Map<String, Object>> task) {
                if(!task.isSuccessful() || !task.isComplete()){
                    response.postValue(new LiveDataResponse(false,"Something went wrong"));
                    return;
                }
                Boolean successful = (Boolean) task.getResult().get("successful");
                if(successful!=null && successful){
                    response.postValue(new LiveDataResponse(true,null));

                } else {
                    String reason = (String) task.getResult().get("reason");
                    response.postValue(new LiveDataResponse(false,reason));
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                response.postValue(new LiveDataResponse(false,"Something went wrong"));
            }
        });

        return response;

    }

    private Task<Map<String, Object>> editProjectMembersBackend(String group, String projectID, ArrayList<String> newMembers){

        Map<String, Object> data = new HashMap<>();
        data.put("group",group);
        data.put("projectID",projectID);
        data.put("newMembers",newMembers);

        return fireFunctions.getHttpsCallable("editProjectMembers")
                .call(data).continueWith(new Continuation<HttpsCallableResult, Map<String, Object>>() {
                    @Override
                    public Map<String, Object> then(@NonNull Task<HttpsCallableResult> task) throws Exception {
                        return (Map<String, Object>) task.getResult().getData();
                    }
                });

    }

    public LiveData<LiveDataResponse> editProjectMembers(String group, String projectID, ArrayList<String> newMembers){

        MutableLiveData<LiveDataResponse> response = new MutableLiveData<>();

        editProjectMembersBackend(group,projectID,newMembers)
                .addOnCompleteListener(new OnCompleteListener<Map<String, Object>>() {
                    @Override
                    public void onComplete(@NonNull Task<Map<String, Object>> task) {
                        if(!task.isSuccessful() || !task.isComplete()){
                            response.postValue(new LiveDataResponse(false, "Something went wrong"));
                            return;
                        }
                        Boolean successful = (Boolean) task.getResult().get("successful");
                        if(successful != null && successful){
                            response.postValue(new LiveDataResponse(true,null));
                        } else {
                            String reason = (String) task.getResult().get("reason");
                            response.postValue(new LiveDataResponse(false,reason));
                        }
                    }
                });

        return response;
    }

    public Single<Project> getSingleProject(String group, String projectID){

        return Single.create(emitter -> {

            fireStore.collection(GROUPS).document(group).collection(PROJECTS).document(projectID)
                    .get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                            DocumentSnapshot snapshot = task.getResult();
                            Map<String, Object> rawProject = (Map<String, Object>) snapshot.toObject(Object.class) ;
                            Project project = new Project(rawProject);
                            project.setProjectID(snapshot.getId());
                            emitter.onSuccess(project);
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            emitter.onError(e);
                        }
                    });

        });

    }

    public boolean isUserSignedIn(){
        return fireAuth.getUid() != null;
    }

    public Single<User> getCurrentUser(){



        return Single.create(emitter -> {

            if(fireAuth.getUid() == null){
                emitter.onError(new Throwable("No user"));
                return;
            }

            fireStore.collection("Users").document(fireAuth.getUid())
                    .get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                            DocumentSnapshot snapshot = task.getResult();
                            User user = snapshot.toObject(User.class);
                            emitter.onSuccess(user);
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            emitter.onError(e);
                        }
                    });

                    }

         );

    }

    public Observable<String> observeLastMessage(String groupName){

        return Observable.create(emitter -> {

            fireRealtime.getReference(groupName+"//MSG")
                    .addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            if(snapshot.getValue()==null){
                                emitter.onNext("gone");
                            } else {
                                emitter.onNext(snapshot.getValue().toString());
                            }

                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            emitter.onError(error.toException());
                        }
                    });

        });

    }

    public  Observable<String> observeLastEvent(String groupName){

        return Observable.create(emitter -> {

            fireRealtime.getReference(groupName+"//EVT")
                    .addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            if(snapshot.getValue()==null){
                                emitter.onNext("gone");
                            } else {
                                emitter.onNext(snapshot.getValue().toString());
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            emitter.onError(error.toException());
                        }
                    });


        });

    }

    public Observable<String> observeLastProject(String groupName){

        return Observable.create(emitter -> {

            fireRealtime.getReference(groupName+"//PRO")
                    .addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            if(snapshot.getValue()==null){
                                emitter.onNext("gone");
                            } else {
                                emitter.onNext(snapshot.getValue().toString());
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                                emitter.onError(error.toException());
                        }
                    });

        });

    }

    public Single<String> getLastMessage(String groupName){

        return Single.create(emitter -> {

            fireRealtime.getReference(groupName).get()
                    .addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<DataSnapshot> task) {

                            emitter.onSuccess(task.getResult().getValue().toString());

                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {

                            emitter.onError(e);

                        }
                    });

        });
    }

    public Single<ArrayList<String>> getUserGroups(){

        return Single.create(emitter -> {

            fireStore.collection(USERS).document(fireAuth.getUid()).get()
                    .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                            Object taskResult = task.getResult().get("groups");
                            ArrayList<String> list = new ArrayList<>((List)taskResult);

                            emitter.onSuccess(list);


                        }

                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {

                            emitter.onError(e);

                        }
                    });

        });


    }


    public LiveData<Boolean> isUserAdmin(String groupName){

        MutableLiveData<Boolean> response = new MutableLiveData<>();

        Map<String, Object> data = new HashMap<>();
        data.put("group", groupName);

        fireFunctions.getHttpsCallable("isUserAdmin").call(data)
                .addOnCompleteListener(new OnCompleteListener<HttpsCallableResult>() {
                    @Override
                    public void onComplete(@NonNull Task<HttpsCallableResult> task) {
                        Map<String, Object> resultData = (Map<String, Object>) task.getResult().getData();
                        Boolean isAdmin = (Boolean) resultData.get("isAdmin");
                        response.postValue(isAdmin);
                    }
                });



        return response;

    }


    public LiveData<LiveDataResponse> getGroupSettings(String groupName) {

        MutableLiveData<LiveDataResponse> response = new MutableLiveData<>();

        Map<String, Object> data = new HashMap<>();
        data.put("group", groupName);


        fireFunctions.getHttpsCallable("getGroupSettings").call(data)
                .addOnCompleteListener(new OnCompleteListener<HttpsCallableResult>() {
                    @Override
                    public void onComplete(@NonNull Task<HttpsCallableResult> task) {
                        try {

                            Map<String, Object> resultData = (Map<String, Object>) task.getResult().getData();
                            if(resultData == null){
                                response.postValue(new LiveDataResponse(false,"ERROR"));
                                return;
                            }
                            Boolean valid = (Boolean) resultData.get("successful");

                            if(valid != null && valid){
                                resultData.remove("successful");
                                response.postValue(new LiveDataResponse(true,"SUCCESSFUL",resultData));
                            } else {
                                response.postValue(new LiveDataResponse(false,"401"));
                            }

                        } catch (Exception e){
                            response.postValue(new LiveDataResponse(false,"Could not reach server"));
                        }

                    }
                });



        return response;
    }

    public LiveData<LiveDataResponse> changeGroupLockMode(String groupName, Boolean lock){

        MutableLiveData<LiveDataResponse> response = new MutableLiveData<>();

        Map<String, Object> data = new HashMap<>();
        data.put("group",groupName);
        data.put("lock",lock);

        fireFunctions.getHttpsCallable("setGroupLockMode").call(data)
                .addOnCompleteListener(new OnCompleteListener<HttpsCallableResult>() {
                    @Override
                    public void onComplete(@NonNull Task<HttpsCallableResult> task) {
                        Map<String, Object> resultData = (Map<String, Object>) task.getResult().getData();
                        if(resultData == null){
                            response.postValue(new LiveDataResponse(false, "ERROR"));
                            return;
                        }
                        Boolean success = (Boolean) resultData.get("successful");
                        if(success != null && success){
                            response.postValue(new LiveDataResponse(true,"SUCCESSFUL"));
                        } else {
                            response.postValue(new LiveDataResponse(false,"UNAUTHORIZED OR INTERNAL ERROR"));
                        }
                    }
                });

        return response;
    }
    public LiveData<LiveDataResponse> changeGroupConversationMode(String groupName, Boolean mode){

        MutableLiveData<LiveDataResponse> response = new MutableLiveData<>();

        Map<String, Object> data = new HashMap<>();
        data.put("group",groupName);
        if(mode){
            data.put("mode","OWNER");
        } else {
            data.put("mode", "OPEN");
        }

        fireFunctions.getHttpsCallable("setGroupConversationMode").call(data)
                .addOnCompleteListener(new OnCompleteListener<HttpsCallableResult>() {
                    @Override
                    public void onComplete(@NonNull Task<HttpsCallableResult> task) {
                        Map<String, Object> resultData = (Map<String, Object>) task.getResult().getData();
                        if(resultData == null){
                            response.postValue(new LiveDataResponse(false, "ERROR"));
                            return;
                        }
                        Boolean success = (Boolean) resultData.get("successful");
                        if(success != null && success){
                            response.postValue(new LiveDataResponse(true,"SUCCESSFUL"));
                        } else {
                            response.postValue(new LiveDataResponse(false,"UNAUTHORIZED OR INTERNAL ERROR"));
                        }
                    }
                });

        return response;

    }

    public LiveData<LiveDataResponse> changeGroupPassword(String groupName, String password){

        MutableLiveData<LiveDataResponse> response = new MutableLiveData<>();

        Map<String, Object> data = new HashMap<>();
        data.put("group",groupName);
        data.put("groupPassword",password);

        fireFunctions.getHttpsCallable("setGroupPassword").call(data)
                .addOnCompleteListener(new OnCompleteListener<HttpsCallableResult>() {
                    @Override
                    public void onComplete(@NonNull Task<HttpsCallableResult> task) {
                        Map<String,Object> resultData = (Map<String, Object>) task.getResult().getData();
                        if(resultData == null){
                            response.postValue(new LiveDataResponse(false,"ERROR"));
                            return;
                        }
                        Boolean successful = (Boolean) resultData.get("successful");
                        if(successful){
                            response.postValue(new LiveDataResponse(true,null));
                        } else {
                            String reason = (String) resultData.get("reason");
                            response.postValue(new LiveDataResponse(false,reason));
                        }
                    }
                });

        return response;

    }

    public Single<LiveDataResponse> leaveGroup(String groupName){

        Map<String, Object> data = new HashMap<>();
        data.put("group", groupName);

        return Single.create(emitter -> {

            fireFunctions.getHttpsCallable("leaveGroup").call(data)
                    .addOnCompleteListener(new OnCompleteListener<HttpsCallableResult>() {
                        @Override
                        public void onComplete(@NonNull Task<HttpsCallableResult> task) {
                            if(!task.isComplete() || !task.isSuccessful()){
                                emitter.onError(task.getException());
                                return;
                            }
                            Map<String, Object> result = (Map<String, Object>) task.getResult().getData();
                            Boolean successful = (Boolean) result.get("successful");
                            if(successful != null && successful){
                                emitter.onSuccess(new LiveDataResponse(true,null));
                            } else {
                                String reason = (String) result.get("reason");
                                emitter.onSuccess(new LiveDataResponse(false,reason));
                            }

                        }
                    });

        });

    }

    public Single<LiveDataResponse> deleteGroup(String groupName){

        Map<String, Object> data = new HashMap<>();
        data.put("group",groupName);

        return Single.create(emitter -> {

            fireFunctions.getHttpsCallable("deleteGroup").call(data)
                    .addOnCompleteListener(new OnCompleteListener<HttpsCallableResult>() {
                        @Override
                        public void onComplete(@NonNull Task<HttpsCallableResult> task) {
                            if(!task.isComplete() || !task.isSuccessful()){
                                emitter.onError(task.getException());
                                return;
                            }
                            Map<String, Object> result = (Map<String, Object>) task.getResult().getData();
                            Boolean successful = (Boolean) result.get("successful");
                            if(successful != null && successful){
                                emitter.onSuccess(new LiveDataResponse(true,null));
                            } else {
                                String reason = (String) result.get("reason");
                                emitter.onSuccess(new LiveDataResponse(false,reason));
                            }
                        }
                    });

        });

    }

    public LiveData<LiveDataResponse> changeAccountPassword( String newPassword){

        MutableLiveData<LiveDataResponse> response = new MutableLiveData<>();

        fireAuth.getCurrentUser().updatePassword(newPassword).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if(task.isComplete() && task.isSuccessful()){
                    response.postValue(new LiveDataResponse(true,null));
                } else {
                    response.postValue(new LiveDataResponse(false,"Something went wrong"));
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {

                response.postValue(new LiveDataResponse(false,e.getMessage()));

            }
        });

        return response;

    }

    public LiveData<LiveDataResponse> reLogin(String password){

        MutableLiveData<LiveDataResponse> response = new MutableLiveData<>();

        AuthCredential credential = EmailAuthProvider.getCredential(fireAuth.getCurrentUser().getEmail(),password);
        fireAuth.getCurrentUser().reauthenticate(credential).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if(task.isComplete() && task.isSuccessful()){
                    response.postValue(new LiveDataResponse(true,null));
                } else {
                    response.postValue(new LiveDataResponse(false,task.getException().getMessage()));
                }
            }
        });

        return response;

    }

    public Single<LiveDataResponse> deleteAccount(){

        return Single.create(emitter -> {

            fireFunctions.getHttpsCallable("deleteAccount").call()
                    .addOnCompleteListener(new OnCompleteListener<HttpsCallableResult>() {
                        @Override
                        public void onComplete(@NonNull Task<HttpsCallableResult> task) {
                            Map<String, Object> result = (Map<String, Object>) task.getResult().getData();
                            Boolean successful = (Boolean) result.get("successful");
                            if(successful != null && successful){
                                emitter.onSuccess(new LiveDataResponse(true,null));
                            } else {
                                String reason = (String) result.get("reason");
                                emitter.onError(new Throwable(reason));
                            }
                        }
                    });

        });

    }

    public String getOwnUID(){
        return fireAuth.getUid();
    }
}
