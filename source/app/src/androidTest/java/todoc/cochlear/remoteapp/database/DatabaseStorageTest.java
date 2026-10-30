package todoc.cochlear.remoteapp.database;

import android.content.Context;
import android.database.sqlite.SQLiteException;

import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.Field;

import todoc.cochlear.remoteapp.database.devices.DatabaseDevice;
import todoc.cochlear.remoteapp.database.devices.EntityDevice;
import todoc.cochlear.remoteapp.database.devices.UtilDevice;
import todoc.cochlear.remoteapp.database.logs.DatabaseLog;
import todoc.cochlear.remoteapp.database.logs.EntityLog;
import todoc.cochlear.remoteapp.database.logs.UtilLog;
import todoc.cochlear.remoteapp.database.users.DatabaseUser;
import todoc.cochlear.remoteapp.database.users.EntityUser;
import todoc.cochlear.remoteapp.database.users.UtilUser;

import static org.junit.Assert.*;

/** 인메모리 Room만 사용한다. 앱 싱글턴·실제 등록 DB·실제 인증 정보는 접근하지 않는다. */
@RunWith(AndroidJUnit4.class)
public class DatabaseStorageTest
{
    private DatabaseUser users;
    private DatabaseDevice devices;
    private DatabaseLog logs;
    private UtilUser userStore;
    private UtilDevice deviceStore;
    private UtilLog logStore;

    @Before
    public void setUp() throws Exception
    {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        users = Room.inMemoryDatabaseBuilder(context, DatabaseUser.class).allowMainThreadQueries().build();
        devices = Room.inMemoryDatabaseBuilder(context, DatabaseDevice.class).allowMainThreadQueries().build();
        logs = Room.inMemoryDatabaseBuilder(context, DatabaseLog.class).allowMainThreadQueries().build();
        userStore = new UtilUser();
        deviceStore = new UtilDevice();
        logStore = new UtilLog();
        injectDatabase(userStore, users);
        injectDatabase(deviceStore, devices);
        injectDatabase(logStore, logs);
    }

    @After
    public void tearDown()
    {
        if (users != null)
        {
            users.close();
        }
        if (devices != null)
        {
            devices.close();
        }
        if (logs != null)
        {
            logs.close();
        }
    }

    @Test
    public void repeatUserSaveKeepsCallerPlaintextAndStoredCipherCompatible() throws Exception
    {
        EntityUser user = user();
        userStore.insert(user);
        assertEquals("A1!x", user.passKey);
        assertEquals(UtilUser.encByKey(UtilUser.user_key, "A1!x"), users.daoUser().getUserByName(user.name).passKey);
        user.nickname = "edited alias";
        userStore.update(user);
        userStore.update(user);
        assertEquals("A1!x", user.passKey);
        EntityUser readback = userStore.getUserByName(user.name);
        assertEquals("A1!x", readback.passKey);
        assertEquals("edited alias", readback.nickname);
        assertFalse(readback.isActive);
    }

    @Test
    public void repeatDeviceSaveKeepsCallerAndPersistedActivationIndependent() throws Exception
    {
        EntityDevice device = device();
        deviceStore.insert(device);
        assertEquals("123456", device.pairingKey);
        assertEquals(UtilDevice.encByKey(UtilDevice.device_key, "123456"), devices.daoDevice().getDeviceBySerialNumber(device.serialNumber).pairingKey);
        device.pairingKey = "654321";
        device.isActive = true;
        device.additionalInformation = "edited alias";
        deviceStore.update(device);
        deviceStore.update(device);
        assertEquals("654321", device.pairingKey);
        assertTrue(device.isActive);
        EntityDevice readback = deviceStore.getDeviceBySerialNumber(device.serialNumber);
        assertEquals("654321", readback.pairingKey);
        assertEquals("edited alias", readback.additionalInformation);
        assertFalse(readback.isActive);
    }

    @Test
    public void deleteRequiresOnlyPrimaryKeyWithoutEncryptingCaller()
    {
        EntityUser user = user();
        userStore.insert(user);
        EntityUser userKey = new EntityUser();
        userKey.name = user.name;
        userStore.delete(userKey);
        assertNull(userKey.passKey);
        assertNull(userStore.getUserByName(user.name));

        EntityDevice device = device();
        deviceStore.insert(device);
        EntityDevice deviceKey = new EntityDevice();
        deviceKey.serialNumber = device.serialNumber;
        deviceStore.delete(deviceKey);
        assertNull(deviceKey.pairingKey);
        assertNull(deviceStore.getDeviceBySerialNumber(device.serialNumber));
    }

    @Test
    public void userDescriptionDoesNotExposeAuthenticationKey()
    {
        EntityUser user = user();
        assertFalse(user.toString().contains(user.passKey));
        assertTrue(user.toString().contains(user.name));
    }

    @Test
    public void firstAndConsecutiveLogsAdvancePointer()
    {
        logStore.writeLog("first");
        logStore.writeLog("second");
        assertEquals("3", logs.daoLog().getIndex().message);
        assertEquals(3, logs.daoLog().findAll().size());
        assertEquals("first", logAt(1).message);
        assertEquals("second", logAt(2).message);
    }

    @Test
    public void lastLogPositionWrapsToFirstWithoutOverwritingPointer()
    {
        seedPointer("2048");
        logStore.writeLog("last");
        assertEquals("1", logs.daoLog().getIndex().message);
        logStore.writeLog("wrapped");
        assertEquals("2", logs.daoLog().getIndex().message);
        assertEquals("last", logAt(2048).message);
        assertEquals("wrapped", logAt(1).message);
    }

    @Test
    public void invalidSavedPointerCannotCrashOrWriteOutsideRing()
    {
        for (String value : new String[]{null, "not-a-number", "0", "-1", "2049", "999999999999999999999"})
        {
            logs.clearAllTables();
            seedPointer(value);
            logStore.writeLog("recovered");
            assertEquals("2", logs.daoLog().getIndex().message);
            assertEquals("recovered", logAt(1).message);
            assertEquals(2, logs.daoLog().findAll().size());
        }
    }

    @Test
    public void rejectedPointerUpdateRollsBackNewLogToo()
    {
        logStore.writeLog("keep");
        logs.getOpenHelper().getWritableDatabase().execSQL(
            "CREATE TEMP TRIGGER reject_pointer BEFORE INSERT ON EntityLog "
                + "WHEN NEW.number = 0 BEGIN SELECT RAISE(ABORT, 'test pointer write failure'); END");
        try
        {
            logStore.writeLog("must rollback");
            fail("Expected injected pointer write failure");
        }
        catch (SQLiteException expected)
        {
            assertEquals("2", logs.daoLog().getIndex().message);
            assertEquals("keep", logAt(1).message);
            assertNull(logAt(2));
            assertEquals(2, logs.daoLog().findAll().size());
        }
    }

    private void seedPointer(String next)
    {
        EntityLog pointer = new EntityLog();
        pointer.number = 0;
        pointer.date = "test date";
        pointer.message = next;
        logs.daoLog().insert(pointer);
    }

    private EntityLog logAt(int number)
    {
        for (EntityLog log : logs.daoLog().findAll())
        {
            if (log.number == number)
            {
                return log;
            }
        }
        return null;
    }

    private static EntityUser user()
    {
        EntityUser user = new EntityUser();
        user.name = "TEST_L";
        user.ear = EntityUser.EAR_LEFT;
        user.passKey = "A1!x";
        user.nickname = "test alias";
        user.defaultUser = EntityUser.USER_NOT_DEFAULT;
        user.isActive = false;
        return user;
    }

    private static EntityDevice device()
    {
        EntityDevice device = new EntityDevice();
        device.serialNumber = "ABCD";
        device.pairingKey = "123456";
        device.additionalInformation = "test alias";
        device.isActive = false;
        return device;
    }

    private static void injectDatabase(Object target, RoomDatabase database) throws Exception
    {
        Field field = target.getClass().getDeclaredField("mDatabase");
        field.setAccessible(true);
        field.set(target, database);
    }
}
