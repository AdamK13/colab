package kocot.klass.localDB;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.ArrayList;
import java.util.List;

import kocot.klass.structures.CalendarEvent;

@Dao
public interface CalendarEventDao {

    @Query("SELECT * FROM calendarEvents")
    List<CalendarEvent> getAll();

    @Query("SELECT eventID FROM calendarEvents WHERE eventGroup LIKE (:group) AND eventTimestamp = (SELECT MAX(eventTimestamp) FROM calendarEvents)")
    String lastGroupEventID(String group);
    @Query("SELECT * FROM calendarEvents WHERE eventGroup LIKE (:group)")
    List<CalendarEvent> getAllGroupEvents(String group);

    @Query("SELECT * FROM calendarEvents WHERE eventGroup LIKE (:group) AND eventTimestamp BETWEEN (:from) and (:to)")
    List<CalendarEvent> getGroupEventsForPeriod(String group, long from, long to);
    @Query("SELECT * FROM calendarEvents WHERE eventGroup LIKE (:group)")
    LiveData<List<CalendarEvent>> getLiveGroupEvents(String group);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertAll(ArrayList<CalendarEvent> events);

    @Insert
    void insertEvent(CalendarEvent event);

    @Delete
    void delete(CalendarEvent event);

    @Query("DELETE FROM calendarEvents WHERE eventGroup LIKE (:group)")
    void deleteAll(String group);
    @Query("DELETE FROM calendarEvents")
    void purge();

}
