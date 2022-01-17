package todoc.cochlear.remoteapp.database;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface DeviceDao
{
    /**
     * 어떤 테이블에서 쿼리를 수행할 것인지 "FROM '테이블'"로 정의한다.
     */
    @Query("SELECT * FROM device")
    List<Device> findAll();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Device device);

    @Delete
    void delete(Device device);

    @Update
    void update(Device device);

    @Query("SELECT * FROM device WHERE deviceSerial = :serial")
    Device getDeviceBySerial(String serial);
}
