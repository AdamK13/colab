package kocot.klass.localDB;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;


import kocot.klass.structures.CalendarEvent;

import kocot.klass.structures.Message;
import kocot.klass.structures.Project;


@Database(entities = {Message.class, CalendarEvent.class, Project.class},version = 1)
public abstract class LocalDatabase extends RoomDatabase {

    private static LocalDatabase instance;

    public abstract MessageDao messageDao();
    public abstract CalendarEventDao calendarEventDao();
    public abstract ProjectDao projectDao();


    public static synchronized LocalDatabase getInstance(Context context){

        if(instance == null){


            instance = Room.databaseBuilder(context.getApplicationContext(),LocalDatabase.class,"LocalDB").allowMainThreadQueries().build();

        }

        return instance;
    }


}
