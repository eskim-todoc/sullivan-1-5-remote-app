package todoc.cochlear.remoteapp.database.devices;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class EntityDevice
{
    @PrimaryKey
    @NonNull
    public String serialNumber;
    public String pairingKey;
    public String additionalInformation;
    @ColumnInfo(defaultValue = "1")
    public boolean isActive = true;

    @NonNull
    @Override
    public String toString()
    {
        return "EntityDevice{" +
            "serialNumber='" + serialNumber + '\'' +
            ", pairingKey='[인증 정보 숨김]'" +
            ", additionalInformation='" + additionalInformation + '\'' +
            ", isActive=" + isActive +
            '}';
    }
}
