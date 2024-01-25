package kocot.klass.app;

import android.app.Dialog;
import android.graphics.Point;
import android.icu.util.Calendar;
import android.icu.util.GregorianCalendar;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Handler;
import android.text.format.DateUtils;
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
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import io.reactivex.rxjava3.observers.DisposableSingleObserver;
import kocot.klass.auxiliary.DateTimeConverter;
import kocot.klass.R;
import kocot.klass.auxiliary.DayViewInfoTag;
import kocot.klass.auxiliary.Toaster;
import kocot.klass.structures.CalendarEvent;
import kocot.klass.auxiliary.LiveDataResponse;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link CalendarFragment#newInstance} factory method to
 * create an instance of this fragment.
 */

public class CalendarFragment extends Fragment {

    private final int STATUS_CREATING = 0;
    private final int STATUS_ERROR = -1;
    private final int STATUS_CREATED = 1;

    private final int DAY_EVENTS = 0;
    private final int MONTH_EVENTS = 1;
    private final int ALL_EVENTS = -1;
    private String groupName;

    private Boolean conversationOpen = false;
    private CalendarModel calendarModel;
    private ArrayList<CalendarEvent> events = new ArrayList<>();
    private ArrayList<TextView> dayViews = new ArrayList<>();
    private ImageButton buttonPreviousMonth;
    private ImageButton buttonNextMonth;
    private TextView textViewMonth;
    private TextView dayOfMonthSub;
    private ImageButton buttonPreviousYear;
    private ImageButton buttonNextYear;
    private ImageButton buttonExpandEvents;
    private ImageButton buttonCreateEventOnDay;
    private TextView textViewYear;
    private RecyclerView eventRecycler;
    private EventRecyclerViewAdapter eventAdapter;
    private View statusPopupView;
    private View divider;
    private View selectedDayView = null;
    private FrameLayout baseLayout;
    private LinearLayout linearLayoutMonthSwitch;
    private LinearLayout linearLayoutYearSwitch;
    private TableLayout calendarView;
    private Calendar calendar = new GregorianCalendar();


    public CalendarFragment() {
        // Required empty public constructor
    }

    public static CalendarFragment newInstance(String group) {
        CalendarFragment fragment = new CalendarFragment();
        Bundle args = new Bundle();
        args.putString("groupName", group);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            groupName = getArguments().getString("groupName");
        }
        calendar.setTime(Calendar.getInstance().getTime());
        calendar.setFirstDayOfWeek(Calendar.MONDAY);
        calendar.set(Calendar.DAY_OF_MONTH,1);

        eventAdapter = new EventRecyclerViewAdapter(getContext());
        calendarModel = new ViewModelProvider(getActivity()).get(CalendarModel.class);



    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_calendar, container, false);

        ArrayList<TableRow> rows = new ArrayList<>();

        buttonPreviousMonth = view.findViewById(R.id.imageButtonPreviousMonth);
        buttonPreviousMonth.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(calendar.get(Calendar.MONTH) == Calendar.JANUARY){
                    calendar.set(Calendar.YEAR, calendar.get(Calendar.YEAR)-1);
                    calendar.set(Calendar.MONTH,Calendar.DECEMBER);
                    calendar.set(Calendar.DAY_OF_MONTH,1);
                } else {
                    calendar.set(Calendar.MONTH,calendar.get(Calendar.MONTH)-1);
                    calendar.set(Calendar.DAY_OF_MONTH,1);
                }
                fillCalendar();


            }
        });

        buttonNextMonth = view.findViewById(R.id.imageButtonNextMonth);
        buttonNextMonth.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(calendar.get(Calendar.MONTH) == Calendar.DECEMBER){
                    calendar.set(Calendar.YEAR,calendar.get(Calendar.YEAR)+1);
                    calendar.set(Calendar.MONTH,Calendar.JANUARY);
                    calendar.set(Calendar.DAY_OF_MONTH,1);
                } else {
                    calendar.set(Calendar.MONTH,calendar.get(Calendar.MONTH)+1);
                    calendar.set(Calendar.DAY_OF_MONTH,1);
                }
                fillCalendar();


            }
        });
        textViewMonth = view.findViewById(R.id.textViewMonth);

        textViewMonth.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setDayViewSelectedVisual(null);
                populateEvents(calendar.getTime(),MONTH_EVENTS);
            }
        });


        buttonPreviousYear = view.findViewById(R.id.imageButtonPreviousYear);
        buttonPreviousYear.setVisibility(View.GONE);
        buttonPreviousYear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                calendar.set(Calendar.YEAR,calendar.get(Calendar.YEAR)-1);
                fillCalendar();
            }
        });

        buttonNextYear = view.findViewById(R.id.imageButtonNextYear);
        buttonNextYear.setVisibility(View.GONE);
        buttonNextYear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                calendar.set(Calendar.YEAR,calendar.get(Calendar.YEAR)+1);
                fillCalendar();
            }
        });

        textViewYear = view.findViewById(R.id.textViewYear);
        textViewYear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(buttonPreviousYear.getVisibility() == View.GONE){
                    buttonPreviousYear.setVisibility(View.VISIBLE);
                    buttonNextYear.setVisibility(View.VISIBLE);
                } else {
                    buttonPreviousYear.setVisibility(View.GONE);
                    buttonNextYear.setVisibility(View.GONE);
                }
            }
        });

        rows.add(0,view.findViewById(R.id.rowDays1));
        rows.add(1,view.findViewById(R.id.rowDays2));
        rows.add(2,view.findViewById(R.id.rowDays3));
        rows.add(3,view.findViewById(R.id.rowDays4));
        rows.add(4,view.findViewById(R.id.rowDays5));
        rows.add(5,view.findViewById(R.id.rowDays6));

        for(TableRow row : rows){
            for (int i = (rows.indexOf(row)) *7 ; i < ( (rows.indexOf(row)) *7) +7 ; i++) {
                dayViews.add(i,(TextView) row.getChildAt(i-((rows.indexOf(row))*7)));
            }
        }

        for(TextView dayView : dayViews){
            dayView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    handleDayClick(view);
                }
            });

        }

        dayOfMonthSub = view.findViewById(R.id.textViewDayOfMonth);

        dayOfMonthSub.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                populateEvents(null, ALL_EVENTS);
            }
        });

        linearLayoutMonthSwitch = view.findViewById(R.id.linearLayoutMonthSwitch);
        linearLayoutYearSwitch = view.findViewById(R.id.linearLayoutYearSwitch);
        calendarView = view.findViewById(R.id.calendarLayout);


        buttonExpandEvents = view.findViewById(R.id.imageButtonExpandEvents);
        buttonExpandEvents.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(calendarView.getVisibility() == View.GONE){
                    calendarView.setVisibility(View.VISIBLE);
                    linearLayoutMonthSwitch.setVisibility(View.VISIBLE);
                    linearLayoutYearSwitch.setVisibility(View.VISIBLE);
                    divider.setVisibility(View.VISIBLE);
                    buttonExpandEvents.setImageDrawable(getActivity().getDrawable(R.drawable.arrow_drop_up_40px));
                } else if(calendarView.getVisibility() == View.VISIBLE){
                    calendarView.setVisibility(View.GONE);
                    linearLayoutMonthSwitch.setVisibility(View.GONE);
                    linearLayoutYearSwitch.setVisibility(View.GONE);
                    divider.setVisibility(View.GONE);
                    buttonExpandEvents.setImageDrawable(getActivity().getDrawable(R.drawable.arrow_drop_down_40px));
                }
            }
        });

        baseLayout = view.findViewById(R.id.baseLayoutCalendarFragment);

        eventRecycler = view.findViewById(R.id.recyclerViewEvents);
        eventRecycler.setLayoutManager(new LinearLayoutManager(getContext()));
        eventRecycler.setAdapter(eventAdapter);

        statusPopupView = LayoutInflater.from(getContext()).inflate(R.layout.status_popup_layout,baseLayout,false);
        statusPopupView.setVisibility(View.GONE);
        baseLayout.addView(statusPopupView);

        divider = view.findViewById(R.id.dividerCalendarFragment);
        buttonCreateEventOnDay = view.findViewById(R.id.imageButtonCreateEventOnDay);

        populateEvents(null,ALL_EVENTS);

        calendarModel.isConversationOpen().subscribe(new DisposableSingleObserver<Boolean>() {
            @Override
            public void onSuccess(@io.reactivex.rxjava3.annotations.NonNull Boolean isOpen) {
                CalendarFragment.this.conversationOpen = isOpen;
                dispose();
            }

            @Override
            public void onError(@io.reactivex.rxjava3.annotations.NonNull Throwable e) {
                dispose();
            }
        });

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        fillCalendar();

    }

    private void populateEvents( @Nullable Date date ,int mode){

        if(mode == ALL_EVENTS){
            calendarModel.getLiveEvents(groupName).observe(getViewLifecycleOwner(), new Observer<List<CalendarEvent>>() {
                @Override
                public void onChanged(List<CalendarEvent> calendarEvents) {

                    ArrayList<CalendarEvent> updatedList = new ArrayList<>(calendarEvents);
                    events = updatedList;

                    eventAdapter.updateEvents(updatedList, ALL_EVENTS);
                    fillCalendar();

                    switchEventsViewMode(null,ALL_EVENTS);

                }
            });
            return;
        }

        if(mode == MONTH_EVENTS){

            Calendar cal = (Calendar)  calendar.clone();
            cal.setTimeInMillis(date.getTime());
            cal.set(Calendar.DAY_OF_MONTH,1);
            cal.set(Calendar.HOUR_OF_DAY,0);
            cal.set(Calendar.MINUTE,0);
            cal.set(Calendar.SECOND,0);
            long from = cal.getTime().getTime();
            cal.set(Calendar.DAY_OF_MONTH,cal.getActualMaximum(Calendar.DAY_OF_MONTH));
            cal.set(Calendar.HOUR_OF_DAY,23);
            cal.set(Calendar.MINUTE,59);
            cal.set(Calendar.SECOND,59);
            long to = cal.getTime().getTime();

            ArrayList<CalendarEvent> monthEvents =
                    new ArrayList<>(calendarModel.getEventsForPeriod(groupName,from,to));

            eventAdapter.updateEvents(monthEvents,MONTH_EVENTS);
            switchEventsViewMode(date,MONTH_EVENTS);
            return;
        }

        if(mode == DAY_EVENTS){

            Calendar cal = (Calendar) calendar.clone();
            cal.setTimeInMillis(date.getTime());
            cal.set(Calendar.HOUR_OF_DAY,0);
            cal.set(Calendar.MINUTE,0);
            cal.set(Calendar.SECOND,0);
            long from = cal.getTime().getTime();
            cal.set(Calendar.HOUR_OF_DAY,23);
            cal.set(Calendar.MINUTE,59);
            cal.set(Calendar.SECOND,59);
            long to = cal.getTime().getTime();

            ArrayList<CalendarEvent> dayEvents =
                    new ArrayList<>(calendarModel.getEventsForPeriod(groupName,from,to));

            eventAdapter.updateEvents(dayEvents,DAY_EVENTS);
            switchEventsViewMode(date,DAY_EVENTS);

        }




    }


    private void fillCalendar(){

        ArrayList<Long> eventTimestamps = new ArrayList<>();
        for(CalendarEvent event : events){
            eventTimestamps.add(event.getTimestampMillis());
        }

        textViewMonth.setText(getMonthName(calendar.get(Calendar.MONTH)));
        textViewYear.setText(String.valueOf(calendar.get(Calendar.YEAR)));

        int first_day_first_week = calendar.get(Calendar.DAY_OF_WEEK)-2;
        int max_days_of_month = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
        if(first_day_first_week<0)
            first_day_first_week=6;

        for (TextView view : dayViews){
            view.setText(" ");

        }

        Calendar mutableCalendar = (Calendar) calendar.clone();
        int previousDaysRequired = first_day_first_week;
        mutableCalendar.set(Calendar.MONTH,calendar.get(Calendar.MONTH)-1);
        int maxDaysPrev = mutableCalendar.getActualMaximum(Calendar.DAY_OF_MONTH);
        mutableCalendar.setTime(calendar.getTime());

        for (int i = 0; i < previousDaysRequired; i++) {
            dayViews.get(i).setText(String.valueOf(maxDaysPrev-previousDaysRequired+i+1));
            dayViews.get(i).setTag(new DayViewInfoTag(null,DayViewInfoTag.GOTO_PREV,false));

        }

        for (int i = 0; i < max_days_of_month; i++) {

            TextView dayView = dayViews.get(i+first_day_first_week);
            dayView.setText(String.valueOf(i+1));
            mutableCalendar.set(Calendar.DAY_OF_MONTH,i+1);
            boolean containsEvent = false;
            for (Long timestamp : eventTimestamps){
                Calendar cal = (Calendar) mutableCalendar.clone();
                cal.setTimeInMillis(timestamp);
                if( (cal.get(Calendar.YEAR) == mutableCalendar.get(Calendar.YEAR)) &&
                        (cal.get(Calendar.MONTH) == mutableCalendar.get(Calendar.MONTH)) &&
                        (cal.get(Calendar.DAY_OF_MONTH) == mutableCalendar.get(Calendar.DAY_OF_MONTH)))
                {
                    containsEvent = true;
                    break;
                }
            }
            DayViewInfoTag tag = new DayViewInfoTag(mutableCalendar.getTime(),DayViewInfoTag.NO_GOTO,containsEvent);
            dayView.setTag(tag);


        }

        int day = 1;
        for (int i = max_days_of_month+first_day_first_week; i < 42; i++) {

            dayViews.get(i).setText(String.valueOf(day));
            dayViews.get(i).setTag(new DayViewInfoTag(null,DayViewInfoTag.GOTO_NEXT,false));
            day++;

        }

        setDayViewsVisual();

        setDayViewSelectedVisual(null);

    }

    private void setDayViewsVisual(){

        for(TextView dayView : dayViews){

            dayView.setBackground(null);
            dayView.setTextColor(getResources().getColor(R.color.teal_light,null));

            DayViewInfoTag infoTag = (DayViewInfoTag) dayView.getTag();
            if(infoTag.containsEvent){
                dayView.setBackground(ResourcesCompat.getDrawable(getResources(),R.drawable.background_rounded_dayview_events,null));
            }
            if(infoTag.date != null && DateUtils.isToday(infoTag.date.getTime())){
                dayView.setTextColor(getResources().getColor(R.color.teal_sharp,null));
            }
            if(infoTag.goTo != DayViewInfoTag.NO_GOTO){
                dayView.setTextColor(getResources().getColor(R.color.gray_background,null));

            }

        }

    }

    private void setDayViewSelectedVisual(View view){


        if(selectedDayView != null ){

            DayViewInfoTag tag = (DayViewInfoTag) selectedDayView.getTag();
            if(tag.containsEvent){
                selectedDayView.setBackground(ResourcesCompat.getDrawable(getResources(),R.drawable.background_rounded_dayview_events,null));
            } else {
                selectedDayView.setBackground(null);
            }

        }

        selectedDayView = view;

        if (view == null){
            return;
        }

        DayViewInfoTag tag = (DayViewInfoTag) view.getTag();

        if(tag.containsEvent){
            view.setBackground(ResourcesCompat.getDrawable(getResources(),R.drawable.background_rounded_dayview_events_selected,null));
        } else {
            view.setBackground(ResourcesCompat.getDrawable(getResources(),R.drawable.background_rounded_dayview_selected,null));
        }

    }

    private void handleDayClick( View view ){

        DayViewInfoTag tag = (DayViewInfoTag) view.getTag();

        if(tag.goTo == DayViewInfoTag.GOTO_PREV){
            buttonPreviousMonth.callOnClick();
            return;
        }
        if(tag.goTo == DayViewInfoTag.GOTO_NEXT){
            buttonNextMonth.callOnClick();
            return;
        }

        if(selectedDayView == view){
            populateEvents(null,ALL_EVENTS);
            setDayViewSelectedVisual(null);
            return;
        }
        populateEvents(tag.date,DAY_EVENTS);
        setDayViewSelectedVisual(view);

    }

    private void showEventCreationDialog(long timeMillis){

        Dialog dialog = new Dialog(getContext());
        dialog.setContentView(R.layout.dialog_create_event);
        dialog.setCancelable(true);
        dialog.getWindow().setBackgroundDrawableResource(R.color.transparent);

        String date = DateTimeConverter.get_DMY_fromTimeMillis(timeMillis);

        DisplayMetrics metrics = getResources().getDisplayMetrics();
        int width = metrics.widthPixels;
        dialog.getWindow().setLayout(width, WindowManager.LayoutParams.WRAP_CONTENT);

        Calendar eventTime = (Calendar) calendar.clone();
        eventTime.setTimeInMillis(timeMillis);

        Animation shake = AnimationUtils.loadAnimation(getContext(), R.anim.shake);
        shake.reset();

        TextView topInfo = dialog.findViewById(R.id.textViewCreateEventTopInfo);
        EditText editTime = dialog.findViewById(R.id.editTextTimeCreateEvent);
        EditText editTitle = dialog.findViewById(R.id.editTextCreateEventTitle);
        EditText editDesc = dialog.findViewById(R.id.editTextCreateEventContent);
        Button create = dialog.findViewById(R.id.buttonCreateEvent);

        topInfo.setText(getString(R.string.create_event_top_info,date));

        create.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String time = editTime.getText().toString();
                if(!time.matches("([01][0-9]|2[0-3]):([0-5][0-9])")){
                    editTime.startAnimation(shake);
                    Toaster.show(getContext(),R.string.error_event_creation_time_input);
                    return;
                }
                String title = editTitle.getText().toString();
                if(title.length() < 4 || title.length() > 40){
                    editTitle.startAnimation(shake);
                    Toaster.show(getContext(),R.string.error_event_creation_title_input);
                    return;
                }
                String desc = editDesc.getText().toString();
                if(desc.length() < 5 || desc.length() > 500){
                    editDesc.startAnimation(shake);
                    Toaster.show(getContext(),R.string.error_event_creation_desc_input);
                    return;
                }
                String[] split = time.split(":");
                eventTime.set(Calendar.HOUR_OF_DAY,Integer.parseInt(split[0]));
                eventTime.set(Calendar.MINUTE,Integer.parseInt(split[1]));
                long eventTimestamp = eventTime.getTime().getTime();

                showEventCreationStatus(STATUS_CREATING);
                calendarModel.createEvent(groupName,eventTimestamp,title,desc)
                        .observe(getViewLifecycleOwner(), new Observer<LiveDataResponse>() {
                            @Override
                            public void onChanged(LiveDataResponse liveDataResponse) {
                                if(liveDataResponse.isSuccessful()){
                                    showEventCreationStatus(STATUS_CREATED);
                                } else {
                                    showEventCreationStatus(STATUS_ERROR);
                                }
                            }
                        });

                dialog.dismiss();
            }
        });

        dialog.show();

    }

    private void showEventCreationStatus(int status){

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
                info.setText(R.string.creating_event);
                info.setTextColor(getResources().getColor(R.color.teal_dark));
                progress.setVisibility(View.VISIBLE);

                statusPopupView.setVisibility(View.VISIBLE);

                Animation statusAnimation = AnimationUtils.loadAnimation(getContext(),R.anim.fade_in);
                statusAnimation.setStartOffset(0);
                statusPopupView.startAnimation(statusAnimation);
                break;

            case STATUS_ERROR:
                info.setText(R.string.creating_event_failure);
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

            case STATUS_CREATED:
                info.setText(R.string.creating_event_successful);
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

    private void switchEventsViewMode(Date date,int viewMode){

        if(viewMode == ALL_EVENTS){

            dayOfMonthSub.setText(R.string.all_events);
            buttonCreateEventOnDay.setVisibility(View.INVISIBLE);
            buttonCreateEventOnDay.setOnClickListener(null);
            return;
        }
        if(viewMode == MONTH_EVENTS){
            java.util.Calendar calendar = new java.util.GregorianCalendar();
            calendar.setTimeInMillis(date.getTime());
            calendar.setTimeZone(java.util.Calendar.getInstance().getTimeZone());
            dayOfMonthSub.setText(getMonthName(calendar.get(java.util.Calendar.MONTH)) + " " + calendar.get(java.util.Calendar.YEAR));
            buttonCreateEventOnDay.setVisibility(View.INVISIBLE);
            buttonCreateEventOnDay.setOnClickListener(null);
            return;
        }
        if(viewMode == DAY_EVENTS){
            dayOfMonthSub.setText(DateTimeConverter.get_DMY_fromTimeMillis(date.getTime()));
            if(calendarModel.isUserAdmin() || conversationOpen){
                buttonCreateEventOnDay.setVisibility(View.VISIBLE);
                buttonCreateEventOnDay.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        showEventCreationDialog(date.getTime());
                    }
                });
            } else {
                buttonCreateEventOnDay.setVisibility(View.INVISIBLE);
                buttonCreateEventOnDay.setOnClickListener(null);
            }

        }




    }

    private String getMonthName(int month){

        switch(month){
            case 0:
                return (getString(R.string.month0));
            case 1:
                return (getString(R.string.month1));
            case 2:
                return (getString(R.string.month2));
            case 3:
                return (getString(R.string.month3));
            case 4:
                return (getString(R.string.month4));
            case 5:
                return (getString(R.string.month5));
            case 6:
                return (getString(R.string.month6));
            case 7:
                return (getString(R.string.month7));
            case 8:
                return (getString(R.string.month8));
            case 9:
                return (getString(R.string.month9));
            case 10:
                return (getString(R.string.month10));
            case 11:
                return (getString(R.string.month11));
            default:
                return " ";
        }


    }






}