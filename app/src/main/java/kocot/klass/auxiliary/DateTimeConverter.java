package kocot.klass.auxiliary;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

public class DateTimeConverter {


    public static String get_DMY_fromTimeMillis(long timeMillis){

        Calendar calendar = new GregorianCalendar();
        calendar.setTimeInMillis(timeMillis);
        calendar.setTimeZone(Calendar.getInstance().getTimeZone());

        String day = calendar.get(Calendar.DAY_OF_MONTH) +".";
        String month = calendar.get(Calendar.MONTH) + 1 +".";
        String year = String.valueOf(calendar.get(Calendar.YEAR));

        if(day.length()<3){
            day = "0" + day;
        }

        if(month.length()<3){
            month = "0" + month;
        }

        return day+month+year;

    }


    public static String get_HHmm_fromTimeMillis(long timeMillis){

        Calendar calendar = new GregorianCalendar();
        calendar.setTimeInMillis(timeMillis);
        calendar.setTimeZone(Calendar.getInstance().getTimeZone());

        String hour = calendar.get(Calendar.HOUR_OF_DAY)+":";
        String minute = String.valueOf(calendar.get(Calendar.MINUTE));

        if(hour.length()<3){
            hour = "0" + hour;
        }
        if(minute.length()<2){
            minute = "0" + minute;
        }

        return hour+minute;

    }


}
