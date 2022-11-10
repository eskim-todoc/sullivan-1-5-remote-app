package todoc.cochlear.remoteapp.database.logs;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface DaoLog
{
    @Query("SELECT * FROM EntityLog")
    List<EntityLog> findAll();

    @Query("SELECT * FROM EntityLog WHERE number = 0")
    EntityLog getIndex();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(EntityLog entityLog);

    @Delete
    void delete(EntityLog entityLog);

    @Update
    void update(EntityLog entityLog);
}
