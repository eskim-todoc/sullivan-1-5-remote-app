package todoc.cochlear.remoteapp.database.devices;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface DaoDevices
{
    @Query("SELECT * FROM entitydevice")
    List<EntityDevice> findAll();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(EntityDevice entityDevice);

    @Delete
    void delete(EntityDevice entityDevice);

    @Update
    void update(EntityDevice entityDevice);

    @Query("SELECT * FROM EntityDevice WHERE serialNumber = :serialNumber")
    EntityDevice getDbDeviceBySerialNumber(String serialNumber);
}
