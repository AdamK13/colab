package kocot.klass.app;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import kocot.klass.BridgeManager;
import kocot.klass.FirebaseManager;
import kocot.klass.auxiliary.LiveDataResponse;


public class LauncherActivityModel extends AndroidViewModel {

    private FirebaseManager fireManager;
    private BridgeManager bridgeManager;

    public LauncherActivityModel(Application application){

        super(application);

        fireManager = FirebaseManager.getInstance();
        bridgeManager = BridgeManager.getInstance(getApplication());

    }

    public boolean validateEmail(String email){
        return email.matches(".+@.+\\..+");
    }

    public int validateRegistration(String email, String name, String password, String repeat){

        if(!email.matches(".+@.+\\..+")){
            return -1;
        }

        if(!name.matches("^[a-zA-Z0-9]{3,23}$")){
            return -2;
        }

        if(! (password.length() >= 8)){
            return -3;
        }

        if(!repeat.equals(password)){
            return -4;
        }


        return 0;
    }

    public LiveData<LiveDataResponse> login(String email, String password){

            return fireManager.login(email, password);

    }

    public LiveData<LiveDataResponse> register(String email, String name, String password, String repeat){


        if(validateRegistration(email, name, password, repeat) == 0){

            return fireManager.register(email, name, password);

        }

        return null;

    }

    public void resetPassword(String email){

        fireManager.resetPassword(email);

    }


    public void sendEmailVerification(){

        fireManager.sendEmailVerification();

    }

    public boolean emailVerified(){

        return fireManager.emailVerified();

    }



    public LiveData<LiveDataResponse> isUserReady(){
        return bridgeManager.isUserReady();
    }

    public boolean isUserSignedIn(){
        return bridgeManager.isUserSignedIn();
    }


}
