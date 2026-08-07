package todoc.cochlear.remoteapp.database.users;

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
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class UtilUser {
    static private final String TAG = "TODOC_" + UtilUser.class.getSimpleName();

    static public UtilUser instance = new UtilUser();

    private DatabaseUser mDatabase;

    static public byte[] user_iv = {(byte) 0x04, (byte) 0x81, (byte) 0x80, (byte) 0x11, (byte) 0x88, (byte) 0x01, (byte) 0x13, (byte) 0x91, (byte) 0x86, (byte) 0x43, (byte) 0x84, (byte) 0x31, (byte) 0x13, (byte) 0x18, (byte) 0x33, (byte) 0x39};
    static public String user_key = "tud4ad8ceo6pe0cj";

    public static String encByKey(String key, String value) throws Exception {
        return encByKey(key.getBytes(), value.getBytes());
    }

    public static String encByKey(byte[] key, byte[] value) throws Exception {
        SecretKeySpec secretKeySpec = new SecretKeySpec(key, "AES");
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, new IvParameterSpec(user_iv));
        byte[] randomKey = cipher.doFinal(value);
        return Base64.encodeToString(randomKey, 0);
    }

    public static String decByKey(String key, String plainText) throws Exception {
        return decByKey(key.getBytes(), Base64.decode(plainText, 0));
    }

    public static String decByKey(byte[] key, byte[] encText) throws Exception {
        SecretKeySpec secretKeySpec = new SecretKeySpec(key, "AES");
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, new IvParameterSpec(user_iv));
        byte[] secureKey = cipher.doFinal(encText);
        return new String(secureKey);
    }

    // 데이터베이스 닫기
    public void close() {
        if (mDatabase != null && mDatabase.isOpen()) {
            mDatabase.close();
            mDatabase = null;

            // TD2-SW-RC-UNIT-Test-ID-2 [DB 사용자 정보 닫기 유닛] 순서[1] 시작.
            Log.d(TAG, "사용자 데이터베이스 '" + DatabaseUser.DATABASE_NAME + "'를 닫았습니다.");
            // TD2-SW-RC-UNIT-Test-ID-2 [DB 사용자 정보 닫기 유닛] 순서[1] 끝.

            // TD2-SW-RC-UNIT-Test-ID-2 [DB 사용자 정보 닫기 유닛] 순서[2] 시작.
            /*
            close();
            */
            // TD2-SW-RC-UNIT-Test-ID-2 [DB 사용자 정보 닫기 유닛] 순서[2] 끝.
        } else {
            Log.d(TAG, "사용자 데이터베이스 '" + DatabaseUser.DATABASE_NAME + "'가 이미 닫혀있습니다.");
        }
    }

    // 데이터베이스 열기
    public void open(Context context) {
        Log.d(TAG, "DB 사용자 정보 열기 유닛 시작.");

        if (mDatabase == null) {
            // TD2-SW-RC-UNIT-Test-ID-1 [DB 사용자 정보 열기 유닛] 순서[2] 시작.
            /*
            new Handler(Looper.getMainLooper()).postDelayed(() ->
            {
                open(context);
            }, 100);
            */
            // TD2-SW-RC-UNIT-Test-ID-1 [DB 사용자 정보 열기 유닛] 순서[2] 끝.

            mDatabase = Room.databaseBuilder(context, DatabaseUser.class, DatabaseUser.DATABASE_NAME).addCallback(new RoomDatabase.Callback() {
                @Override
                public void onCreate(@NonNull SupportSQLiteDatabase db) {
                    super.onCreate(db);
                    db.execSQL(DatabaseUser.DATABASE_ENCODING);
                    Log.d(TAG, "사용자 데이터베이스 '" + DatabaseUser.DATABASE_NAME + "'을 생성했습니다.");
                }

                @Override
                public void onOpen(@NonNull SupportSQLiteDatabase db) {
                    super.onOpen(db);
                    // TD2-SW-RC-UNIT-Test-ID-1 [DB 사용자 정보 열기 유닛] 순서[1] 시작.
                    Log.d(TAG, "사용자 데이터베이스 '" + DatabaseUser.DATABASE_NAME + "'을 열었습니다.");
                    // TD2-SW-RC-UNIT-Test-ID-1 [DB 사용자 정보 열기 유닛] 순서[1] 끝.
                }
            }).fallbackToDestructiveMigration().allowMainThreadQueries().build();
        } else {
            Log.d(TAG, "사용자 데이터베이스 '" + DatabaseUser.DATABASE_NAME + "'가 이미 열려있습니다.");
        }
    }

    // 데이터베이스 업데이트
    public void update(EntityUser user) {
        if (mDatabase != null) {
            try {
                Log.d(TAG, "UtilUser-update: user.passkey = " + user.passKey);
                user.passKey = UtilUser.encByKey(user_key, user.passKey);
                Log.d(TAG, "UtilUser-update: encrypted user.passkey = " + user.passKey);
                mDatabase.daoUser().update(user);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // 데이터베이스 삭제
    public void delete(EntityUser user) {
        if (mDatabase != null) {
            try {
                Log.d(TAG, "UtilUser-delete: user.passkey = " + user.passKey);
                user.passKey = UtilUser.encByKey(user_key, user.passKey);
                Log.d(TAG, "UtilUser-delete: encrypted user.passkey = " + user.passKey);
                mDatabase.daoUser().delete(user);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // 데이터베이스 추가
    public void insert(EntityUser user) {
        if (mDatabase != null) {
            try {
                Log.d(TAG, "UtilUser-insert: user.passkey = " + user.passKey);
                user.passKey = UtilUser.encByKey(user_key, user.passKey);
                Log.d(TAG, "UtilUser-insert: encrypted user.passkey = " + user.passKey);
                mDatabase.daoUser().insert(user);
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }

    // 기본사용자 가져오기
    public EntityUser getDefaultUser() {
        if (mDatabase != null) {
            EntityUser defaultUser = mDatabase.daoUser().getDefaultUser();
            if (defaultUser != null) {
                try {
                    Log.d(TAG, "UtilUser-getDefaultUser: user.passkey = " + defaultUser.passKey);
                    defaultUser.passKey = UtilUser.decByKey(user_key, defaultUser.passKey);
                    Log.d(TAG, "UtilUser-getDefaultUser: decrypted user.passkey = " + defaultUser.passKey);
                    return defaultUser;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        return null;
    }

    // 모든 사용자 가져오기
    public List<EntityUser> getUsers() {
        if (mDatabase != null) {
            List<EntityUser> users = mDatabase.daoUser().findAll();
            for (EntityUser user : users) {
                try {
                    Log.d(TAG, "UtilUser-getUsers: user.passkey = " + user.passKey);
                    user.passKey = UtilUser.decByKey(user_key, user.passKey);
                    Log.d(TAG, "UtilUser-getUsers: decrypted user.passkey = " + user.passKey);
                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }
            }

            return users;
        }

        return null;
    }

    // 이름과 일치하는 사용자 가져오기
    public EntityUser getUserByName(String name) {
        if (mDatabase != null) {
            EntityUser user = mDatabase.daoUser().getUserByName(name);
            if (user != null) {
                try {
                    Log.d(TAG, "UtilUser-getUserByName: user.passkey = " + user.passKey);
                    user.passKey = UtilUser.decByKey(user_key, user.passKey);
                    Log.d(TAG, "UtilUser-getUserByName: decrypted user.passkey = " + user.passKey);
                    return user;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        return null;
    }

    // 착용위치를 제외한 이름 정보만 얻어오기
    static public String getNameOnly(String nameWithEar) {
        return nameWithEar.substring(0, nameWithEar.length() - (EntityUser.EAR_LEFT.length() + EntityUser.DELIMITER.length()));
    }

    // 착용 위치에 대한 한글 문자열 얻어오기
    //
    // 이름과 함께 붙어 있는 형태("홍길동_L")로 들어올 수도 있어 마지막 조각을 본다.
    // F 는 착용 위치가 아니라 맵 데이터가 공장 초기화된 상태를 뜻한다.
    static public String getEarKorean(String ear) {
        if (ear == null) {
            return "";
        }

        String[] splits = ear.split(EntityUser.DELIMITER);
        String   value  = splits[splits.length - 1];

        if (value.equals(EntityUser.EAR_LEFT)) {
            return EntityUser.EAR_LEFT_KR;
        }

        if (value.equals(EntityUser.EAR_RIGHT)) {
            return EntityUser.EAR_RIGHT_KR;
        }

        if (value.equals(EntityUser.EAR_FACTORY_RESET)) {
            return EntityUser.EAR_FACTORY_RESET_KR;
        }

        return value;
    }

    static public void copyData(EntityUser dst, EntityUser src) {
        if (src != null && dst != null) {
            dst.name = src.name;
            dst.ear = src.ear;
            dst.nickname = src.nickname;
            dst.passKey = src.passKey;
            dst.defaultUser = src.defaultUser;
        }
    }
}
