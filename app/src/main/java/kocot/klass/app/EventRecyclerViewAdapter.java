package kocot.klass.app;

import android.content.Context;
import android.content.res.Resources;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;

import kocot.klass.auxiliary.DateTimeConverter;
import kocot.klass.R;
import kocot.klass.structures.CalendarEvent;

public class EventRecyclerViewAdapter extends RecyclerView.Adapter<EventRecyclerViewAdapter.EventViewHolder> {

    Context context;
    ArrayList<CalendarEvent> events = new ArrayList<>();
    private final int ALL_EVENTS = -1;
    private final int MONTH_VIEW = 1;
    private final int DAY_VIEW = 0;
    Boolean dateVisible = new Boolean(false);


    public EventRecyclerViewAdapter(Context context){
        this.context = context;
    }

    public static class EventViewHolder extends RecyclerView.ViewHolder{

        TextView time, date, title, description, creator;
        LinearLayout layoutMain;

        public EventViewHolder(@NonNull View itemView) {
            super(itemView);

            layoutMain = itemView.findViewById(R.id.linearLayoutEventViewMain);
            time = itemView.findViewById(R.id.textViewEventTime);
            date = itemView.findViewById(R.id.textViewEventDate);
            title = itemView.findViewById(R.id.textViewEventTitle);
            description = itemView.findViewById(R.id.textViewEventDescription);
            creator = itemView.findViewById(R.id.textViewEventCreator);

        }
    }



    public ArrayList<CalendarEvent> getEvents(){
        return events;
    }

    public void updateEvents(ArrayList<CalendarEvent> updatedEvents,int viewMode){

        this.events = (ArrayList<CalendarEvent>) updatedEvents.clone();
        this.dateVisible = viewMode != DAY_VIEW;


        events.sort((event1, event2) -> {
            return Long.compare(event1.getTimestampMillis(),event2.getTimestampMillis());
        });

        long now = Calendar.getInstance().getTime().getTime();
        ArrayList<CalendarEvent> listFromNow = new ArrayList<>();

        for(CalendarEvent event : this.events){

            if(DateUtils.isToday(event.getTimestampMillis())){
                listFromNow.add(event);
            }

        }

        events.removeAll(listFromNow);

        for(CalendarEvent event : this.events){
            if(event.getTimestampMillis()>now){
                listFromNow.add(event);
            }
        }
        Collections.reverse(events);
        for(CalendarEvent event : this.events){
            if(event.getTimestampMillis()<now){
                listFromNow.add(event);
            }
        }

        this.events = listFromNow;
        System.out.println(listFromNow);

        notifyDataSetChanged();

    }



    @NonNull
    @Override
    public EventRecyclerViewAdapter.EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.event_view_layout,parent,false);
        return new EventViewHolder(view);

    }

    @Override
    public void onBindViewHolder(@NonNull EventRecyclerViewAdapter.EventViewHolder holder, int position) {

        Resources resources = holder.itemView.getContext().getResources();

        holder.time.setText(DateTimeConverter.get_HHmm_fromTimeMillis(events.get(position).getTimestampMillis()));
        holder.date.setTextColor(resources.getColor(R.color.teal_darker, null));

        if(Calendar.getInstance().getTime().getTime() > events.get(position).getTimestampMillis()){
            holder.layoutMain.setBackgroundTintList(resources.getColorStateList(R.color.teal_light_semi_transparent,null));
        } else {
            holder.layoutMain.setBackgroundTintList(resources.getColorStateList(R.color.teal_darkest,null));
        }

        if(dateVisible){
            if(DateUtils.isToday(events.get(position).getTimestampMillis())){

                holder.date.setText(R.string.today);
                holder.date.setTextColor(resources.getColor(R.color.confirmation_green, null));
                holder.layoutMain.setBackgroundTintList(resources.getColorStateList(R.color.teal_darkest,null));
            } else {
                holder.date.setText(DateTimeConverter.get_DMY_fromTimeMillis(events.get(position).getTimestampMillis()));
            }
            holder.date.setVisibility(View.VISIBLE);
        } else {
            holder.date.setVisibility(View.GONE);
        }

        holder.title.setText(events.get(position).getTitle());
        holder.description.setText(events.get(position).getContent());
        holder.creator.setText(events.get(position).getCreator());

    }

    @Override
    public int getItemCount() {
        return events.size();
    }


}
