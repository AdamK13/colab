package kocot.klass.localDB;

import android.content.Context;

import androidx.lifecycle.LiveData;

import java.util.ArrayList;
import java.util.List;

import kocot.klass.structures.CalendarEvent;
import kocot.klass.structures.Message;
import kocot.klass.structures.Project;

public final class LocalDatabaseManager {

    private static LocalDatabaseManager instance;
    private LocalDatabase localDatabaseInstance;

    private MessageDao messageDao;
    private CalendarEventDao calendarEventDao;
    private ProjectDao projectDao;



    private LocalDatabaseManager(Context context){

        this.localDatabaseInstance = LocalDatabase.getInstance(context);
        this.messageDao = localDatabaseInstance.messageDao();
        this.calendarEventDao = localDatabaseInstance.calendarEventDao();
        this.projectDao = localDatabaseInstance.projectDao();


    }

    // Context should always be application context

    public static LocalDatabaseManager getInstance(Context context){

        if(instance == null){
            instance = new LocalDatabaseManager(context);
        }

        return instance;

    }

    public void updateEvents(ArrayList<CalendarEvent> events){

        calendarEventDao.insertAll(events);

    }

    public LiveData<List<CalendarEvent>> getLiveEvents(String group){

        return calendarEventDao.getLiveGroupEvents(group);

    }

    public List<CalendarEvent> getEventsForPeriod(String group, long from, long to){

        return calendarEventDao.getGroupEventsForPeriod(group,from,to);

    }

    public String getLastGroupEventId(String group){

        return calendarEventDao.lastGroupEventID(group);

    }

    public void updateMessages(ArrayList<Message> messages){

        messageDao.insertAll(messages);

    }


    public LiveData<List<Message>> getLiveMessages(String group){

        return messageDao.getLiveGroupMessages(group);

    }


    public String getLastGroupMessageID(String group){

        return messageDao.lastGroupMessageID(group);

    }

    public void deleteAll(String group){

        messageDao.deleteAll(group);
        calendarEventDao.deleteAll(group);
        projectDao.deleteAll(group);

    }

    public void purge(){
        messageDao.purge();
        calendarEventDao.purge();
        projectDao.purge();
    }

    public String getLastGroupProjectID(String group){

        return projectDao.lastGroupProjectID(group);
    }

    public void updateProjects(String group, ArrayList<Project> projects){

        List<Project> currentLocalProjects = projectDao.getAllByGroup(group);

        ArrayList<Project> listToDelete = new ArrayList<>(currentLocalProjects);
        listToDelete.removeAll(projects);

        projectDao.deleteList(listToDelete);

        projectDao.insertAll(projects);

    }

    public void forceUpdateProject(Project project){
        projectDao.replaceProject(project);
    }

    public LiveData<List<Project>> getLiveProjects(String group){

        return projectDao.getLiveProjects(group);

    }






}
