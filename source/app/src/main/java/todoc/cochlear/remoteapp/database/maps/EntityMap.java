package todoc.cochlear.remoteapp.database.maps;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class EntityMap
{
    // Primary key
    @PrimaryKey
    @NonNull
    public String name_ear;

    // Metadata
    public long stamp;
    public String name;
    public String ear;

    // Map data information
    public String serialize_map_data;

    static public String makePrimaryKey(String name, String ear)
    {
        return name + "_" + ear;
    }
}
