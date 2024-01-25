package kocot.klass.auxiliary;

import androidx.annotation.Nullable;

import java.util.Date;

public class DayViewInfoTag {

    public static final int GOTO_PREV = -1;
    public static final int NO_GOTO = 0;
    public static final int GOTO_NEXT = 1;

    public final Date date;
    public final int goTo;
    public final boolean containsEvent;


    public DayViewInfoTag(@Nullable Date date, int goTo, boolean containsEvent){

        this.date = date;
        this.goTo = goTo;
        this.containsEvent = containsEvent;

    }

}
