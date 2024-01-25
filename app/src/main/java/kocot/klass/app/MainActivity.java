package kocot.klass.app;


import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.observers.DisposableObserver;
import kocot.klass.R;
import kocot.klass.auxiliary.GroupSwitchClickRelay;
import kocot.klass.auxiliary.Toaster;
import kocot.klass.auxiliary.LiveDataResponse;

public class MainActivity extends AppCompatActivity {


    private final String MSG_fragmentTag = "MESSAGES";
    private final String CAL_fragmentTag = "CALENDAR";
    private final String PRO_fragmentTag = "PROJECTS";
    private MainActivityModel mainModel;
    private LinearLayout toolbarLayout;
    private LinearLayout chipLayout;
    private TextView textViewGroupName;
    private ImageButton adminSettingsButton;
    private ImageButton userSettingsButton;
    private ConstraintLayout baseLayout;
    private FragmentManager manager;
    private Button chipMessages;
    private Button chipCalendar;
    private Button chipProjects;
    private Dialog switchGroupDialog;
    private GroupSwitchRecyclerViewAdapter groupSwitchAdapter;
    private boolean initPartUser = false, initPartGroups = false;
    private String openGroup = null;
    private volatile boolean initCalled = false;





    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        baseLayout = findViewById(R.id.baseMainActivityLayout);

        toolbarLayout = findViewById(R.id.toolbarLayout);
        chipLayout = findViewById(R.id.chipButtonLayout);
        chipMessages = chipLayout.findViewById(R.id.chipButtonMessages);
        chipCalendar = chipLayout.findViewById(R.id.chipButtonCalendar);
        chipProjects = chipLayout.findViewById(R.id.chipButtonProjects);



        textViewGroupName = findViewById(R.id.textViewToolbarGroupName);
        adminSettingsButton = findViewById(R.id.imageButtonAdminSettings);
        adminSettingsButton.setVisibility(View.GONE);
        userSettingsButton = findViewById(R.id.imageButtonToolbarSettings);



        manager = getSupportFragmentManager();
        mainModel = new ViewModelProvider(this).get(MainActivityModel.class);

        init();
        initCalled = true;




        chipMessages.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                setCurrentlyDisplayedFragmentUI(MSG_fragmentTag);
                String currentFragment = getCurrentlyDisplayedFragment();
                if(currentFragment.equals(MSG_fragmentTag)){
                    return;
                }

                FragmentTransaction transaction = manager.beginTransaction()
                                .setCustomAnimations(R.anim.slide_in_left,R.anim.slide_out_right);
                transaction.replace(R.id.fragmentContainerView, MessagesFragment.newInstance(mainModel.getCurrentGroup()),MSG_fragmentTag);
                transaction.commitNow();

            }
        });

        chipCalendar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                setCurrentlyDisplayedFragmentUI(CAL_fragmentTag);
                String currentFragment = getCurrentlyDisplayedFragment();
                if(currentFragment.equals(CAL_fragmentTag)){
                    return;
                }
                if(currentFragment.equals(PRO_fragmentTag)){
                    FragmentTransaction transaction = manager.beginTransaction()
                            .setCustomAnimations(R.anim.slide_in_left,R.anim.slide_out_right);
                    transaction.replace(R.id.fragmentContainerView,CalendarFragment.newInstance(mainModel.getCurrentGroup()),CAL_fragmentTag);
                    transaction.commitNow();
                    return;
                }

                FragmentTransaction transaction = manager.beginTransaction()
                                .setCustomAnimations(R.anim.slide_in_right,R.anim.slide_out_left);
                transaction.replace(R.id.fragmentContainerView,CalendarFragment.newInstance(mainModel.getCurrentGroup()),CAL_fragmentTag);
                transaction.commitNow();
            }
        });

        chipProjects.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                setCurrentlyDisplayedFragmentUI(PRO_fragmentTag);
                String currentFragment = getCurrentlyDisplayedFragment();
                if(currentFragment.equals(PRO_fragmentTag)){
                    return;
                }

                FragmentTransaction transaction = manager.beginTransaction()
                        .setCustomAnimations(R.anim.slide_in_right,R.anim.slide_out_left);
                transaction.replace(R.id.fragmentContainerView,ProjectsFragment.newInstance(),PRO_fragmentTag);
                transaction.commitNow();
            }
        });



        textViewGroupName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showPopupGroupSwitch();
            }
        });

        groupSwitchAdapter = new GroupSwitchRecyclerViewAdapter(this);

        GroupSwitchClickRelay relay = GroupSwitchClickRelay.getInstance();
        relay.getRelay().observe(this, new Observer<String>() {
            @Override
            public void onChanged(String group) {
                mainModel.setGroup(group);
                if(switchGroupDialog != null)
                    switchGroupDialog.dismiss();
                init();
            }
        });

    }

    private void init(){

        if(!mainModel.isUserSignedIn()){
            returnToLauncher();
            return;
        }

        mainModel.init().subscribe(new DisposableObserver<LiveDataResponse>() {
            @Override
            public void onNext(@NonNull LiveDataResponse liveDataResponse) {
                String part = liveDataResponse.getInfo();
                if(part.equals("USER")){
                    initPartUser = true;
                    userSettingsButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            startUserSettingsActivity();
                        }
                    });
                }
                if(part.equals("GROUPS")){
                    initPartGroups = true;
                    if(!liveDataResponse.isSuccessful()){
                        Toaster.show(MainActivity.this,(String) liveDataResponse.getData());
                        return;
                    }
                    String initMode = (String) liveDataResponse.getData();

                    mainModel.setGroup(null);

                    textViewGroupName.setText(mainModel.getCurrentGroup());
                    checkIfAdmin();

                    FragmentTransaction transaction = manager.beginTransaction();
                    transaction.replace(R.id.fragmentContainerView, MessagesFragment.newInstance(mainModel.getCurrentGroup()),MSG_fragmentTag);
                    transaction.addToBackStack(null);
                    transaction.commit();
                    setCurrentlyDisplayedFragmentUI(MSG_fragmentTag);

                    if(initMode.equals("initOffline")){
                        Toaster.show(MainActivity.this,R.string.offline_mode);
                    }
                }
                if(initPartUser&&initPartGroups){
                    initPartUser = false;
                    initPartGroups = false;
                    openGroup = mainModel.getCurrentGroup();
                    dispose();

                }
            }

            @Override
            public void onError(@NonNull Throwable e) {
                if(e.getMessage().equals("No user")){
                    returnToLauncher();
                    return;
                }
                Toaster.show(MainActivity.this,e.getMessage());
            }

            @Override
            public void onComplete() {
            }
        });


    }

    @Override
    protected void onResume() {
        super.onResume();
        setCurrentlyDisplayedFragmentUI(getCurrentlyDisplayedFragment());
        if(initCalled){
            initCalled = false;
        } else {
            if(!mainModel.groupAvailable(openGroup)){
                init();
            }
        }
    }

    private String getCurrentlyDisplayedFragment(){

         List<Fragment> fragments = manager.getFragments();

         for(Fragment fragment : fragments){
             if(fragment.isVisible()){
                 return fragment.getTag() != null ? fragment.getTag() : "NONE";
             }
         }

         return "NONE";

    }

    private void setCurrentlyDisplayedFragmentUI(String fragmentTag){

        Drawable selected = AppCompatResources
                .getDrawable(getApplicationContext(),R.drawable.background_rounded_border_fill_green);
        Drawable not_selected = AppCompatResources
                .getDrawable(getApplicationContext(),R.drawable.background_rounded_border_fill);

        switch(fragmentTag){

            case "MESSAGES":
                chipCalendar.setBackground(not_selected);
                chipProjects.setBackground(not_selected);
                chipMessages.setBackground(selected);
                break;

            case "CALENDAR":
                chipMessages.setBackground(not_selected);
                chipProjects.setBackground(not_selected);
                chipCalendar.setBackground(selected);
                break;

            case "PROJECTS":
                chipMessages.setBackground(not_selected);
                chipCalendar.setBackground(not_selected);
                chipProjects.setBackground(selected);
                break;


        }

    }

    private void checkIfAdmin(){

        mainModel.isAdmin().observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean isAdmin) {
                if(isAdmin==null){
                    returnToLauncher();
                    return;
                }
                if(isAdmin){
                    adminSettingsButton.setVisibility(View.VISIBLE);
                    adminSettingsButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            startSettingsActivity();
                        }
                    });
                } else {
                    adminSettingsButton.setVisibility(View.GONE);
                    adminSettingsButton.setOnClickListener(null);
                }
                mainModel.saveAdminStatus(isAdmin);


            }
        });

    }

    private void showPopupGroupSwitch(){


        ArrayList<String> groups = mainModel.getGroups();


        switchGroupDialog = new Dialog(this);
        switchGroupDialog.setContentView(R.layout.popup_switch_group);
        switchGroupDialog.setCancelable(true);
        switchGroupDialog.getWindow().setBackgroundDrawableResource(R.color.transparent);


        DisplayMetrics metrics = getResources().getDisplayMetrics();
        int width = metrics.widthPixels;
        switchGroupDialog.getWindow().setLayout(width, WindowManager.LayoutParams.WRAP_CONTENT);

        RecyclerView groupsRecycler = switchGroupDialog.findViewById(R.id.recyclerViewSwitchGroup);
        groupsRecycler.setLayoutManager(new LinearLayoutManager(switchGroupDialog.getContext()));
        groupsRecycler.setAdapter(groupSwitchAdapter);
        groupSwitchAdapter.setGroups(groups);

        Button joinOrCreateButton = switchGroupDialog.findViewById(R.id.buttonJoinOrCreateGroup);

        joinOrCreateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!mainModel.isInternetAccessible()){
                    Toaster.show(MainActivity.this,R.string.no_internet);
                    return;
                }
                switchGroupDialog.dismiss();
                startSetupActivity();
                finish();
            }
        });



        switchGroupDialog.show();

    }



    private void startSettingsActivity(){

        if(mainModel.isInternetAccessible()){
            Intent intent = new Intent(this,GroupAdminActivity.class);
            startActivity(intent);
        } else {
            Toaster.show(this,R.string.no_internet);
        }


    }

    private void startUserSettingsActivity(){

        Intent intent = new Intent(this, UserSettingsActivity.class);
        startActivity(intent);

    }

    private void returnToLauncher(){

        Intent intent = new Intent(getApplicationContext(), LauncherActivity.class);
        startActivity(intent);
        finish();

    }

    private void startSetupActivity(){

        Intent intent = new Intent(getApplicationContext(), SetupActivity.class);
        intent.putExtra("firstStart",false);
        startActivity(intent);

    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}



