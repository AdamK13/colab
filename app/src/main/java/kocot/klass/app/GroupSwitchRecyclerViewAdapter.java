package kocot.klass.app;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import kocot.klass.R;
import kocot.klass.auxiliary.GroupSwitchClickRelay;

public class GroupSwitchRecyclerViewAdapter extends RecyclerView.Adapter<GroupSwitchRecyclerViewAdapter.GroupViewHolder> {

    Context context;
    ArrayList<String> groups = new ArrayList<>();



    public GroupSwitchRecyclerViewAdapter(Context context){
        this.context = context;

    }
    public static class GroupViewHolder extends RecyclerView.ViewHolder{

        private final GroupSwitchClickRelay relay = GroupSwitchClickRelay.getInstance();
        TextView tvGroup;
        public GroupViewHolder(@NonNull View itemView) {
            super(itemView);
            tvGroup = itemView.findViewById(R.id.textViewGroupSwitch);
            tvGroup.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    relay.relayClick((String) view.getTag());
                }
            });
        }
    }


    public void setGroups(ArrayList<String> groups){
        this.groups = groups;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public GroupSwitchRecyclerViewAdapter.GroupViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.group_switch_view,parent,false);
        return new GroupViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GroupViewHolder holder, int position) {

        holder.tvGroup.setText(groups.get(position));
        holder.tvGroup.setTag(groups.get(position));

    }

    @Override
    public int getItemCount() {
        return groups.size();
    }


}
