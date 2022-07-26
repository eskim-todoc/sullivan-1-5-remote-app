package todoc.cochlear.remoteapp.database.users;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface DaoUser
{
    @Query("SELECT * FROM EntityUser")
    List<EntityUser> findAll();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(EntityUser entityUser);

    @Delete
    void delete(EntityUser entityUser);

    @Update
    void update(EntityUser entityUser);

    @Query("SELECT * FROM EntityUser WHERE defaultUser = '" + EntityUser.USER_DEFAULT + "'")
    EntityUser getDefaultUser();

    @Query("SELECT * FROM EntityUser WHERE name = :name")
    EntityUser getUserByName(String name);
}
