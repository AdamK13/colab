package kocot.klass.app;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import android.app.ActivityManager;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Pair;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

import kocot.klass.R;
import kocot.klass.auxiliary.LiveDataResponse;
import kocot.klass.auxiliary.Toaster;
import kocot.klass.structures.Project;

public class UserSettingsActivity extends AppCompatActivity {

    private static final int DIALOG_LEAVE_GROUP = 1;
    private static final int DIALOG_DELETE_GROUP = 0;
    private ImageButton buttonReturn;
    private LinearLayout layoutGroupsHolder;
    private Button buttonResetApp;
    private Button buttonChangePassword;
    private Button buttonDeleteAccount;
    private Button buttonLogout;
    private TextView textViewUsername;
    private TextView textViewEmail;

    private UserSettingsModel model;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_settings);

        buttonReturn = findViewById(R.id.imageButtonReturn);
        layoutGroupsHolder = findViewById(R.id.linearLayoutGroupLeaveRemoveHolder);
        buttonResetApp = findViewById(R.id.buttonSettingsResetApp);
        buttonChangePassword = findViewById(R.id.buttonSettingsChangePassword);
        buttonDeleteAccount = findViewById(R.id.buttonSettingsDeleteAccount);
        buttonLogout = findViewById(R.id.buttonSettingsLogout);
        textViewUsername = findViewById(R.id.textViewUserSettingsUsername);
        textViewEmail = findViewById(R.id.textViewUserSettingsEmail);

        model = new ViewModelProvider(this).get(UserSettingsModel.class);

        buttonReturn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });


        buttonResetApp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                resetApp();
            }
        });

        buttonChangePassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!model.isInternetAccessible()){
                    Toaster.show(UserSettingsActivity.this,R.string.no_internet);
                    return;
                }
                showPasswordChangeDialog();
            }
        });

        buttonLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!model.isInternetAccessible()){
                    Toaster.show(UserSettingsActivity.this,R.string.no_internet);
                    return;
                }
                logout();
            }
        });

        buttonDeleteAccount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!model.isInternetAccessible()){
                    Toaster.show(UserSettingsActivity.this,R.string.no_internet);
                    return;
                }
                showDialogDeleteAccount();
            }
        });

        Pair<String, String> usernameEmail = model.getUsernameEmail();
        if(usernameEmail == null){
            textViewEmail.setVisibility(View.GONE);
            textViewUsername.setVisibility(View.GONE);
        } else {
            textViewUsername.setText(usernameEmail.first);
            textViewEmail.setText(usernameEmail.second);
        }

        populateGroups();

    }


    private void populateGroups(){

        ArrayList<String> groups = model.getGroups();
        layoutGroupsHolder.removeAllViews();

        if(groups.isEmpty()){
            getOnBackPressedDispatcher().addCallback(new OnBackPressedCallback(true) {
                @Override
                public void handleOnBackPressed() {
                    startSetupActivity();
                }
            });

        }

        for(String group : groups){

            View groupView = getLayoutInflater().inflate(R.layout.group_leave_remove_setting_view,layoutGroupsHolder,false);
            TextView textViewGroup = groupView.findViewById(R.id.textViewGroupLeaveRemoveTemplate);
            Button buttonGroup = groupView.findViewById(R.id.buttonGroupLeaveRemoveTemplate);
            ProgressBar progressBar = groupView.findViewById(R.id.progressBarGroupLeaveRemove);
            Boolean isAdmin = model.isUserAdmin(group);
            if(isAdmin){
                Drawable background = buttonGroup.getBackground();
                background = DrawableCompat.wrap(background);
                DrawableCompat.setTint(background,getResources().getColor(R.color.error_red,null));
                buttonGroup.setBackground(background);
                buttonGroup.setText(R.string.delete);
                buttonGroup.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if(!model.isInternetAccessible()){
                            Toaster.show(UserSettingsActivity.this,R.string.no_internet);
                            return;
                        }
                        showWarningDialog(DIALOG_DELETE_GROUP,group);
                    }
                });
            } else {
                buttonGroup.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if(!model.isInternetAccessible()){
                            Toaster.show(UserSettingsActivity.this,R.string.no_internet);
                            return;
                        }
                        showWarningDialog(DIALOG_LEAVE_GROUP,group);
                    }
                });
            }

            textViewGroup.setText(group);
            layoutGroupsHolder.addView(groupView);

        }

    }

    private void resetApp(){

        ((ActivityManager) getApplicationContext().getSystemService(ACTIVITY_SERVICE))
                .clearApplicationUserData();

    }


    private void logout(){

        model.logout();

        Intent intent = new Intent(getApplicationContext(), LauncherActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();

    }

    private void showPasswordChangeDialog(){

        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_change_password);
        dialog.setCancelable(true);
        dialog.getWindow().setBackgroundDrawableResource(R.color.transparent);

        DisplayMetrics metrics = getResources().getDisplayMetrics();
        int width = metrics.widthPixels;
        dialog.getWindow().setLayout(width, WindowManager.LayoutParams.WRAP_CONTENT);

        EditText passwordOld = dialog.findViewById(R.id.editTextChangePasswordOld);
        EditText passwordNew = dialog.findViewById(R.id.editTextChangePasswordNew);
        Button changePasswordButtonGo = dialog.findViewById(R.id.buttonChangePasswordGo);
        ProgressBar changingProgress = dialog.findViewById(R.id.progressBarChangingAccountPassword);

        changePasswordButtonGo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if(passwordOld.getText().length() < 1){
                    Toaster.show(UserSettingsActivity.this,R.string.changing_password_no_current_error);
                    return;
                }
                if(passwordNew.getText().length() < 8){
                    Toaster.show(UserSettingsActivity.this,R.string.password_length_error);
                    return;
                }
                if(passwordNew.getText().toString().equals(passwordOld.getText().toString())){
                    Toaster.show(UserSettingsActivity.this,R.string.new_password_same_as_old_error);
                    return;
                }

                changingProgress.setVisibility(View.VISIBLE);
                changePasswordButtonGo.setVisibility(View.GONE);

                model.reLogin(passwordOld.getText().toString())
                        .observe(UserSettingsActivity.this, new Observer<LiveDataResponse>() {
                            @Override
                            public void onChanged(LiveDataResponse liveDataResponse) {
                                if(!liveDataResponse.isSuccessful()){
                                    Toaster.show(UserSettingsActivity.this,liveDataResponse.getInfo());
                                    changingProgress.setVisibility(View.GONE);
                                    changePasswordButtonGo.setVisibility(View.VISIBLE);
                                    return;
                                }

                                model.changeAccountPassword(passwordNew.getText().toString())
                                        .observe(UserSettingsActivity.this, new Observer<LiveDataResponse>() {
                                            @Override
                                            public void onChanged(LiveDataResponse liveDataResponse) {
                                                if(liveDataResponse.isSuccessful()){
                                                    Toaster.show(UserSettingsActivity.this,R.string.changing_password_success);
                                                    dialog.dismiss();
                                                } else {
                                                    Toaster.show(UserSettingsActivity.this, liveDataResponse.getInfo());
                                                    changingProgress.setVisibility(View.GONE);
                                                    changePasswordButtonGo.setVisibility(View.VISIBLE);
                                                }
                                            }
                                        });
                            }
                        });


            }
        });

        dialog.show();

    }

    private void showWarningDialog(int TYPE, String group){

        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_user_settings_leave_delete_group);
        dialog.setCancelable(true);
        dialog.getWindow().setBackgroundDrawableResource(R.color.transparent);

        DisplayMetrics metrics = getResources().getDisplayMetrics();
        int width = metrics.widthPixels;
        dialog.getWindow().setLayout(width, WindowManager.LayoutParams.WRAP_CONTENT);

        TextView tvInfo = dialog.findViewById(R.id.textViewUserSettingsDialogInfo);
        Button buttonPerform = dialog.findViewById(R.id.buttonUserSettingsDialogPerformAction);
        Button buttonCancel = dialog.findViewById(R.id.buttonUserSettingsDialogCancel);
        ProgressBar progressBar = dialog.findViewById(R.id.progressBarUserSettingsDialog);

        buttonCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });

        if(TYPE == DIALOG_LEAVE_GROUP){

            tvInfo.setText(getString(R.string.leave_group_warning_info,group));
            buttonPerform.setText(R.string.leave);
            buttonPerform.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    buttonPerform.setVisibility(View.GONE);
                    buttonCancel.setVisibility(View.GONE);
                    progressBar.setVisibility(View.VISIBLE);
                    model.leaveGroup(group).observe(UserSettingsActivity.this, new Observer<LiveDataResponse>() {
                        @Override
                        public void onChanged(LiveDataResponse liveDataResponse) {
                            if(liveDataResponse.isSuccessful()){
                                String info = getString(R.string.left_group_info,group);
                                Toaster.show(UserSettingsActivity.this,info);
                                populateGroups();
                                dialog.dismiss();
                            } else {
                                Toaster.show(UserSettingsActivity.this,liveDataResponse.getInfo());
                                dialog.dismiss();
                            }
                        }
                    });
                }
            });

        }

        if(TYPE == DIALOG_DELETE_GROUP){

            tvInfo.setText(getString(R.string.delete_group_warning_info,group));
            buttonPerform.setText(R.string.delete);
            buttonPerform.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    buttonPerform.setVisibility(View.GONE);
                    buttonCancel.setVisibility(View.GONE);
                    progressBar.setVisibility(View.VISIBLE);

                    model.deleteGroup(group).observe(UserSettingsActivity.this, new Observer<LiveDataResponse>() {
                        @Override
                        public void onChanged(LiveDataResponse liveDataResponse) {
                            if(liveDataResponse.isSuccessful()){
                                model.leaveGroup(group).observe(UserSettingsActivity.this, new Observer<LiveDataResponse>() {
                                    @Override
                                    public void onChanged(LiveDataResponse liveDataResponse) {
                                        if(liveDataResponse.isSuccessful()){
                                            String info = getString(R.string.deleted_group_info,group);
                                            Toaster.show(UserSettingsActivity.this,info);
                                            populateGroups();
                                            dialog.dismiss();
                                        }
                                    }
                                });
                            } else {
                                Toaster.show(UserSettingsActivity.this,liveDataResponse.getInfo());
                                dialog.dismiss();

                            }
                        }
                    });
                }
            });


        }


        dialog.show();

    }

    private void showDialogDeleteAccount(){

        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_user_settings_delete_account);
        dialog.setCancelable(true);
        dialog.getWindow().setBackgroundDrawableResource(R.color.transparent);

        DisplayMetrics metrics = getResources().getDisplayMetrics();
        int width = metrics.widthPixels;
        dialog.getWindow().setLayout(width, WindowManager.LayoutParams.WRAP_CONTENT);

        TextView tvInfo = dialog.findViewById(R.id.textViewUserSettingsDialogInfoDelAcc);
        EditText etPassword = dialog.findViewById(R.id.editTextDeleteAccountEnterPassword);
        Button buttonPerform = dialog.findViewById(R.id.buttonUserSettingsDialogPerformActionDelAcc);
        Button buttonCancel = dialog.findViewById(R.id.buttonUserSettingsDialogDelAccCancel);
        ProgressBar progressBar = dialog.findViewById(R.id.progressBarUserSettingsDialogDelAcc);

        buttonCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });

        buttonPerform.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                buttonPerform.setVisibility(View.GONE);
                buttonCancel.setVisibility(View.GONE);
                progressBar.setVisibility(View.VISIBLE);
                if(etPassword.getText().length() > 1){
                    model.reLogin(etPassword.getText().toString())
                            .observe(UserSettingsActivity.this, new Observer<LiveDataResponse>() {
                        @Override
                        public void onChanged(LiveDataResponse liveDataResponse) {
                            if(liveDataResponse.isSuccessful()){
                                model.deleteAccount()
                                        .observe(UserSettingsActivity.this, new Observer<LiveDataResponse>() {
                                    @Override
                                    public void onChanged(LiveDataResponse liveDataResponse) {
                                        if(liveDataResponse.isSuccessful()){
                                            Toaster.show(UserSettingsActivity.this,R.string.goodbye);
                                            try {
                                                Thread.sleep(3000);
                                            } catch (Exception ignored){}
                                            resetApp();
                                        } else {
                                            Toaster.show(UserSettingsActivity.this,liveDataResponse.getInfo());
                                            dialog.dismiss();
                                        }
                                    }
                                });
                            } else {
                                Toaster.show(UserSettingsActivity.this,liveDataResponse.getInfo());
                                etPassword.getText().clear();
                                buttonPerform.setVisibility(View.VISIBLE);
                                buttonCancel.setVisibility(View.VISIBLE);
                                progressBar.setVisibility(View.GONE);
                            }
                        }
                    });
                } else {
                    Toaster.show(UserSettingsActivity.this,R.string.password_wrong);
                    etPassword.getText().clear();
                    buttonPerform.setVisibility(View.VISIBLE);
                    buttonCancel.setVisibility(View.VISIBLE);
                    progressBar.setVisibility(View.GONE);
                }
            }
        });

        dialog.show();

    }

    private void startSetupActivity(){

        Intent intent = new Intent(getApplicationContext(), SetupActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.putExtra("firstStart",false);
        startActivity(intent);
        finish();

    }
}