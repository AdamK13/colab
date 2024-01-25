package kocot.klass.app;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.widget.SwitchCompat;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import android.app.Dialog;
import android.os.Bundle;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import kocot.klass.R;
import kocot.klass.auxiliary.Toaster;
import kocot.klass.auxiliary.LiveDataResponse;

public class GroupAdminActivity extends AppCompatActivity {


    LinearLayout groupMembersContainer;
    LinearLayout groupMemberListContainer;
    LinearLayout generalSettingsContainer;
    LinearLayout passwordSettingsContainer;
    ImageButton setVisibilityButton;
    ImageButton groupMembersExpandButton;
    Button changePasswordButton;
    EditText passwordEditText;
    String currentPassword;
    Boolean lock;
    SwitchCompat groupLock;
    TextView groupLockInfo;
    SwitchCompat writingPerm;
    TextView writingPermInfo;
    ProgressBar progressBarMain;
    ProgressBar progressBarMembers;
    TextView textViewGroupName;
    GroupAdminActivityModel model;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_admin);


        generalSettingsContainer = findViewById(R.id.linearLayoutAdminGeneralSettings);
        groupMembersContainer = findViewById(R.id.linearLayoutAdminGroupMembers);
        groupMemberListContainer = findViewById(R.id.linearLayoutAdminMemberList);
        passwordSettingsContainer = findViewById(R.id.linearLayoutAdminPasswordEdit);

        changePasswordButton = findViewById(R.id.buttonAdminChangePassword);
        setVisibilityButton = findViewById(R.id.imageButtonAdminPasswordVisibility);
        passwordEditText = findViewById(R.id.editTextAdminPassword);

        groupMembersExpandButton = findViewById(R.id.imageButtonGroupMembersExpand);
        groupMembersExpandButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                switchGroupMembersExpand();
            }
        });

        groupLock = findViewById(R.id.switchAdminLockGroup);
        groupLockInfo = findViewById(R.id.textViewAdminLockInfo);

        writingPerm = findViewById(R.id.switchAdminWritingPermissions);
        writingPermInfo = findViewById(R.id.textViewAdminWritingInfo);

        progressBarMain = findViewById(R.id.progressBarAdminPanel);
        progressBarMembers = findViewById(R.id.progressBarGroupMembers);

        textViewGroupName = findViewById(R.id.textViewToolbarGroupName);

        model = new ViewModelProvider(this).get(GroupAdminActivityModel.class);

        textViewGroupName.setText(model.getCurrentGroup());


        loadingState();
        init();


    }

    private void init(){

        groupLock.setEnabled(false);
        writingPerm.setEnabled(false);
        changePasswordButton.setEnabled(false);
        setVisibilityButton.setEnabled(false);
        passwordEditText.setEnabled(false);

        model.getGroupSettings().observe(this, new Observer<LiveDataResponse>() {
            @Override
            public void onChanged(LiveDataResponse liveDataResponse) {
                if(liveDataResponse.isSuccessful()){

                    Map<String,Object> response = (Map<String, Object>) liveDataResponse.getData();
                    if(response != null){
                        lock = (Boolean) response.get("lock");
                        String conversation = (String) response.get("conversation");
                        String groupPassword = (String) response.get("groupPassword");
                        currentPassword = groupPassword;

                        if(lock != null && conversation != null){
                            groupLock.setChecked(lock);
                            writingPerm.setChecked(conversation.contentEquals("OWNER"));
                            passwordEditText.setText(groupPassword);
                            passwordEditText.setTransformationMethod(PasswordTransformationMethod.getInstance());
                            passwordEditText.setEnabled(false);
                            setInfo();
                            readyState();
                            return;
                        }
                    }

                } else {

                    Toaster.show(GroupAdminActivity.this,liveDataResponse.getInfo());

                }

                finish();

            }
        });

        populateMemberList();

    }

    private void populateMemberList(){

        progressBarMembers.setVisibility(View.VISIBLE);

        LayoutInflater inflater = getLayoutInflater();

        model.getGroupMembers().observe(this, new Observer<LiveDataResponse>() {
            @Override
            public void onChanged(LiveDataResponse liveDataResponse) {
                if(liveDataResponse.isSuccessful()){
                    groupMemberListContainer.removeAllViews();
                    ArrayList<HashMap<String, String>> memberList = (ArrayList<HashMap<String, String>>) liveDataResponse.getData();

                    for(HashMap<String, String> member : memberList){

                        View memberView =  inflater.inflate(R.layout.member_view_layout,groupMemberListContainer,false);

                        TextView memberName = memberView.findViewById(R.id.textViewMemberViewUsername);
                        TextView memberUID = memberView.findViewById(R.id.textViewMemberViewUID);
                        ProgressBar progressBar = memberView.findViewById(R.id.progressBarMemberView);
                        ImageButton removeMemberButton = memberView.findViewById(R.id.imageButtonMemberViewRemove);
                        memberName.setText(member.get("userName"));
                        String uid = (String) member.get("userID");
                        memberUID.setText(uid);
                        if(uid.equals(model.getOwnUID())){
                            removeMemberButton.setEnabled(false);
                            removeMemberButton.setVisibility(View.GONE);
                            groupMemberListContainer.addView(memberView);
                            continue;
                        }
                        removeMemberButton.setTag(member.get("userID"));

                        removeMemberButton.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                removeMemberButton.setVisibility(View.GONE);
                                progressBar.setVisibility(View.VISIBLE);
                                String uid = (String) view.getTag();
                                model.removeGroupMember(uid).observe(GroupAdminActivity.this, new Observer<LiveDataResponse>() {
                                    @Override
                                    public void onChanged(LiveDataResponse liveDataResponse) {
                                        if(liveDataResponse.isSuccessful()){
                                            Toaster.show(GroupAdminActivity.this,R.string.member_removed);
                                            populateMemberList();
                                        } else {
                                            Toaster.show(GroupAdminActivity.this,R.string.failed_to_remove_member);
                                            progressBar.setVisibility(View.GONE);
                                            removeMemberButton.setVisibility(View.VISIBLE);
                                        }
                                    }
                                });
                            }
                        });
                        groupMemberListContainer.addView(memberView);


                    }
                }
                progressBarMembers.setVisibility(View.GONE);


            }
        });




    }


    private void setInfo(){

        if(groupLock.isChecked()){
            groupLockInfo.setText(R.string.admin_panel_locked_group_info);
        } else {
            groupLockInfo.setText(R.string.admin_panel_unlocked_group_info);
        }

        if(writingPerm.isChecked()){
            writingPermInfo.setText(R.string.admin_panel_writing_closed_info);
        } else {
            writingPermInfo.setText(R.string.admin_panel_writing_open_info);
        }

    }

    private void loadingState(){
        generalSettingsContainer.setVisibility(View.GONE);
        groupMembersContainer.setVisibility(View.GONE);
        passwordSettingsContainer.setVisibility(View.GONE);

        progressBarMain.setVisibility(View.VISIBLE);
    }

    private void readyState(){
        generalSettingsContainer.setVisibility(View.VISIBLE);
        groupMembersContainer.setVisibility(View.VISIBLE);

        if(lock){
            passwordSettingsContainer.setVisibility(View.VISIBLE);
            passwordEditText.setEnabled(true);
            changePasswordButton.setEnabled(true);
            setVisibilityButton.setEnabled(true);
            setVisibilityButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    switchPasswordVisibility();
                }
            });

            changePasswordButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    changePassword();
                }
            });
        } else {
            passwordSettingsContainer.setVisibility(View.GONE);
            passwordEditText.setEnabled(false);
        }

        groupLock.setEnabled(true);
        writingPerm.setEnabled(true);

        progressBarMain.setVisibility(View.GONE);


        groupLock.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean nextState) {
                lockWarning(nextState);
                compoundButton.toggle();
            }
        });

        writingPerm.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean nextState) {
                conversationModeWarning(nextState);
                compoundButton.toggle();
            }
        });
    }

    private void lockWarning(Boolean nextState){

        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_warning_group_setting_change);
        dialog.setCancelable(true);
        dialog.getWindow().setBackgroundDrawableResource(R.color.transparent);

        TextView info = dialog.findViewById(R.id.textViewSettingsChangeInfo);
        Button performAction = dialog.findViewById(R.id.buttonSettingsChangePerformAction);
        Button cancel = dialog.findViewById(R.id.buttonSettingsChangeCancel);
        ProgressBar progressBar = dialog.findViewById(R.id.progressBarDialogWarning);


        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.cancel();
            }
        });

        if(nextState){ // True = locking group

            info.setText(getResources().getString(R.string.warning_locking_group));
            performAction.setText(getResources().getString(R.string.lock_button));

            performAction.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    performAction.setVisibility(View.GONE);
                    cancel.setVisibility(View.GONE);
                    progressBar.setVisibility(View.VISIBLE);
                    model.lockGroup().observe(GroupAdminActivity.this, new Observer<LiveDataResponse>() {
                        @Override
                        public void onChanged(LiveDataResponse liveDataResponse) {
                            if(liveDataResponse.isSuccessful()){
                                Toaster.show(GroupAdminActivity.this,R.string.locking_group_success);
                                dialog.dismiss();
                                groupLock.setOnCheckedChangeListener(null);
                                init();
                            } else {
                                Toaster.show(GroupAdminActivity.this,R.string.locking_group_failure);
                            }
                        }
                    });
                }
            });



        } else {

            info.setText(getResources().getString(R.string.warning_unlocking_group));
            performAction.setText(getResources().getString(R.string.unlock_button));

            performAction.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    performAction.setVisibility(View.GONE);
                    cancel.setVisibility(View.GONE);
                    progressBar.setVisibility(View.VISIBLE);
                    model.unlockGroup().observe(GroupAdminActivity.this, new Observer<LiveDataResponse>() {
                        @Override
                        public void onChanged(LiveDataResponse liveDataResponse) {
                            if(liveDataResponse.isSuccessful()){
                                Toaster.show(GroupAdminActivity.this,R.string.unlocking_group_success);
                                dialog.dismiss();
                                groupLock.setOnCheckedChangeListener(null);
                                init();
                            } else {
                                Toaster.show(GroupAdminActivity.this,R.string.unlocking_group_failure);
                            }
                        }
                    });
                }
            });



        }

        DisplayMetrics metrics = getResources().getDisplayMetrics();
        int width = metrics.widthPixels;

        dialog.show();

        dialog.getWindow().setLayout( width, LinearLayout.LayoutParams.WRAP_CONTENT);

    }

    private void conversationModeWarning(Boolean nextState){

        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_warning_group_setting_change);
        dialog.setCancelable(true);
        dialog.getWindow().setBackgroundDrawableResource(R.color.transparent);

        TextView info = dialog.findViewById(R.id.textViewSettingsChangeInfo);
        Button performAction = dialog.findViewById(R.id.buttonSettingsChangePerformAction);
        Button cancel = dialog.findViewById(R.id.buttonSettingsChangeCancel);
        ProgressBar progressBar = dialog.findViewById(R.id.progressBarDialogWarning);

        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.cancel();
            }
        });

        if(nextState){ // True = Owner only mode
            info.setText(R.string.warning_closing_conversation);
            performAction.setText(R.string.close_conversation_button);

            performAction.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    performAction.setVisibility(View.GONE);
                    cancel.setVisibility(View.GONE);
                    progressBar.setVisibility(View.VISIBLE);

                    model.setOwnerOnlyConversationMode().observe(GroupAdminActivity.this, new Observer<LiveDataResponse>() {
                        @Override
                        public void onChanged(LiveDataResponse liveDataResponse) {
                            if(liveDataResponse.isSuccessful()){
                                Toaster.show(GroupAdminActivity.this,R.string.closing_conversation_success);
                                dialog.dismiss();
                                writingPerm.setOnCheckedChangeListener(null);
                                init();
                            } else {
                                Toaster.show(GroupAdminActivity.this,R.string.closing_conversation_failure);
                            }
                        }
                    });

                }
            });
        } else {

            info.setText(R.string.warning_opening_conversation);
            performAction.setText(R.string.open_conversation_button);

            performAction.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    performAction.setVisibility(View.GONE);
                    cancel.setVisibility(View.GONE);
                    progressBar.setVisibility(View.VISIBLE);

                    model.setOpenConversationMode().observe(GroupAdminActivity.this, new Observer<LiveDataResponse>() {
                        @Override
                        public void onChanged(LiveDataResponse liveDataResponse) {
                            if(liveDataResponse.isSuccessful()){
                                Toaster.show(GroupAdminActivity.this,R.string.opening_conversation_success);
                                dialog.dismiss();
                                writingPerm.setOnCheckedChangeListener(null);
                                init();
                            } else {
                                Toaster.show(GroupAdminActivity.this, R.string.opening_conversation_failure);
                            }
                        }
                    });

                }
            });

        }

        DisplayMetrics metrics = getResources().getDisplayMetrics();
        int width = metrics.widthPixels;

        dialog.show();

        dialog.getWindow().setLayout( width, LinearLayout.LayoutParams.WRAP_CONTENT);

    }

    private void switchPasswordVisibility(){

        if (passwordEditText.getTransformationMethod()
                instanceof HideReturnsTransformationMethod) {
            passwordEditText.setTransformationMethod(PasswordTransformationMethod.getInstance());
        } else {
            passwordEditText.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
        }

    }

    private void switchGroupMembersExpand(){

        if(generalSettingsContainer.getVisibility() == View.VISIBLE){
            generalSettingsContainer.setVisibility(View.GONE);
            groupMembersExpandButton.setImageDrawable(AppCompatResources.getDrawable(this,R.drawable.arrow_drop_down_40px));
        } else {
            generalSettingsContainer.setVisibility(View.VISIBLE);
            groupMembersExpandButton.setImageDrawable(AppCompatResources.getDrawable(this,R.drawable.arrow_drop_up_40px));
        }

    }

    private void changePassword(){

        String desiredPassword = passwordEditText.getText().toString();

        if(currentPassword.equals(desiredPassword)){
            Toaster.show(this,R.string.choose_different_password);
            return;
        }

        if(!model.validateGroupPassword(desiredPassword)){
            Toaster.show(this,R.string.invalid_group_password);
            return;
        }

        passwordChangeWarning(desiredPassword);
    }

    private void passwordChangeWarning(String password){

        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_warning_group_setting_change);
        dialog.setCancelable(true);
        dialog.getWindow().setBackgroundDrawableResource(R.color.transparent);

        TextView info = dialog.findViewById(R.id.textViewSettingsChangeInfo);
        Button performAction = dialog.findViewById(R.id.buttonSettingsChangePerformAction);
        Button cancel = dialog.findViewById(R.id.buttonSettingsChangeCancel);
        ProgressBar progressBar = dialog.findViewById(R.id.progressBarDialogWarning);

        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.cancel();
            }
        });

        info.setText(getString(R.string.warning_changing_password,password));
        performAction.setText(R.string.change);

        performAction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                performAction.setVisibility(View.GONE);
                performAction.setEnabled(false);
                cancel.setVisibility(View.GONE);
                cancel.setEnabled(false);
                progressBar.setVisibility(View.VISIBLE);
                model.changePassword(password).observe(GroupAdminActivity.this, new Observer<LiveDataResponse>() {
                    @Override
                    public void onChanged(LiveDataResponse liveDataResponse) {

                        if(liveDataResponse.isSuccessful()){
                            init();
                            dialog.dismiss();
                            Toaster.show(GroupAdminActivity.this,R.string.password_change_success);
                        } else {
                            Toaster.show(GroupAdminActivity.this,R.string.password_change_failure);
                        }

                    }
                });
            }
        });

        DisplayMetrics metrics = getResources().getDisplayMetrics();
        int width = metrics.widthPixels;

        dialog.show();

        dialog.getWindow().setLayout( width, LinearLayout.LayoutParams.WRAP_CONTENT);

    }

}