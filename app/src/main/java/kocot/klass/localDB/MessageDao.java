package kocot.klass.localDB;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.ArrayList;
import java.util.List;

import kocot.klass.structures.Message;

@Dao
public interface MessageDao {

    @Query("SELECT * FROM messages")
    List<Message> getAll();

    @Query("SELECT messageID FROM messages WHERE messageGroup LIKE (:group) AND timestampSeconds = (SELECT MAX(timestampSeconds) FROM messages)")
    String lastGroupMessageID(String group);

    @Query("SELECT * FROM messages WHERE messageGroup LIKE (:group)")
    List<Message> getAllGroupMessages(String group);

    @Query("SELECT * FROM messages WHERE messageGroup LIKE (:group) ORDER BY timestampSeconds DESC")
    LiveData<List<Message>> getLiveGroupMessages(String group);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertAll(ArrayList<Message> messages);

    @Query("DELETE FROM messages WHERE messageGroup LIKE (:group)")
    void deleteAll(String group);
    @Query("DELETE FROM messages")
    void purge();

    @Insert
    void insertMessage(Message message);

    @Delete
    void delete(Message message);

}
