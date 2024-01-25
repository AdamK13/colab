package kocot.klass.app;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import kocot.klass.R;
import kocot.klass.auxiliary.LiveDataResponse;
import kocot.klass.auxiliary.Toaster;

public class SetupFragment extends Fragment {

    public static final int MODE_JOIN = 1;
    public static final int MODE_CREATE = 0;
    private int mode;
    private TextView textViewGroupName;
    private TextView textViewPassword;
    private TextView textViewPasswordInfo;
    private SetupModel model;
    private EditText editTextGroupPassword;
    private EditText editTextGroupName;
    private Button buttonExecute;
    private ProgressBar progressBar;
    public SetupFragment() {

    }



    public static SetupFragment newInstance(int mode) {
        SetupFragment fragment = new SetupFragment();
        Bundle args = new Bundle();
        args.putInt("mode", mode);
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mode = getArguments().getInt("mode");
        }
        model = new ViewModelProvider(requireActivity()).get(SetupModel.class);
        model.postStatus(SetupModel.STATUS_HOLD);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_setup, container, false);
        textViewGroupName = view.findViewById(R.id.textViewGroupNameSetupFragment);
        textViewPassword = view.findViewById(R.id.textViewGroupPasswordSetupFragment);
        textViewPasswordInfo = view.findViewById(R.id.textViewGroupPasswordInfoSetupFragment);
        editTextGroupPassword = view.findViewById(R.id.editTextGroupPasswordSetupFragment);
        editTextGroupName = view.findViewById(R.id.editTextGroupNameSetupFragment);
        buttonExecute = view.findViewById(R.id.buttonSetupFragment);
        progressBar = view.findViewById(R.id.progressBarSetupFragment);

        switch (mode){
            case MODE_CREATE:
                initCreate();
                break;

            case MODE_JOIN:
                initJoin();
                break;

        }

        return view;

    }

    private void initJoin(){

        textViewPassword.setVisibility(View.GONE);
        textViewPasswordInfo.setVisibility(View.GONE);
        editTextGroupPassword.setVisibility(View.GONE);
        editTextGroupName.setEnabled(true);
        editTextGroupName.getText().clear();
        progressBar.setVisibility(View.GONE);

        textViewPassword.setText(R.string.join_group_password_prompt);
        textViewGroupName.setText(R.string.join_group_prompt);

        buttonExecute.setText(R.string.join_group);
        buttonExecute.setEnabled(true);
        buttonExecute.setVisibility(View.VISIBLE);

        buttonExecute.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!model.isInternetAccessible()){
                    Toaster.show(getContext(),R.string.no_internet);
                    return;
                }
                String groupName = editTextGroupName.getText().toString();
                if(!model.validateGroupName(groupName)){
                    Toaster.show(getContext(),R.string.invalid_group_name);
                    return;
                }
                joinGroup(groupName,null);
            }
        });

    }

    private void initJoinPassword(){

        textViewPassword.setVisibility(View.VISIBLE);
        textViewPasswordInfo.setVisibility(View.VISIBLE);

        editTextGroupPassword.setEnabled(true);
        editTextGroupPassword.getText().clear();
        editTextGroupPassword.setVisibility(View.VISIBLE);

        editTextGroupName.setEnabled(true);
        editTextGroupName.setVisibility(View.VISIBLE);

        textViewPassword.setText(R.string.join_group_password_prompt);
        textViewGroupName.setText(R.string.join_group_prompt);

        buttonExecute.setEnabled(true);
        buttonExecute.setVisibility(View.VISIBLE);
        buttonExecute.setText(R.string.join_group);

        progressBar.setVisibility(View.GONE);

        buttonExecute.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!model.isInternetAccessible()){
                    Toaster.show(getContext(),R.string.no_internet);
                    return;
                }
                String groupName = editTextGroupName.getText().toString();
                String groupPassword = editTextGroupPassword.getText().toString();
                if(!model.validateGroupName(groupName)){
                    Toaster.show(getContext(),R.string.invalid_group_name);
                    return;
                }
                if(!model.validateGroupPassword(groupPassword)){
                    Toaster.show(getContext(),R.string.invalid_group_password);
                    return;
                }

                joinGroup(groupName,groupPassword);
            }
        });

    }

    private void initCreate(){

        editTextGroupPassword.setEnabled(true);
        editTextGroupPassword.getText().clear();
        editTextGroupPassword.setVisibility(View.VISIBLE);
        editTextGroupName.setEnabled(true);
        editTextGroupName.getText().clear();
        editTextGroupName.setVisibility(View.VISIBLE);

        buttonExecute.setEnabled(true);
        buttonExecute.setVisibility(View.VISIBLE);
        buttonExecute.setText(R.string.create_group);
        progressBar.setVisibility(View.GONE);

        buttonExecute.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!model.isInternetAccessible()){
                    Toaster.show(getContext(),R.string.no_internet);
                    return;
                }
                String groupName = editTextGroupName.getText().toString();
                String groupPassword = editTextGroupPassword.getText().toString();
                if(!model.validateGroupName(groupName)){
                    Toaster.show(getContext(),R.string.invalid_group_name);
                    return;
                }
                if(!model.validateGroupPassword(groupPassword)){
                    Toaster.show(getContext(),R.string.invalid_group_password);
                    return;
                }
                createGroup(groupName,groupPassword);
            }
        });

    }

    private void createGroup(String groupName, String groupPassword){

        editTextGroupName.setEnabled(false);
        editTextGroupPassword.setEnabled(false);
        buttonExecute.setEnabled(false);
        buttonExecute.setVisibility(View.GONE);
        progressBar.setVisibility(View.VISIBLE);

        model.createGroup(groupName, groupPassword)
                .observe(getViewLifecycleOwner(), new Observer<LiveDataResponse>() {
                    @Override
                    public void onChanged(LiveDataResponse liveDataResponse) {
                        if(liveDataResponse.isSuccessful()){
                            model.postStatus(SetupModel.STATUS_PROCEED);
                        } else{
                            model.postStatus(SetupModel.STATUS_HOLD);
                            if(liveDataResponse.getInfo().equals("NAME_UNAVAILABLE")) {
                                Toaster.show(getContext(), R.string.group_name_unavailable);
                            }
                            else{
                                Toaster.show(getContext(),R.string.something_went_wrong);
                            }

                            initCreate();
                        }
                    }
                });
    }

    private void joinGroup(String groupName, @Nullable String groupPassword){

        editTextGroupName.setEnabled(false);
        editTextGroupPassword.setEnabled(false);
        buttonExecute.setEnabled(false);
        buttonExecute.setVisibility(View.GONE);
        progressBar.setVisibility(View.VISIBLE);

        model.joinGroup(groupName, groupPassword)
                .observe(getViewLifecycleOwner(), new Observer<LiveDataResponse>() {
            @Override
            public void onChanged(LiveDataResponse liveDataResponse) {
                if(liveDataResponse.isSuccessful()){
                    model.postStatus(SetupModel.STATUS_PROCEED);
                } else if(liveDataResponse.getInfo().equals("PASSWORD_REQUIRED")){
                    model.postStatus(SetupModel.STATUS_HOLD);
                    Toaster.show(getContext(),R.string.join_group_password_required);
                    initJoinPassword();
                } else {
                    switch(liveDataResponse.getInfo()){
                        case "GROUP_DOES_NOT_EXIST":
                            Toaster.show(getContext(),R.string.group_does_not_exist);
                            initJoin();
                            break;
                        case "WRONG_PASSWORD":
                            Toaster.show(getContext(),R.string.password_wrong);
                            initJoinPassword();
                            break;
                        default:
                            Toaster.show(getContext(),liveDataResponse.getInfo());
                    }
                    model.postStatus(SetupModel.STATUS_HOLD);

                }
            }
        });

    }
}