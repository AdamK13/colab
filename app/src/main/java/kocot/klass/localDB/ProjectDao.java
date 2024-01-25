package kocot.klass.localDB;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.ArrayList;
import java.util.List;

import kocot.klass.structures.Project;

@Dao
public interface ProjectDao {

    @Query("SELECT * FROM projects")
    List<Project> getAll();

    @Query("SELECT * FROM PROJECTS WHERE projectGroup LIKE (:group)")
    List<Project> getAllByGroup(String group);

    @Query("SELECT projectID FROM projects WHERE projectGroup LIKE (:group) AND projectCreation = (SELECT MAX(projectCreation) FROM projects)")
    String lastGroupProjectID(String group);

    @Query("SELECT * FROM projects WHERE projectGroup LIKE (:group) ORDER BY projectCreation DESC")
    LiveData<List<Project>> getLiveProjects(String group);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(ArrayList<Project> projects);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void replaceProject(Project project);

    @Delete
    void deleteList(ArrayList<Project> projects);

    @Insert
    void insertProject(Project project);

    @Delete
    void delete(Project project);

    @Query("DELETE FROM projects WHERE projectGroup LIKE (:group)")
    void deleteAll(String group);
    @Query("DELETE FROM projects")
    void purge();

}
