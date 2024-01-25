package kocot.klass.app;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import kocot.klass.R;
import kocot.klass.auxiliary.Toaster;

public class SetupActivity extends AppCompatActivity {

    private Boolean firstGroup;
    private SetupModel setupModel;
    private SharedPreferences preferences;
    private Button buttonJoin;
    private Button buttonCreate;

    private ImageButton buttonReturn;
    private TextView textViewPrompt;
    private LinearLayout buttonsLayout;
    private FrameLayout fragmentLayout;
    private FragmentManager fragmentManager;
    String groupName = null;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup);
        preferences = getSharedPreferences("groupList",MODE_PRIVATE);

        fragmentLayout = findViewById(R.id.fragmentContainerViewSetup);
        fragmentLayout.setVisibility(View.GONE);

        setupModel = new ViewModelProvider(this).get(SetupModel.class);
        fragmentManager = getSupportFragmentManager();

        buttonJoin = findViewById(R.id.buttonSetupJoin);
        buttonCreate = findViewById(R.id.buttonSetupCreate);
        buttonReturn = findViewById(R.id.imageButtonSetupReturn);

        textViewPrompt = findViewById(R.id.textViewSetupActivityPrompt);
        buttonsLayout = findViewById(R.id.linearLayoutSetupButtons);

        firstGroup = getIntent().getBooleanExtra("firstStart", true);

        buttonReturn.setVisibility(View.VISIBLE);
        buttonReturn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startUserSettingsActivity();
            }
        });




        buttonJoin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                buttonJoin.setEnabled(false);
                buttonCreate.setEnabled(false);
                textViewPrompt.setVisibility(View.GONE);
                buttonsLayout.setVisibility(View.GONE);
                fragmentLayout.setVisibility(View.VISIBLE);
                FragmentTransaction transaction = fragmentManager.beginTransaction();
                transaction.replace(R.id.fragmentContainerViewSetup, SetupFragment.newInstance(SetupFragment.MODE_JOIN));
                transaction.commitNow();

            }
        });

        buttonCreate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                buttonJoin.setEnabled(false);
                buttonCreate.setEnabled(false);
                textViewPrompt.setVisibility(View.GONE);
                buttonsLayout.setVisibility(View.GONE);
                fragmentLayout.setVisibility(View.VISIBLE);
                FragmentTransaction transaction = fragmentManager.beginTransaction();
                transaction.replace(R.id.fragmentContainerViewSetup, SetupFragment.newInstance(SetupFragment.MODE_CREATE));
                transaction.commitNow();



            }
        });

        setupModel.observeStatus().observe(this, new Observer<Integer>() {
            @Override
            public void onChanged(Integer integer) {
                if(integer.equals(SetupModel.STATUS_PROCEED)){
                    startMainActivity(true);
                }
            }
        });



    }

    @Override
    public void onBackPressed() {
        finish();
    }



    private void startUserSettingsActivity(){

        Intent intent = new Intent(getApplicationContext(), UserSettingsActivity.class);
        startActivity(intent);

    }

    private void startMainActivity(Boolean firstStart){

        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        intent.putExtra("firstStart",firstStart);


        startActivity(intent);
        finish();

    }
}