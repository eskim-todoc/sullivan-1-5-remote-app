package todoc.cochlear.remoteapp.connection;

import java.util.List;
import java.util.Objects;

import todoc.cochlear.remoteapp.database.users.EntityUser;

/**
 * 현재 GATT의 인증값은 고정하고, 등록 이름·별칭·향후 인증값은 별도로 갱신한다.
 */
public final class SessionUserProfile
{
    private final String authenticationPassKey;
    private EntityUser registration;

    public SessionUserProfile(EntityUser initial)
    {
        authenticationPassKey = initial == null ? null : initial.passKey;
        registration = copy(initial);
    }

    public String authenticationPassKey()
    {
        return authenticationPassKey;
    }

    public EntityUser registrationSnapshot()
    {
        return copy(registration);
    }

    public boolean canUpdate(EntityUser updated, boolean exclusiveBusy)
    {
        if (updated == null)
        {
            return !exclusiveBusy;
        }
        return updated != null && registration != null && Objects.equals(registration.ear, updated.ear)
            && (updated.isActive || !exclusiveBusy);
    }

    public void updateRegistration(EntityUser updated)
    {
        if (updated == null)
        {
            registration = null;
            return;
        }
        if (registration == null || !Objects.equals(registration.ear, updated.ear))
        {
            throw new IllegalArgumentException("기존 세션과 같은 착용 위치의 등록값이 필요합니다.");
        }
        registration = copy(updated);
    }

    public static EntityUser replacementFor(EntityUser current, List<String> oldNames, List<EntityUser> updated)
    {
        if (!belongsToGroup(current, oldNames) || updated == null)
        {
            return null;
        }
        for (EntityUser candidate : updated)
        {
            if (candidate != null && Objects.equals(current.ear, candidate.ear))
            {
                return copy(candidate);
            }
        }
        return null;
    }

    public static boolean belongsToGroup(EntityUser current, List<String> oldNames)
    {
        return current != null && oldNames != null && oldNames.contains(current.name);
    }

    private static EntityUser copy(EntityUser source)
    {
        if (source == null)
        {
            return null;
        }
        EntityUser copy = new EntityUser();
        copy.name = source.name;
        copy.ear = source.ear;
        copy.nickname = source.nickname;
        copy.passKey = source.passKey;
        copy.defaultUser = source.defaultUser;
        copy.isActive = source.isActive;
        return copy;
    }
}
