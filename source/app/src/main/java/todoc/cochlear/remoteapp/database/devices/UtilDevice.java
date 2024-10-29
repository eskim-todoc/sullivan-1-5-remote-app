package todoc.cochlear.remoteapp.database.devices;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;

import java.util.List;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class UtilDevice {
    static private final String TAG = "TODOC_" + UtilDevice.class.getSimpleName();

    static public UtilDevice instance = new UtilDevice();

    private DatabaseDevice mDatabase;

    static public byte[] device_iv = {(byte) 0x04, (byte) 0x81, (byte) 0x80, (byte) 0x11, (byte) 0x88, (byte) 0x01, (byte) 0x13, (byte) 0x91, (byte) 0x86, (byte) 0x43, (byte) 0x84, (byte) 0x31, (byte) 0x13, (byte) 0x18, (byte) 0x33, (byte) 0x39};
    static public String device_key = "tud4ad8ceo6pe0cj";

    public static String encByKey(String key, String value) throws Exception {
        return encByKey(key.getBytes(), value.getBytes());
    }

    public static String encByKey(byte[] key, byte[] value) throws Exception {
        SecretKeySpec secretKeySpec = new SecretKeySpec(key, "AES");
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, new IvParameterSpec(device_iv));
        byte[] randomKey = cipher.doFinal(value);
        return Base64.encodeToString(randomKey, 0);
    }

    public static String decByKey(String key, String plainText) throws Exception {
        return decByKey(key.getBytes(), Base64.decode(plainText, 0));
    }

    public static String decByKey(byte[] key, byte[] encText) throws Exception {
        SecretKeySpec secretKeySpec = new SecretKeySpec(key, "AES");
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, new IvParameterSpec(device_iv));
        byte[] secureKey = cipher.doFinal(encText);
        return new String(secureKey);
    }

    // 데이터베이스 닫기
    public void close() {
        if (mDatabase != null && mDatabase.isOpen()) {
            mDatabase.close();
            mDatabase = null;

            // TD2-SW-RC-UNIT-Test-ID-10 [DB 기기 정보 닫기 유닛] 순서[1] 시작.
            Log.d(TAG, "기기 데이터베이스 '" + DatabaseDevice.DATABASE_NAME + "'를 닫았습니다.");
            // TD2-SW-RC-UNIT-Test-ID-10 [DB 기기 정보 닫기 유닛] 순서[1] 끝.

            // TD2-SW-RC-UNIT-Test-ID-10 [DB 기기 정보 닫기 유닛] 순서[2] 시작.
            /*
            close();
            */
            // TD2-SW-RC-UNIT-Test-ID-10 [DB 기기 정보 닫기 유닛] 순서[2] 끝.
        } else {
            Log.d(TAG, "기기 데이터베이스 '" + DatabaseDevice.DATABASE_NAME + "'가 이미 닫혀있습니다.");
        }
    }

    // 데이터베이스 열기
    public void open(Context context) {
        Log.d(TAG, "DB 기기 정보 열기 유닛 시작.");

        if (mDatabase == null) {
            // TD2-SW-RC-UNIT-Test-ID-9 [DB 기기 정보 열기 유닛] 순서[2] 시작.
            /*
            new Handler(Looper.getMainLooper()).postDelayed(() ->
            {
                open(context);
            }, 100);
            */
            // TD2-SW-RC-UNIT-Test-ID-9 [DB 기기 정보 열기 유닛] 순서[2] 끝.

            mDatabase = Room.databaseBuilder(context, DatabaseDevice.class, DatabaseDevice.DATABASE_NAME).addCallback(new RoomDatabase.Callback() {
                @Override
                public void onCreate(@NonNull SupportSQLiteDatabase db) {
                    super.onCreate(db);
                    db.execSQL(DatabaseDevice.DATABASE_ENCODING);
                    Log.d(TAG, "기기 데이터베이스 '" + DatabaseDevice.DATABASE_NAME + "'을 생성했습니다.");
                }

                @Override
                public void onOpen(@NonNull SupportSQLiteDatabase db) {
                    super.onOpen(db);
                    // TD2-SW-RC-UNIT-Test-ID-9 [DB 기기 정보 열기 유닛] 순서[1] 시작.
                    Log.d(TAG, "기기 데이터베이스 '" + DatabaseDevice.DATABASE_NAME + "'을 열었습니다.");
                    // TD2-SW-RC-UNIT-Test-ID-9 [DB 기기 정보 열기 유닛] 순서[1] 끝.
                }
            }).fallbackToDestructiveMigration().allowMainThreadQueries().build();
        } else {
            Log.d(TAG, "기기 데이터베이스 '" + DatabaseDevice.DATABASE_NAME + "'가 이미 열려있습니다.");
        }
    }

    // 데이터베이스 업데이트
    public void update(EntityDevice device) {
        if (mDatabase != null) {
            try {
                Log.d(TAG, "UtilDevice-update: device.passkey = " + device.pairingKey);
                device.pairingKey = UtilDevice.encByKey(device_key, device.pairingKey);
                Log.d(TAG, "UtilDevice-update: encrypted device.passkey = " + device.pairingKey);
                mDatabase.daoDevice().update(device);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // 데이터베이스 삭제
    public void delete(EntityDevice device) {
        if (mDatabase != null) {
            try {
                Log.d(TAG, "UtilDevice-delete: device.passkey = " + device.pairingKey);
                device.pairingKey = UtilDevice.encByKey(device_key, device.pairingKey);
                Log.d(TAG, "UtilDevice-delete: encrypted device.passkey = " + device.pairingKey);
                mDatabase.daoDevice().delete(device);
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }

    // 데이터베이스 추가
    public void insert(EntityDevice device) {
        if (mDatabase != null) {
            try {
                Log.d(TAG, "UtilDevice-insert: device.pairingKey = " + device.pairingKey);
                device.pairingKey = UtilDevice.encByKey(device_key, device.pairingKey);
                Log.d(TAG, "UtilDevice-insert: encrypted device.pairingKey = " + device.pairingKey);
                mDatabase.daoDevice().insert(device);
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }

    // 모든 기기 가져오기
    public List<EntityDevice> getDevices() {
        if (mDatabase != null) {
            List<EntityDevice> devices = mDatabase.daoDevice().findAll();
            try {
                for (EntityDevice device : devices) {
                    Log.d(TAG, "UtilDevice-getDevices: device.pairingKey = " + device.pairingKey);
                    device.pairingKey = UtilDevice.decByKey(device_key, device.pairingKey);
                    Log.d(TAG, "UtilDevice-getDevices: decrypted device.pairingKey = " + device.pairingKey);
                }
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }

            return devices;
        }

        return null;
    }

    // 제품번호와 일치하는 기기 가져오기
    public EntityDevice getDeviceBySerialNumber(String serialNumber) {
        if (mDatabase != null) {
            EntityDevice device = mDatabase.daoDevice().getDeviceBySerialNumber(serialNumber);
            if (device != null) {
                try {
                    Log.d(TAG, "UtilDevice-getDeviceBySerialNumber: device.pairingKey = " + device.pairingKey);
                    device.pairingKey = UtilDevice.decByKey(device_key, device.pairingKey);
                    Log.d(TAG, "UtilDevice-getDeviceBySerialNumber: decrypted device.pairingKey = " + device.pairingKey);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return device;
        }

        return null;
    }
}