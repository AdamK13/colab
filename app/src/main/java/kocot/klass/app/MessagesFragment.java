package kocot.klass.app;

import android.app.Dialog;
import android.graphics.Point;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Handler;
import android.util.DisplayMetrics;
import android.view.Gravity;
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
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.rxjava3.observers.DisposableSingleObserver;
import kocot.klass.R;
import kocot.klass.auxiliary.LiveDataResponse;
import kocot.klass.auxiliary.Toaster;
import kocot.klass.structures.Message;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link MessagesFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class MessagesFragment extends Fragment {

    private RecyclerView recyclerView;
    private MessageRecyclerViewAdapter adapter;
    private MessagesModel messagesModel;
    private View statusPopupView;
    private FrameLayout baseLayout;
    private ImageButton createMessageButton;
    private ProgressBar messagesProgressBar;
    private String group;
    public MessagesFragment() {
        // Required empty public constructor
    }

    public static MessagesFragment newInstance(String group) {
        MessagesFragment messagesFragment = new MessagesFragment();
        Bundle args = new Bundle();
        args.putString("group",group);
        messagesFragment.setArguments(args);

        return messagesFragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        adapter = new MessageRecyclerViewAdapter(getContext());
        messagesModel = new ViewModelProvider(getActivity()).get(MessagesModel.class);



    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        this.group = getArguments().getString("group");
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_messages, container, false);

        baseLayout = view.findViewById(R.id.baseLayoutMessagesFragment);

        createMessageButton = view.findViewById(R.id.imageButtonCreateMessage);

        messagesProgressBar = view.findViewById(R.id.progressBarMessagesFragment);
        messagesProgressBar.setVisibility(View.VISIBLE);

        recyclerView = view.findViewById(R.id.recyclerViewMessages);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        statusPopupView = LayoutInflater.from(getContext()).inflate(R.layout.status_popup_layout,baseLayout,false);
        statusPopupView.setVisibility(View.GONE);
        baseLayout.addView(statusPopupView);
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) statusPopupView.getLayoutParams();
        params.gravity = Gravity.BOTTOM|Gravity.LEFT;

        if(messagesModel.isUserAdmin()){
            createMessageButton.setVisibility(View.VISIBLE);
        } else {
            messagesModel.isConversationOpen().subscribe(new DisposableSingleObserver<Boolean>() {
                @Override
                public void onSuccess(@io.reactivex.rxjava3.annotations.NonNull Boolean isOpen) {
                    createMessageButton.setVisibility(isOpen ? View.VISIBLE : View.GONE);
                    dispose();
                }

                @Override
                public void onError(@io.reactivex.rxjava3.annotations.NonNull Throwable e) {
                    dispose();
                }
            });
        }

        populateMessages();


        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView.setOnScrollChangeListener(new View.OnScrollChangeListener() {
            @Override
            public void onScrollChange(View view, int i, int i1, int i2, int i3) {

                if(i1>i3){
                    createMessageButton.setAlpha(0.2f);
                }
                if(i3>i1){
                    createMessageButton.setAlpha(1f);
                }

            }
        });

        createMessageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showMessageDialog();
            }
        });

    }

    private void populateMessages(){

        messagesModel.getLiveMessages(group).observe(getViewLifecycleOwner(), new Observer<List<Message>>() {
            @Override
            public void onChanged(List<Message> messages) {
                messagesProgressBar.setVisibility(View.GONE);

                ArrayList<Message> updatedList = new ArrayList<>(messages);
                ArrayList<Message> oldList = adapter.getMessages();

                int sizeDiff = updatedList.size() - oldList.size();

                if(sizeDiff > 3){
                    adapter.updateMessages(updatedList);
                } else if(sizeDiff > 0){
                    ArrayList<Message> diff = new ArrayList<>(updatedList);
                    diff.removeAll(oldList);
                    for(Message message : diff){
                        adapter.insertMessage(message);
                        recyclerView.smoothScrollToPosition(0);

                    }
                }

            }
        });


    }

    private void showMessageDialog(){


        Dialog dialog = new Dialog(getContext());
        dialog.setContentView(R.layout.dialog_send_message);
        dialog.setCancelable(true);
        dialog.getWindow().setBackgroundDrawableResource(R.color.transparent);

        DisplayMetrics metrics = getResources().getDisplayMetrics();
        int width = metrics.widthPixels;
        dialog.getWindow().setLayout(width, WindowManager.LayoutParams.WRAP_CONTENT);

        EditText messageEdit = dialog.findViewById(R.id.editTextDialogMessage);
        Button messageSendButton = dialog.findViewById(R.id.buttonDialogMessage);

        messageSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                String messageContent = messageEdit.getText().toString();

                if(messageContent.length() < 2 || messageContent.length() > 500){
                    Toaster.show(getContext(),R.string.sending_message_length_error);
                    return;
                }
                showMessageSendingStatus(0);
                messagesModel.sendMessage(group,messageContent).observe(getViewLifecycleOwner(), new Observer<LiveDataResponse>() {
                    @Override
                    public void onChanged(LiveDataResponse liveDataResponse) {

                        if(liveDataResponse.isSuccessful()){
                            showMessageSendingStatus(1);
                        } else {
                            showMessageSendingStatus(-1);
                        }

                    }
                });
                dialog.dismiss();

            }
        });


        dialog.show();

    }

    private void showMessageSendingStatus(int status){
        // -1 == ERROR ... 0 == SENDING ... 1 == SENT


        statusPopupView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                statusPopupView.setVisibility(View.GONE);
            }
        });

        TextView info = statusPopupView.findViewById(R.id.textViewStatusInfo);
        ProgressBar progress = statusPopupView.findViewById(R.id.progressBarStatus);



        switch(status){
            case 0:
                info.setText(R.string.sending_message);
                info.setTextColor(getResources().getColor(R.color.teal_dark));
                progress.setVisibility(View.VISIBLE);

                statusPopupView.setVisibility(View.VISIBLE);

                Animation statusAnimation = AnimationUtils.loadAnimation(getContext(),R.anim.fade_in);
                statusAnimation.setStartOffset(0);
                statusPopupView.startAnimation(statusAnimation);
                break;

            case -1:
                info.setText(R.string.sending_message_failure);
                info.setTextColor(getResources().getColor(R.color.error_red));
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

            case 1:
                info.setText(R.string.sending_message_successful);
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
                        } catch (Exception ignored){};


                    }
                },2500);
                break;


        }


    }
}