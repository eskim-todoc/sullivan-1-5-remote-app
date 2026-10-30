package todoc.cochlear.remoteapp.database.users;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A display/edit group; BLE authentication remains attached to its individual ear record.
 */
public final class UserGroup
{
    public static final List<String> EARS = Collections.unmodifiableList(Arrays.asList(
        EntityUser.EAR_LEFT, EntityUser.EAR_RIGHT, EntityUser.EAR_FACTORY_RESET));

    public final String initials;
    private final Map<String, EntityUser> members = new LinkedHashMap<>();

    public UserGroup(String initials, List<EntityUser> profiles)
    {
        this.initials = initials == null ? "" : initials;
        if (profiles != null)
        {
            for (EntityUser profile : profiles)
            {
                if (profile != null)
                {
                    members.put(profile.ear, copy(profile));
                }
            }
        }
    }

    public EntityUser get(String ear)
    {
        return members.get(ear);
    }

    public List<EntityUser> getProfiles()
    {
        return new ArrayList<>(members.values());
    }

    public List<String> getNames()
    {
        List<String> names = new ArrayList<>();
        for (EntityUser member : members.values())
        {
            names.add(member.name);
        }
        return names;
    }

    public String aliasSummary()
    {
        Set<String> aliases = new LinkedHashSet<>();
        for (EntityUser member : members.values())
        {
            String alias = member.nickname == null ? "" : member.nickname.trim();
            if (!alias.isEmpty())
            {
                aliases.add(alias);
            }
        }
        return String.join(" / ", aliases);
    }

    public String displayName()
    {
        String alias = aliasSummary();
        return initials + (alias.isEmpty() ? "" : " (" + alias + ")");
    }

    public static List<UserGroup> fromUsers(List<EntityUser> users)
    {
        Map<String, List<EntityUser>> grouped = new LinkedHashMap<>();
        if (users != null)
        {
            for (EntityUser user : users)
            {
                if (user != null)
                {
                    String initials = initialsOf(user);
                    grouped.computeIfAbsent(initials, key -> new ArrayList<>()).add(user);
                }
            }
        }
        List<UserGroup> result = new ArrayList<>();
        for (Map.Entry<String, List<EntityUser>> entry : grouped.entrySet())
        {
            result.add(new UserGroup(entry.getKey(), entry.getValue()));
        }
        return result;
    }

    public static String initialsOf(EntityUser user)
    {
        if (user == null || user.name == null)
        {
            return "";
        }
        String suffix = EntityUser.DELIMITER + user.ear;
        return user.ear != null && user.name.endsWith(suffix)
            ? user.name.substring(0, user.name.length() - suffix.length()) : user.name;
    }

    public static EntityUser copy(EntityUser source)
    {
        EntityUser target = new EntityUser();
        target.name = source.name;
        target.ear = source.ear;
        target.nickname = source.nickname;
        target.passKey = source.passKey;
        target.defaultUser = source.defaultUser;
        target.isActive = source.isActive;
        return target;
    }
}
