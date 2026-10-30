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

    @Query("SELECT * FROM EntityUser WHERE isActive = 1")
    List<EntityUser> findActive();

    @Query("UPDATE EntityUser SET isActive = :active WHERE name = :name")
    int setActive(String name, boolean active);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(EntityUser entityUser);

    @Insert(onConflict = OnConflictStrategy.ABORT)
    void insertGroupMember(EntityUser entityUser);

    @Query("DELETE FROM EntityUser WHERE name = :name")
    int deleteByName(String name);

    @Delete
    void delete(EntityUser entityUser);

    @Update
    void update(EntityUser entityUser);

    @Query("SELECT * FROM EntityUser WHERE defaultUser = '" + EntityUser.USER_DEFAULT + "'")
    EntityUser getDefaultUser();

    @Query("SELECT * FROM EntityUser WHERE name = :name")
    EntityUser getUserByName(String name);
}
