package todoc.cochlear.remoteapp.logging;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface LoggingDao
{
    @Query("SELECT * FROM logging")
    List<Logging> findAll();

    @Query("SELECT * FROM logging WHERE number = :num")
    Logging getRowByNumber(int num);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Logging logging);

    @Delete
    void delete(Logging logging);

    @Update
    void update(Logging logging);
}
