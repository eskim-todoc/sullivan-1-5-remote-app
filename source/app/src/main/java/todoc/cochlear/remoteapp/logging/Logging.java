package todoc.cochlear.remoteapp.logging;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class Logging
{
    @PrimaryKey
    @NonNull
    public int number; // 주키

    public String date;
    public String message;

    public int getNumber()
    {
        return number;
    }

    public void setNumber(int newNumber)
    {
        number = newNumber;
    }

    public String getDate()
    {
        return date;
    }

    public void setDate(String newData)
    {
        date = newData;
    }

    public String getMessage()
    {
        return message;
    }

    public void setMessage(String newMessage)
    {
        message = newMessage;
    }

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
