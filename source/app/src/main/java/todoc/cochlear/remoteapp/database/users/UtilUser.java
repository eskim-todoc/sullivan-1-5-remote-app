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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import todoc.cochlear.remoteapp.activity.R;

public class UtilUser
{
    static private final String TAG = "TODOC_" + UtilUser.class.getSimpleName();

    static public UtilUser instance = new UtilUser();

    private DatabaseUser mDatabase;

    static public byte[] user_iv = {(byte) 0x04, (byte) 0x81, (byte) 0x80, (byte) 0x11, (byte) 0x88, (byte) 0x01, (byte) 0x13, (byte) 0x91, (byte) 0x86, (byte) 0x43, (byte) 0x84, (byte) 0x31, (byte) 0x13, (byte) 0x18, (byte) 0x33, (byte) 0x39};
    static public String user_key = "tud4ad8ceo6pe0cj";

    public static String encByKey(String key, String value) throws Exception
    {
        return encByKey(key.getBytes(), value.getBytes());
    }

    public static String encByKey(byte[] key, byte[] value) throws Exception
    {
        SecretKeySpec secretKeySpec = new SecretKeySpec(key, "AES");
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, new IvParameterSpec(user_iv));
        byte[] randomKey = cipher.doFinal(value);
        return Base64.encodeToString(randomKey, 0);
    }

    public static String decByKey(String key, String plainText) throws Exception
    {
        return decByKey(key.getBytes(), Base64.decode(plainText, 0));
    }

    public static String decByKey(byte[] key, byte[] encText) throws Exception
    {
        SecretKeySpec secretKeySpec = new SecretKeySpec(key, "AES");
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, new IvParameterSpec(user_iv));
        byte[] secureKey = cipher.doFinal(encText);
        return new String(secureKey);
    }

    // 데이터베이스 닫기
    public void close()
    {
        if (mDatabase != null && mDatabase.isOpen())
        {
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
        }
        else
        {
            Log.d(TAG, "사용자 데이터베이스 '" + DatabaseUser.DATABASE_NAME + "'가 이미 닫혀있습니다.");
        }
    }

    // 데이터베이스 열기
    public void open(Context context)
    {
        Log.d(TAG, "DB 사용자 정보 열기 유닛 시작.");

        if (mDatabase == null)
        {
            // TD2-SW-RC-UNIT-Test-ID-1 [DB 사용자 정보 열기 유닛] 순서[2] 시작.
            /*
            new Handler(Looper.getMainLooper()).postDelayed(() ->
            {
                open(context);
            }, 100);
            */
            // TD2-SW-RC-UNIT-Test-ID-1 [DB 사용자 정보 열기 유닛] 순서[2] 끝.

            mDatabase = Room.databaseBuilder(context, DatabaseUser.class, DatabaseUser.DATABASE_NAME).addCallback(new RoomDatabase.Callback()
            {
                @Override
                public void onCreate(@NonNull SupportSQLiteDatabase db)
                {
                    super.onCreate(db);
                    db.execSQL(DatabaseUser.DATABASE_ENCODING);
                    Log.d(TAG, "사용자 데이터베이스 '" + DatabaseUser.DATABASE_NAME + "'을 생성했습니다.");
                }

                @Override
                public void onOpen(@NonNull SupportSQLiteDatabase db)
                {
                    super.onOpen(db);
                    // TD2-SW-RC-UNIT-Test-ID-1 [DB 사용자 정보 열기 유닛] 순서[1] 시작.
                    Log.d(TAG, "사용자 데이터베이스 '" + DatabaseUser.DATABASE_NAME + "'을 열었습니다.");
                    // TD2-SW-RC-UNIT-Test-ID-1 [DB 사용자 정보 열기 유닛] 순서[1] 끝.
                }
            }).addMigrations(DatabaseUser.MIGRATION_1_2).allowMainThreadQueries().build();
        }
        else
        {
            Log.d(TAG, "사용자 데이터베이스 '" + DatabaseUser.DATABASE_NAME + "'가 이미 열려있습니다.");
        }
    }

    // 데이터베이스 업데이트
    public void update(EntityUser user)
    {
        if (mDatabase != null)
        {
            try
            {
                user.passKey = UtilUser.encByKey(user_key, user.passKey);
                mDatabase.daoUser().update(user);
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
    }

    // 데이터베이스 삭제
    public void delete(EntityUser user)
    {
        if (mDatabase != null)
        {
            try
            {
                user.passKey = UtilUser.encByKey(user_key, user.passKey);
                mDatabase.daoUser().delete(user);
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
    }

    // 데이터베이스 추가
    public void insert(EntityUser user)
    {
        if (mDatabase != null)
        {
            try
            {
                user.passKey = UtilUser.encByKey(user_key, user.passKey);
                mDatabase.daoUser().insert(user);
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }

        }
    }

    // 기본사용자 가져오기
    public EntityUser getDefaultUser()
    {
        if (mDatabase != null)
        {
            EntityUser defaultUser = mDatabase.daoUser().getDefaultUser();
            if (defaultUser != null)
            {
                try
                {
                    defaultUser.passKey = UtilUser.decByKey(user_key, defaultUser.passKey);
                    return defaultUser;
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
        }

        return null;
    }

    // 활성 상태만 갱신하여 기존 사용자 키와 등록 정보를 그대로 둔다.
    public boolean setActive(String name, boolean active)
    {
        if (mDatabase == null || name == null)
        {
            return false;
        }
        try
        {
            return mDatabase.daoUser().setActive(name, active) == 1;
        }
        catch (RuntimeException error)
        {
            Log.e(TAG, "사용자 활성 상태를 저장하지 못했습니다.", error);
            return false;
        }
    }

    public enum GroupWriteResult
    {
        SAVED, NAME_IN_USE, STALE, FAILED
    }

    private static final class GroupWriteException extends RuntimeException
    {
        final GroupWriteResult result;

        GroupWriteException(GroupWriteResult result)
        {
            this.result = result;
        }
    }

    public UserGroup getGroup(String initials)
    {
        for (UserGroup group : UserGroup.fromUsers(getUsers()))
        {
            if (Objects.equals(group.initials, initials))
            {
                return group;
            }
        }
        return null;
    }

    /**
     * All old ears are checked before any row is replaced; Room rolls back every ear on failure.
     */
    public GroupWriteResult saveGroup(UserGroup original, UserGroup updated)
    {
        if (mDatabase == null || updated == null || updated.getProfiles().isEmpty())
        {
            return GroupWriteResult.FAILED;
        }
        try
        {
            List<EntityUser> expected = encryptedCopies(original);
            List<EntityUser> replacements = encryptedCopies(updated);
            mDatabase.runInTransaction(() ->
            {
                DaoUser dao = mDatabase.daoUser();
                List<EntityUser> current = dao.findAll();
                if (original != null)
                {
                    verifyGroupSnapshot(current, original.initials, expected);
                }
                for (EntityUser row : current)
                {
                    if (Objects.equals(UserGroup.initialsOf(row), updated.initials)
                        && (original == null || !Objects.equals(original.initials, updated.initials)))
                    {
                        throw new GroupWriteException(GroupWriteResult.NAME_IN_USE);
                    }
                }
                if (original != null)
                {
                    for (EntityUser row : expected)
                    {
                        if (dao.deleteByName(row.name) != 1)
                        {
                            throw new GroupWriteException(GroupWriteResult.STALE);
                        }
                    }
                }
                for (EntityUser row : replacements)
                {
                    dao.insertGroupMember(row);
                }
            });
            return GroupWriteResult.SAVED;
        }
        catch (GroupWriteException error)
        {
            return error.result;
        }
        catch (Exception error)
        {
            Log.e(TAG, "사용자 그룹을 저장하지 못했습니다.", error);
            return GroupWriteResult.FAILED;
        }
    }

    public boolean deleteGroup(UserGroup original)
    {
        if (mDatabase == null || original == null)
        {
            return false;
        }
        try
        {
            List<EntityUser> expected = encryptedCopies(original);
            mDatabase.runInTransaction(() ->
            {
                DaoUser dao = mDatabase.daoUser();
                verifyGroupSnapshot(dao.findAll(), original.initials, expected);
                for (EntityUser row : expected)
                {
                    if (dao.deleteByName(row.name) != 1)
                    {
                        throw new GroupWriteException(GroupWriteResult.STALE);
                    }
                }
            });
            return true;
        }
        catch (Exception error)
        {
            Log.e(TAG, "사용자 그룹을 삭제하지 못했습니다.", error);
            return false;
        }
    }

    private static List<EntityUser> encryptedCopies(UserGroup group) throws Exception
    {
        List<EntityUser> copies = new ArrayList<>();
        if (group != null)
        {
            for (EntityUser source : group.getProfiles())
            {
                EntityUser copy = UserGroup.copy(source);
                copy.passKey = encByKey(user_key, copy.passKey);
                copies.add(copy);
            }
        }
        return copies;
    }

    private static void verifyGroupSnapshot(List<EntityUser> current, String initials, List<EntityUser> expected)
    {
        Map<String, EntityUser> originals = new LinkedHashMap<>();
        for (EntityUser row : expected)
        {
            originals.put(row.name, row);
        }
        int count = 0;
        for (EntityUser row : current)
        {
            if (!Objects.equals(UserGroup.initialsOf(row), initials))
            {
                continue;
            }
            count++;
            EntityUser original = originals.get(row.name);
            if (original == null || !Objects.equals(original.passKey, row.passKey)
                || !Objects.equals(original.nickname, row.nickname) || !Objects.equals(original.ear, row.ear)
                || !Objects.equals(original.defaultUser, row.defaultUser) || original.isActive != row.isActive)
            {
                throw new GroupWriteException(GroupWriteResult.STALE);
            }
        }
        if (count != expected.size())
        {
            throw new GroupWriteException(GroupWriteResult.STALE);
        }
    }

    // 모든 사용자 가져오기
    public List<EntityUser> getUsers()
    {
        if (mDatabase != null)
        {
            List<EntityUser> users = mDatabase.daoUser().findAll();
            for (EntityUser user : users)
            {
                try
                {
                    user.passKey = UtilUser.decByKey(user_key, user.passKey);
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                    return null;
                }
            }

            return users;
        }

        return null;
    }

    // 이름과 일치하는 사용자 가져오기
    public EntityUser getUserByName(String name)
    {
        if (mDatabase != null)
        {
            EntityUser user = mDatabase.daoUser().getUserByName(name);
            if (user != null)
            {
                try
                {
                    user.passKey = UtilUser.decByKey(user_key, user.passKey);
                    return user;
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
        }

        return null;
    }

    // 착용위치를 제외한 이름 정보만 얻어오기
    static public String getNameOnly(String nameWithEar)
    {
        return nameWithEar.substring(0, nameWithEar.length() - (EntityUser.EAR_LEFT.length() + EntityUser.DELIMITER.length()));
    }

    // 화면에 보일 사용자 표기를 만든다 - "토닥 - 왼쪽" 처럼 이름 뒤에 착용 위치를 붙인다.
    //
    // 별명이 있으면 별명을, 없으면 이름을 쓴다. 어느 쪽이든 착용 위치는 붙는다.
    // 예전에는 별명이 있을 때 착용 위치를 빼먹어, 같은 사람의 좌/우 기기를 화면에서
    // 구분할 수 없었다.
    //
    // 이름과 착용 위치는 연결 대상을 함께 식별하므로 같은 글자 크기와 색으로 표시한다.
    static public CharSequence makeDisplayName(Context context, EntityUser user)
    {
        if (user == null)
        {
            return "";
        }

        String base = (user.nickname != null && !user.nickname.isEmpty()) //
            ? user.nickname //
            : getNameOnly(user.name);

        if (base == null)
        {
            base = "";
        }

        String ear = getEarKorean(user.ear);

        if (ear == null || ear.isEmpty())
        {
            return base;
        }

        return base + EAR_SEPARATOR + ear;
    }

    static public final String EAR_SEPARATOR = " - ";

    // 착용 위치에 대한 한글 문자열 얻어오기
    //
    // 이름과 함께 붙어 있는 형태("홍길동_L")로 들어올 수도 있어 마지막 조각을 본다.
    // F 는 착용 위치가 아니라 맵 데이터가 공장 초기화된 상태를 뜻한다.
    static public String getEarKorean(String ear)
    {
        if (ear == null)
        {
            return "";
        }

        String[] splits = ear.split(EntityUser.DELIMITER);
        String value = splits[splits.length - 1];

        if (value.equals(EntityUser.EAR_LEFT))
        {
            return EntityUser.EAR_LEFT_KR;
        }

        if (value.equals(EntityUser.EAR_RIGHT))
        {
            return EntityUser.EAR_RIGHT_KR;
        }

        if (value.equals(EntityUser.EAR_FACTORY_RESET))
        {
            return EntityUser.EAR_FACTORY_RESET_KR;
        }

        return value;
    }

    static public void copyData(EntityUser dst, EntityUser src)
    {
        if (src != null && dst != null)
        {
            dst.name = src.name;
            dst.ear = src.ear;
            dst.nickname = src.nickname;
            dst.passKey = src.passKey;
            dst.defaultUser = src.defaultUser;
            dst.isActive = src.isActive;
        }
    }
}
