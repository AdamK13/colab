package kocot.klass.app;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import kocot.klass.auxiliary.DateTimeConverter;
import kocot.klass.structures.Message;
import kocot.klass.R;

public class MessageRecyclerViewAdapter extends RecyclerView.Adapter<MessageRecyclerViewAdapter.MessageViewHolder> {

    Context context;
    ArrayList<Message> messages = new ArrayList<>();


    public MessageRecyclerViewAdapter(Context context){

        this.context = context;

    }


    @Override
    public int getItemViewType(int position) {
        if (position == 0) return 1;
        else return 2;
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.message_view_layout, parent, false);
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {

        holder.content.setText(messages.get(position).getContent());
        holder.username.setText(messages.get(position).getUsername());


        long timeSeconds = messages.get(position).getTimestampSeconds();



        holder.date.setText(DateTimeConverter.get_DMY_fromTimeMillis(timeSeconds*1000));
        holder.time.setText(DateTimeConverter.get_HHmm_fromTimeMillis(timeSeconds*1000));



    }

    public ArrayList<Message> getMessages() {
        return messages;
    }

    public void insertMessage(Message message){
        if(!messages.contains(message)){
            messages.add(0,message);
            notifyItemInserted(0);
        }


    }
    public void updateMessages(ArrayList<Message> messages){

        this.messages = messages;
        notifyDataSetChanged();

    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    public static class MessageViewHolder extends RecyclerView.ViewHolder{

        TextView content, username, time, date;

        public MessageViewHolder(@NonNull View itemView) {
            super(itemView);

            content = itemView.findViewById(R.id.textViewMessageContent);
            username = itemView.findViewById(R.id.textViewMessageUsername);
            time = itemView.findViewById(R.id.textViewMessageTime);
            date = itemView.findViewById(R.id.textViewMessageDate);

        }

    }

}
