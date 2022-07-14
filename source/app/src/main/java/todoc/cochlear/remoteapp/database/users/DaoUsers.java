package todoc.cochlear.remoteapp.database.users;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface DaoUsers
{
    @Query("SELECT * FROM EntityUser")
    List<EntityUser> findAll();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(EntityUser entityUser);

    @Delete
    void delete(EntityUser entityUser);

    @Update
    void update(EntityUser entityUser);

    @Query("SELECT * FROM EntityUser WHERE name = :name")
    EntityUser getDbUserByName(String name);

    @Query("SELECT * FROM EntityUser WHERE defaultUser = :defaultUser")
    EntityUser getDbUserByDefaultUSer(String defaultUser);
}
