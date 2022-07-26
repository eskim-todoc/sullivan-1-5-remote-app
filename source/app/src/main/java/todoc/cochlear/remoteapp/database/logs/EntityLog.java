package todoc.cochlear.remoteapp.database.logs;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class EntityLog
{
    @PrimaryKey
    @NonNull
    public int number; // 주키
    public String date;
    public String message;

    @NonNull
    @Override
    public String toString()
    {
        return "Logging{" +
                "number=" + number +
                ", date='" + date + '\'' +
                ", message='" + message + '\'' +
                '}';
    }
}
