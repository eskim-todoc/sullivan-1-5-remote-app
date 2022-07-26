package todoc.cochlear.remoteapp.database.devices;

import androidx.annotation.NonNull;
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

    @NonNull
    @Override
    public String toString()
    {
        return "EntityDevice{" +
                "serialNumber='" + serialNumber + '\'' +
                ", pairingKey='" + pairingKey + '\'' +
                ", additionalInformation='" + additionalInformation + '\'' +
                '}';
    }
}
