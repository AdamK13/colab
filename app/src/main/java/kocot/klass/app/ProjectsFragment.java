package kocot.klass.app;

import android.app.Dialog;
import android.os.Bundle;

import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.annotation.Nullable;

import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.observers.DisposableSingleObserver;
import kocot.klass.R;
import kocot.klass.auxiliary.LiveDataResponse;
import kocot.klass.auxiliary.Toaster;
import kocot.klass.structures.Project;


public class ProjectsFragment extends Fragment {


    private final int MODE_CREATING = 0;
    private final int MODE_EDITING = 1;
    private final int STATUS_CREATING = 0;
    private final int STATUS_ERROR = -1;
    private final int STATUS_CREATED = 1;
    private View statusPopupView;
    private FrameLayout baseLayout;
    private ProjectsModel model;
    private ArrayList<HashMap<String, String>> groupMembers;
    private ArrayList<String> checkedMembers = new ArrayList<>();
    private LinearLayout layoutProjectsHolder;
    private ScrollView scrollProjectsHolder;
    private ImageButton buttonCreateProject;

    private LinearLayout createProjectHolder;
    private EditText createProjectTitleEdit;
    private EditText createProjectDescriptionEdit;
    private TextView createProjectDescriptionInfo;
    private ImageButton createProjectCancelButton;
    private ImageButton createProjectDoneButton;
    private ImageButton createProjectEditMembersButton;




    public ProjectsFragment() {
        // Required empty public constructor
    }


    public static ProjectsFragment newInstance() {
        ProjectsFragment fragment = new ProjectsFragment();
        Bundle args = new Bundle();
;
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        model = new ViewModelProvider(this).get(ProjectsModel.class);




    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_projects, container, false);

        baseLayout = view.findViewById(R.id.baseLayoutProjectsFragment);

        layoutProjectsHolder = view.findViewById(R.id.linearLayoutProjectsHolder);
        scrollProjectsHolder = view.findViewById(R.id.scrollViewProjectsHolder);

        buttonCreateProject = view.findViewById(R.id.imageButtonProjectsCreate);
        buttonCreateProject.setVisibility(View.GONE);

        createProjectHolder = view.findViewById(R.id.linearLayoutCreatingProjectHolder);
        createProjectTitleEdit = view.findViewById(R.id.editTextCreatingProjectTitle);
        createProjectDescriptionEdit = view.findViewById(R.id.editTextCreatingProjectDescription);
        createProjectDescriptionInfo = view.findViewById(R.id.textViewProjectCreatingDescriptionInfo);
        createProjectCancelButton = view.findViewById(R.id.imageButtonCreatingProjectCancel);
        createProjectDoneButton = view.findViewById(R.id.imageButtonCreatingProjectDone);
        createProjectEditMembersButton = view.findViewById(R.id.imageButtonCreatingEditMembers);

        if(model.isUserAdmin()){
            enableCreateButton();
        } else {

            model.isConversationOpen().subscribe(new DisposableSingleObserver<Boolean>() {
                @Override
                public void onSuccess(@NonNull Boolean isOpen) {
                    if(isOpen)
                        enableCreateButton();
                    else
                        disableCreateButton();
                    dispose();
                }

                @Override
                public void onError(@NonNull Throwable e) {
                    dispose();
                }
            });

        }




        populateProjects();

        model.getGroupMembers().observe(getViewLifecycleOwner(), new Observer<LiveDataResponse>() {
            @Override
            public void onChanged(LiveDataResponse liveDataResponse) {
                if(liveDataResponse.isSuccessful())
                    groupMembers = (ArrayList<HashMap<String, String>>) liveDataResponse.getData();
                else
                    groupMembers = null;
            }
        });

        statusPopupView = LayoutInflater.from(getContext()).inflate(R.layout.status_popup_layout,baseLayout,false);
        statusPopupView.setVisibility(View.GONE);
        baseLayout.addView(statusPopupView);

        return view;
    }

    private void enableCreateButton(){
        buttonCreateProject.setVisibility(View.VISIBLE);
        buttonCreateProject.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showProjectCreationView();
            }
        });
    }

    private void disableCreateButton(){
        buttonCreateProject.setVisibility(View.GONE);
        buttonCreateProject.setOnClickListener(null);
    }

    private void populateProjects(){

        LayoutInflater inflater = getLayoutInflater();

        model.getLiveProjects().observe(getViewLifecycleOwner(), new Observer<List<Project>>() {
            @Override
            public void onChanged(List<Project> projects) {
                layoutProjectsHolder.removeAllViews();
                for(Project project : projects){

                    if(project.getProjectMembers().isEmpty()){
                        continue;
                    }
                    View projectView = inflater.inflate(R.layout.project_view_layout,layoutProjectsHolder,false);
                    TextView projectTitle = projectView.findViewById(R.id.textViewProjectTitle);
                    TextView projectContent = projectView.findViewById(R.id.textViewProjectDescription);
                    ImageButton projectMembers = projectView.findViewById(R.id.imageButtonProjectMembers);
                    ImageButton projectDone = projectView.findViewById(R.id.imageButtonProjectDone);
                    LinearLayout projectMembersHolder = projectView.findViewById(R.id.linearLayoutProjectMembers);

                    if(project.getCreatorUid().equals(model.getOwnUID())
                            || model.isUserAdmin()){

                        projectDone.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                showProjectDeleteDialog(project.getGroup(),project.getProjectID());
                            }
                        });

                        projectMembers.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                showProjectMembersDialog(MODE_EDITING,project);
                            }
                        });

                    } else {

                        projectMembers.setVisibility(View.GONE);
                        projectDone.setVisibility(View.GONE);

                    }



                    for(String member : project.getProjectMembers()){
                        View memberLayout;
                        TextView memberView;
                        String[] split = member.split("::");
                        if(split[1].equals(project.getCreatorUid())){
                            memberLayout = inflater.inflate(R.layout.project_member_owner_view,projectMembersHolder,false);
                            memberView = memberLayout.findViewById(R.id.textViewProjectMemberOwnerTemplate);
                            projectMembersHolder.addView(memberLayout,0);
                        } else {
                            memberLayout = inflater.inflate(R.layout.project_member_view,projectMembersHolder,false);
                            memberView = memberLayout.findViewById(R.id.textViewProjectMemberTemplate);
                            projectMembersHolder.addView(memberLayout);
                        }
                        memberView.setText(split[0]);




                    }

                    projectTitle.setText(project.getTitle());
                    projectContent.setText(project.getContent());


                    layoutProjectsHolder.addView(projectView);

                }
            }
        });



    }

    private void showProjectCreationView(){

        scrollProjectsHolder.setVisibility(View.GONE);
        buttonCreateProject.setVisibility(View.GONE);
        createProjectHolder.setVisibility(View.VISIBLE);


        createProjectDescriptionEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                int length = editable.length();
                String description = getString(R.string.project_description) + " ("+length+"/750)";
                createProjectDescriptionInfo.setText(description);
            }
        });

        createProjectCancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                createProjectTitleEdit.getText().clear();
                createProjectDescriptionEdit.getText().clear();
                checkedMembers.clear();
                closeProjectCreationView();

            }
        });

        createProjectDoneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String title = createProjectTitleEdit.getText().toString();
                String description = createProjectDescriptionEdit.getText().toString();

                if(title.length()<3 || title.length()> 35){
                    Toaster.show(getContext(),R.string.project_creating_error_title);
                    return;
                }
                if(description.length()<20 || description.length()>750){
                    Toaster.show(getContext(),R.string.project_creating_error_description);
                    return;
                }
                showProjectCreationStatus(STATUS_CREATING);
                model.createProject(title,description, checkedMembers)
                        .observe(getViewLifecycleOwner(), new Observer<LiveDataResponse>() {
                            @Override
                            public void onChanged(LiveDataResponse liveDataResponse) {
                                if(liveDataResponse.isSuccessful()){
                                    showProjectCreationStatus(STATUS_CREATED);
                                    closeProjectCreationView();
                                    checkedMembers.clear();
                                } else {
                                    showProjectCreationStatus(STATUS_ERROR);
                                }
                            }
                        });


            }
        });

        createProjectEditMembersButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if(groupMembers == null){
                    Toaster.show(getContext(),R.string.project_creating_error_no_members);
                } else {
                    showProjectMembersDialog(MODE_CREATING, null);
                }


            }
        });




    }

    private void showProjectMembersDialog(int mode, @Nullable Project project){

        if(groupMembers == null){
            Toaster.show(getContext(),R.string.something_went_wrong);
            return;
        }

        Dialog dialog = new Dialog(getContext());
        dialog.setContentView(R.layout.dialog_project_members);
        dialog.setCancelable(true);
        dialog.getWindow().setBackgroundDrawableResource(R.color.transparent);

        LinearLayout membersHolder = dialog.findViewById(R.id.linearLayoutProjectCreationMembersHolder);
        Button buttonApply = dialog.findViewById(R.id.buttonApplyMembers);
        ProgressBar progressBar = dialog.findViewById(R.id.progressBarDialogProjectMembers);
        String ownUID = model.getOwnUID();
        ArrayList<SwitchCompat> switchList = new ArrayList<>();


        membersHolder.removeAllViews();



        for(HashMap<String,String> pair : groupMembers){

            String username = pair.get("userName");
            String uid = pair.get("userID");
            LinearLayout memberSwitchLayout = (LinearLayout) getLayoutInflater().inflate(R.layout.member_switch_layout,membersHolder,false);
            SwitchCompat memberSwitch = memberSwitchLayout.findViewById(R.id.switchMemberTemplate);
            memberSwitch.setText(username);
            memberSwitch.setTag(username+"::"+uid);
            if(mode == MODE_CREATING && uid.equals(ownUID)){
                memberSwitch.setChecked(true);
                memberSwitch.setEnabled(false);
                membersHolder.addView(memberSwitchLayout,0);
                switchList.add(memberSwitch);
                continue;
            }


            if(mode == MODE_CREATING){
                if(checkedMembers.contains(username+"::"+uid)){
                    memberSwitch.setChecked(true);
                }
            }
            if(mode == MODE_EDITING && project != null){
                boolean isProjectOwner = project.getCreatorUid().equals(uid);
                if(project.getProjectMembers() != null && project.getProjectMembers().contains(username+"::"+uid)){
                    memberSwitch.setChecked(true);
                }
                if(isProjectOwner){
                    memberSwitch.setChecked(true);
                    memberSwitch.setEnabled(false);
                }
            }

            membersHolder.addView(memberSwitchLayout);
            switchList.add(memberSwitch);


        }

        if(mode == MODE_CREATING){
            buttonApply.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    checkedMembers.clear();
                    for(SwitchCompat memberSwitch : switchList){
                        if(memberSwitch.isChecked()){
                            checkedMembers.add((String)memberSwitch.getTag());
                        }
                    }
                    dialog.dismiss();
                }
            });
        }
        if(mode == MODE_EDITING && project != null){
            buttonApply.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    ArrayList<String> newMembers = new ArrayList<>();
                    for(SwitchCompat memberSwitch : switchList){
                        if(memberSwitch.isChecked()){
                            newMembers.add((String) memberSwitch.getTag());
                        }
                    }
                    if(project.getProjectMembers().containsAll(newMembers)
                            && newMembers.containsAll(project.getProjectMembers())){
                        dialog.dismiss();
                    } else {
                        buttonApply.setVisibility(View.GONE);
                        progressBar.setVisibility(View.VISIBLE);
                        model.editProjectMembers(project.getGroup(), project.getProjectID(), newMembers)
                                .observe(getViewLifecycleOwner(), new Observer<LiveDataResponse>() {
                                    @Override
                                    public void onChanged(LiveDataResponse liveDataResponse) {
                                        if(liveDataResponse.isSuccessful()){
                                            dialog.dismiss();
                                            model.forceUpdateProject(project.getProjectID());
                                        } else {
                                            dialog.dismiss();
                                            Toaster.show(getContext(),R.string.something_went_wrong);
                                        }
                                    }
                                });
                    }
                }
            });
        }


        DisplayMetrics metrics = getResources().getDisplayMetrics();
        int width = metrics.widthPixels;
        dialog.getWindow().setLayout(width, WindowManager.LayoutParams.WRAP_CONTENT);



        dialog.show();

    }

    private void showProjectDeleteDialog(String group,String projectID){

        Dialog dialog = new Dialog(getContext());
        dialog.setContentView(R.layout.dialog_warning_deleting_project);
        dialog.setCancelable(true);
        dialog.getWindow().setBackgroundDrawableResource(R.color.transparent);

        Button delete = dialog.findViewById(R.id.buttonDeleteProject);
        Button cancel = dialog.findViewById(R.id.buttonCancelDeleting);

        ProgressBar progressBar = dialog.findViewById(R.id.progressBarDeletingProject);
        progressBar.setVisibility(View.GONE);

        delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                progressBar.setVisibility(View.VISIBLE);
                delete.setVisibility(View.GONE);
                cancel.setVisibility(View.GONE);

                model.deleteProject(group,projectID)
                        .observe(getViewLifecycleOwner(), new Observer<LiveDataResponse>() {
                            @Override
                            public void onChanged(LiveDataResponse liveDataResponse) {
                                if(liveDataResponse.isSuccessful()){
                                    dialog.dismiss();
                                    model.updateProjects(group);
                                } else {
                                    Toaster.show(getContext(),liveDataResponse.getInfo());
                                    progressBar.setVisibility(View.GONE);
                                    delete.setVisibility(View.VISIBLE);
                                    cancel.setVisibility(View.VISIBLE);
                                }
                            }
                        });
            }
        });

        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });

        dialog.show();

    }

    private void closeProjectCreationView(){

        createProjectHolder.setVisibility(View.GONE);
        scrollProjectsHolder.setVisibility(View.VISIBLE);
        buttonCreateProject.setVisibility(View.VISIBLE);
        createProjectTitleEdit.getText().clear();
        createProjectDescriptionEdit.getText().clear();
        checkedMembers.clear();


    }

    private void showProjectCreationStatus(int status){

        // -1 == ERROR ... 0 == CREATING ... 1 == CREATED

        statusPopupView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                statusPopupView.setVisibility(View.GONE);
            }
        });

        TextView info = statusPopupView.findViewById(R.id.textViewStatusInfo);
        ProgressBar progress = statusPopupView.findViewById(R.id.progressBarStatus);



        switch(status){
            case STATUS_CREATING:
                info.setText(R.string.creating_project);
                info.setTextColor(getResources().getColor(R.color.teal_dark));
                progress.setVisibility(View.VISIBLE);

                statusPopupView.setVisibility(View.VISIBLE);

                Animation statusAnimation = AnimationUtils.loadAnimation(getContext(),R.anim.fade_in);
                statusAnimation.setStartOffset(0);
                statusPopupView.startAnimation(statusAnimation);
                break;

            case STATUS_ERROR:
                info.setText(R.string.creating_project_failure);
                info.setTextColor(getResources().getColor(R.color.error_red));
                progress.setVisibility(View.GONE);

                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Animation statusAnimation = AnimationUtils.loadAnimation(getContext(),R.anim.fade_out);
                        statusAnimation.setStartOffset(0);
                        statusPopupView.startAnimation(statusAnimation);
                        statusPopupView.setVisibility(View.GONE);
                    }
                },2500);
                break;

            case STATUS_CREATED:
                info.setText(R.string.creating_project_successful);
                info.setTextColor(getResources().getColor(R.color.confirmation_green));
                progress.setVisibility(View.GONE);


                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        try{
                            Animation statusAnimation = AnimationUtils.loadAnimation(getContext(),R.anim.fade_out);
                            statusAnimation.setStartOffset(0);
                            statusPopupView.startAnimation(statusAnimation);
                            statusPopupView.setVisibility(View.GONE);
                        } catch (Exception ignored){}


                    }
                },2500);
                break;


        }

    }


}