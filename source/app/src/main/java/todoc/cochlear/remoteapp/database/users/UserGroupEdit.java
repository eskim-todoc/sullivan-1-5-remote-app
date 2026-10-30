package todoc.cochlear.remoteapp.database.users;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Creates a complete replacement snapshot before any database write is allowed.
 */
public final class UserGroupEdit
{
    public static final class KeyException extends IllegalArgumentException
    {
        public final String ear;

        KeyException(String ear, String message)
        {
            super(message);
            this.ear = ear;
        }
    }

    private UserGroupEdit()
    {
    }

    public static UserGroup prepare(UserGroup original, String initials, String alias, boolean aliasChanged,
                                    Map<String, String> keys, Map<String, Boolean> active)
    {
        String name = initials == null ? "" : initials;
        if (original != null && !original.initials.equals(name))
        {
            throw new IllegalArgumentException("등록된 사용자 이니셜은 변경할 수 없습니다.");
        }
        if (original == null)
        {
            name = name.trim();
        }
        if (name.isEmpty())
        {
            throw new IllegalArgumentException("사용자 이니셜을 입력하세요.");
        }
        List<EntityUser> updated = new ArrayList<>();
        if (original != null)
        {
            for (EntityUser old : original.getProfiles())
            {
                if (!UserGroup.EARS.contains(old.ear))
                {
                    EntityUser preserved = UserGroup.copy(old);
                    preserved.name = name + old.name.substring(original.initials.length());
                    if (aliasChanged)
                    {
                        preserved.nickname = alias == null ? "" : alias;
                    }
                    updated.add(preserved);
                }
            }
        }
        for (String ear : UserGroup.EARS)
        {
            EntityUser old = original == null ? null : original.get(ear);
            String key = keys.get(ear);
            key = key == null ? "" : key;
            if (key.isEmpty())
            {
                continue;
            }
            if (!validWireKey(key))
            {
                String label = EntityUser.EAR_LEFT.equals(ear) ? "왼쪽" : EntityUser.EAR_RIGHT.equals(ear) ? "오른쪽" : "초기화 장치";
                throw new KeyException(ear, label + " 내부기 키는 영문·숫자·기호 4자로 입력하세요.");
            }
            EntityUser profile = old == null ? new EntityUser() : UserGroup.copy(old);
            profile.name = name + EntityUser.DELIMITER + ear;
            profile.ear = ear;
            profile.passKey = key;
            profile.isActive = Boolean.TRUE.equals(active.get(ear));
            profile.defaultUser = old == null ? EntityUser.USER_NOT_DEFAULT : old.defaultUser;
            if (old == null || aliasChanged)
            {
                profile.nickname = alias == null ? "" : alias;
            }
            updated.add(profile);
        }
        if (updated.isEmpty())
        {
            throw new IllegalArgumentException("왼쪽 또는 오른쪽 내부기 키를 하나 이상 입력하세요.");
        }
        return new UserGroup(name, updated);
    }

    public static boolean validWireKey(String key)
    {
        if (key == null || key.length() != 4)
        {
            return false;
        }
        for (int index = 0; index < key.length(); index++)
        {
            char value = key.charAt(index);
            if (value < 0x21 || value > 0x7e)
            {
                return false;
            }
        }
        return true;
    }
}
