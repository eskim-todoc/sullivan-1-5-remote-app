package todoc.cochlear.remoteapp.database.devices;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface DaoDevice
{
    @Query("SELECT * FROM EntityDevice")
    List<EntityDevice> findAll();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(EntityDevice entityDevice);

    @Delete
    void delete(EntityDevice entityDevice);

    @Update
    void update(EntityDevice entityDevice);

    @Query("SELECT * FROM EntityDevice WHERE serialNumber = :serialNumber")
    EntityDevice getDeviceBySerialNumber(String serialNumber);
}
