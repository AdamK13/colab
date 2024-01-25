package kocot.klass.app;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityOptionsCompat;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;


import kocot.klass.auxiliary.LiveDataResponse;
import kocot.klass.R;
import kocot.klass.auxiliary.Toaster;

public class LauncherActivity extends AppCompatActivity {

    public LauncherActivityModel launcher_vm;

    private TextView appNameTV;
    private  EditText emailAddress;
    private  EditText name;
    private  EditText password;
    private  EditText repeatPassword;

    private Button signIn;
    private Button register;
    private Button resetPassword;

    private ImageButton back;

    private ProgressBar progressBar;

    private boolean signClick, registerClick;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launcher);


        launcher_vm = new ViewModelProvider(this).get(LauncherActivityModel.class);




        appNameTV = findViewById(R.id.textViewLauncherAppName);

        signClick = false;
        registerClick = false;


        emailAddress = findViewById(R.id.editTextLauncherEmailAddress);
        name = findViewById(R.id.editTextLauncherName);
        password = findViewById(R.id.editTextLauncherPassword);
        repeatPassword = findViewById(R.id.editTextLauncherRepeatPassword);

        signIn = findViewById(R.id.buttonLauncherSignIn);
        register = findViewById(R.id.buttonLauncherRegister);
        resetPassword = findViewById(R.id.buttonLauncherResetPassword);

        back = findViewById(R.id.imageButtonLauncherBack);

        progressBar = findViewById(R.id.progressBarLauncher);

        loadingState();

        signIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                signInButtonClick();
            }
        });

        register.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                registerButtonClick();
            }
        });

        resetPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                resetPasswordClick();
            }
        });

        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                clearFields();
                firstRunState();
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();

        if(!launcher_vm.isUserSignedIn()){
            firstRunState();
            return;
        }

        launcher_vm.isUserReady().observe(this, new Observer<LiveDataResponse>() {
            @Override
            public void onChanged(LiveDataResponse liveDataResponse) {

                if(liveDataResponse.isSuccessful() && liveDataResponse.getInfo().equals("READY")){
                    startMainActivity();
                }

                if(liveDataResponse.isSuccessful() && liveDataResponse.getInfo().equals("HOLD")){
                    // ---- WAITING FOR GROUP CHECK ----
                }

                if(!liveDataResponse.isSuccessful() && liveDataResponse.getInfo().equals("SETUP")){
                    startSetupActivity();
                }

                if(!liveDataResponse.isSuccessful() && liveDataResponse.getInfo().equals("NOUSER")){
                    firstRunState();
                }

                if(liveDataResponse.isSuccessful() && liveDataResponse.getInfo().equals("NOUSER_RELOAD")){
                    Exception ex = (Exception) liveDataResponse.getData();
                    Toaster.show(LauncherActivity.this,ex.getLocalizedMessage());
                    firstRunState();
                }

                if(liveDataResponse.isSuccessful() && liveDataResponse.getInfo().equals("OFFLINE")){
                    startMainActivity();
                }




            }
        });

    }

    private void signInButtonClick(){


        loginState();

        if(!signClick){

            signClick = true;
            return;

        }

        if(launcher_vm.validateEmail(emailAddress.getText().toString()) && !password.getText().toString().isEmpty()) {
            loadingState();


            launcher_vm.login(emailAddress.getText().toString(),password.getText().toString())
                    .observe(this, new Observer<LiveDataResponse>() {
                        @Override
                        public void onChanged(LiveDataResponse res) {

                            if(res.isSuccessful()){

                                if(launcher_vm.emailVerified()){
                                    LauncherActivity.this.recreate();

                                } else {
                                    Toaster.show(LauncherActivity.this, R.string.verify_email);
                                    loginState();
                                }


                            } else {
                                loginState();
                                animPassWrong();
                                animEmailWrong();
                                Toaster.show(LauncherActivity.this,res.getInfo());

                            }

                        }
                    });
        } else if(!password.getText().toString().isEmpty()){

            animEmailWrong();
            Toaster.show(this,R.string.email_error);

        }





    }

    private void  registerButtonClick(){

        registerState();

        if(!registerClick){
            registerClick = true;
            return;
        }


        String emailS = emailAddress.getText().toString();
        String nameS = name.getText().toString();
        String passS = password.getText().toString();
        String repeatS = repeatPassword.getText().toString();

        switch(launcher_vm.validateRegistration(emailS,nameS,passS,repeatS)){

            case -1:
                animEmailWrong();
                Toaster.show(this,R.string.email_error);
                return;


            case -2:
                animNameWrong();
                Toaster.show(this,R.string.name_error);
                return;


            case -3:
                animPassWrong();
                Toaster.show(this,R.string.pass_error);
                return;


            case -4:
                animRepeatWrong();
                Toaster.show(this,R.string.repeat_error);
                return;


        }
        loadingState();
        launcher_vm.register(emailS,nameS,passS,repeatS).observe(this, new Observer<LiveDataResponse>() {
            @Override
            public void onChanged(LiveDataResponse res) {
                if(res.isSuccessful()){
                    loginState();
                    launcher_vm.sendEmailVerification();
                    Toaster.show(LauncherActivity.this, R.string.registration_success);
                }else{
                    registerState();
                    switch (res.getInfo()){
                        case "NAME":
                            Toaster.show(LauncherActivity.this,R.string.name_taken);
                            break;
                        case "EMAIL":
                            Toaster.show(LauncherActivity.this,R.string.email_error);
                            break;
                        case "PASSWORD":
                            Toaster.show(LauncherActivity.this,R.string.pass_error);
                            break;
                        default:
                            Toaster.show(LauncherActivity.this,res.getInfo());
                            break;
                    }
                }
            }
        });


    }

    private void resetPasswordClick(){

        String email = emailAddress.getText().toString();

        if(launcher_vm.validateEmail(email)){

            launcher_vm.resetPassword(email);
            Toaster.show(this,R.string.reset_password_confirmation);

        } else {

            animEmailWrong();
            Toaster.show(this,R.string.email_error);

        }

    }

    private void clearFields(){

        emailAddress.getText().clear();
        name.getText().clear();
        password.getText().clear();
        repeatPassword.getText().clear();

    }

    private void loadingState(){

        emailAddress.setVisibility(View.GONE);
        name.setVisibility(View.GONE);
        password.setVisibility(View.GONE);
        repeatPassword.setVisibility(View.GONE);
        back.setVisibility(View.GONE);
        signIn.setVisibility(View.GONE);
        register.setVisibility(View.GONE);
        resetPassword.setVisibility(View.GONE);
        progressBar.setVisibility(View.VISIBLE);

    }

    private void firstRunState(){

        signClick = false;
        registerClick = false;

        emailAddress.setVisibility(View.GONE);
        name.setVisibility(View.GONE);
        password.setVisibility(View.GONE);
        repeatPassword.setVisibility(View.GONE);
        back.setVisibility(View.GONE);
        signIn.setVisibility(View.VISIBLE);
        register.setVisibility(View.VISIBLE);
        resetPassword.setVisibility(View.GONE);
        progressBar.setVisibility(View.GONE);

    }

    private void registerState(){

        emailAddress.setVisibility(View.VISIBLE);
        name.setVisibility(View.VISIBLE);
        password.setVisibility(View.VISIBLE);
        repeatPassword.setVisibility(View.VISIBLE);
        back.setVisibility(View.VISIBLE);
        resetPassword.setVisibility(View.GONE);
        register.setVisibility(View.VISIBLE);
        signIn.setVisibility(View.GONE);
        progressBar.setVisibility(View.GONE);

    }

    private void loginState(){

        emailAddress.setVisibility(View.VISIBLE);
        password.setVisibility(View.VISIBLE);
        back.setVisibility(View.VISIBLE);
        signIn.setVisibility(View.VISIBLE);
        resetPassword.setVisibility(View.VISIBLE);
        register.setVisibility(View.GONE);
        progressBar.setVisibility(View.GONE);

    }

    private void startMainActivity(){

        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        startActivity(intent);
        finish();

    }

    private void startSetupActivity(){


        Intent intent = new Intent(this, SetupActivity.class);
        ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(this,appNameTV,"appNameTV");
        startActivity(intent, options.toBundle());


    }

    private void animEmailWrong(){

        emailAddress.startAnimation(
                AnimationUtils.loadAnimation(LauncherActivity.this,R.anim.shake)
        );

    }

    private void animNameWrong(){

        name.startAnimation(
                AnimationUtils.loadAnimation(LauncherActivity.this,R.anim.shake)
        );

    }


    private void animPassWrong(){

        password.startAnimation(
                AnimationUtils.loadAnimation(LauncherActivity.this,R.anim.shake)
        );

    }

    private void animRepeatWrong(){

        repeatPassword.startAnimation(
                AnimationUtils.loadAnimation(LauncherActivity.this,R.anim.shake)
        );

    }






}