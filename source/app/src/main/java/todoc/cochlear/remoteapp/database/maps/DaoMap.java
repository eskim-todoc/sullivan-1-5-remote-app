package todoc.cochlear.remoteapp.database.maps;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface DaoMap
{
    @Query("SELECT * FROM EntityMap")
    List<EntityMap> getAll();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(EntityMap entityMap);

    @Delete
    void delete(EntityMap entityMap);

    @Update
    void update(EntityMap entityMap);
}
