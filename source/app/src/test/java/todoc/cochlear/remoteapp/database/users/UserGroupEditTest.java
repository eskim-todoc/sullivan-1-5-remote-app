package todoc.cochlear.remoteapp.database.users;

import org.junit.Test;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class UserGroupEditTest
{
    private static EntityUser profile(String initials, String ear, String key, String alias, boolean active)
    {
        EntityUser user = new EntityUser();
        user.name = initials + "_" + ear;
        user.ear = ear;
        user.passKey = key;
        user.nickname = alias;
        user.defaultUser = EntityUser.USER_NOT_DEFAULT;
        user.isActive = active;
        return user;
    }

    private static Map<String, String> keys(String left, String right)
    {
        Map<String, String> keys = new LinkedHashMap<>();
        keys.put(EntityUser.EAR_LEFT, left);
        keys.put(EntityUser.EAR_RIGHT, right);
        return keys;
    }

    private static Map<String, Boolean> active(boolean left, boolean right)
    {
        Map<String, Boolean> active = new LinkedHashMap<>();
        active.put(EntityUser.EAR_LEFT, left);
        active.put(EntityUser.EAR_RIGHT, right);
        return active;
    }

    @Test
    public void groupsOnePersonWithoutCombiningDifferentEarKeysOrInitials()
    {
        List<UserGroup> grouped = UserGroup.fromUsers(Arrays.asList(
            profile("PEK", "L", "1111", "name", true),
            profile("PEK", "R", "2222", "name", false),
            profile("OTHER", "L", "3333", "", true)));
        assertEquals(2, grouped.size());
        assertEquals("PEK (name)", grouped.get(0).displayName());
        assertEquals("1111", grouped.get(0).get("L").passKey);
        assertEquals("2222", grouped.get(0).get("R").passKey);
        assertFalse(grouped.get(0).get("R").isActive);
    }

    @Test
    public void absentSideDoesNotBecomeAnActiveProfileWhenNoKeyExists()
    {
        UserGroup updated = UserGroupEdit.prepare(null, "PEK", "name", true,
            keys("1111", ""), active(true, true));
        assertEquals(1, updated.getProfiles().size());
        assertNull(updated.get("R"));
        assertTrue(updated.get("L").isActive);
    }

    @Test
    public void editingOneEarPreservesOtherEarKeyAliasAndOriginalSnapshot()
    {
        UserGroup original = new UserGroup("PEK", Arrays.asList(
            profile("PEK", "L", "1111", "left alias", true),
            profile("PEK", "R", "2222", "right alias", true)));
        UserGroup updated = UserGroupEdit.prepare(original, "PEK", original.aliasSummary(), false,
            keys("4444", "2222"), active(false, true));
        assertFalse(updated.get("L").isActive);
        assertEquals("4444", updated.get("L").passKey);
        assertTrue(updated.get("R").isActive);
        assertEquals("2222", updated.get("R").passKey);
        assertEquals("right alias", updated.get("R").nickname);
        assertEquals("1111", original.get("L").passKey);
        assertTrue(original.get("L").isActive);
    }

    @Test
    public void registeredInitialsCannotChangeOrMutateEitherEarSnapshot()
    {
        UserGroup original = new UserGroup("PEK", Arrays.asList(
            profile("PEK", "L", "1111", "left", true),
            profile("PEK", "R", "2222", "right", false)));
        try
        {
            UserGroupEdit.prepare(original, "NEW", "changed", true,
                keys("4444", "9999"), active(false, true));
            fail("Registered initials must remain fixed");
        }
        catch (IllegalArgumentException expected)
        {
            assertTrue(expected.getMessage().contains("이니셜은 변경할 수 없습니다"));
        }
        assertEquals(Arrays.asList("PEK_L", "PEK_R"), original.getNames());
        assertEquals("1111", original.get("L").passKey);
        assertEquals("2222", original.get("R").passKey);
        assertEquals("left", original.get("L").nickname);
        assertEquals("right", original.get("R").nickname);
        assertTrue(original.get("L").isActive);
        assertFalse(original.get("R").isActive);
    }

    @Test
    public void invalidSecondKeyRejectsWholeEditWithoutChangingFirstEar()
    {
        UserGroup original = new UserGroup("PEK", Arrays.asList(
            profile("PEK", "L", "1111", "name", true),
            profile("PEK", "R", "2222", "name", true)));
        try
        {
            UserGroupEdit.prepare(original, "PEK", "changed", true,
                keys("4444", "22"), active(false, false));
            fail("Incomplete right key must reject the whole edit");
        }
        catch (IllegalArgumentException expected)
        {
            assertTrue(expected instanceof UserGroupEdit.KeyException);
            assertEquals("1111", original.get("L").passKey);
            assertEquals("2222", original.get("R").passKey);
            assertEquals("name", original.get("L").nickname);
            assertTrue(original.get("L").isActive);
        }
    }

    @Test
    public void factoryRecordAndKeysSurviveSameInitialGroupEditAlongsideBothEars()
    {
        UserGroup original = new UserGroup("PEK", Arrays.asList(
            profile("PEK", "L", "1111", "left", true),
            profile("PEK", "F", "FFFF", "factory", false)));
        Map<String, String> keys = keys("1111", "2222");
        keys.put("F", "FFFF");
        Map<String, Boolean> active = active(true, true);
        active.put("F", false);
        UserGroup updated = UserGroupEdit.prepare(original, "PEK", original.aliasSummary(), false, keys, active);
        assertEquals(3, updated.getProfiles().size());
        assertEquals("PEK_F", updated.get("F").name);
        assertEquals("FFFF", updated.get("F").passKey);
        assertEquals("factory", updated.get("F").nickname);
        assertFalse(updated.get("F").isActive);
    }

    @Test
    public void explicitAliasEditAppliesToBothEarsWithoutChangingEitherKey()
    {
        UserGroup original = new UserGroup("PEK", Arrays.asList(
            profile("PEK", "L", "1111", "left", true),
            profile("PEK", "R", "2222", "right", true)));
        UserGroup updated = UserGroupEdit.prepare(original, "PEK", "shared", true,
            keys("1111", "2222"), active(true, true));
        assertEquals("shared", updated.get("L").nickname);
        assertEquals("shared", updated.get("R").nickname);
        assertEquals("1111", updated.get("L").passKey);
        assertEquals("2222", updated.get("R").passKey);
    }

    @Test
    public void fourKoreanCharactersAreRejectedBeforeAnyProfileCanBeSaved()
    {
        UserGroup original = new UserGroup("PEK", Arrays.asList(
            profile("PEK", "L", "1111", "name", true),
            profile("PEK", "R", "2222", "name", true)));
        try
        {
            UserGroupEdit.prepare(original, "PEK", "changed", true,
                keys("4444", "가나다라"), active(false, true));
            fail("Four Unicode characters are not four protocol bytes");
        }
        catch (UserGroupEdit.KeyException expected)
        {
            assertEquals("R", expected.ear);
            assertTrue(expected.getMessage().contains("영문·숫자·기호 4자"));
            assertEquals("1111", original.get("L").passKey);
            assertEquals("2222", original.get("R").passKey);
        }
    }

    @Test
    public void printableAsciiKeysMayMixLettersDigitsAndSymbols()
    {
        UserGroup updated = UserGroupEdit.prepare(null, "자유 이니셜", "자유 별칭", true,
            keys("Ab!9", "~Z0?"), active(true, true));
        assertEquals("Ab!9", updated.get("L").passKey);
        assertEquals("~Z0?", updated.get("R").passKey);
        assertEquals("자유 이니셜", updated.initials);
        assertEquals("자유 별칭", updated.get("L").nickname);
    }

    @Test
    public void spacesControlCharactersAndDeleteAreNotWireKeys()
    {
        for (String invalid : Arrays.asList("ab c", "abc\u007f", "ab\tc", "ab\u0000c"))
        {
            try
            {
                UserGroupEdit.prepare(null, "PEK", "", true, keys(invalid, ""), active(true, false));
                fail("Non-printable key accepted");
            }
            catch (UserGroupEdit.KeyException expected)
            {
                assertEquals("L", expected.ear);
            }
        }
    }

    @Test
    public void clearingOneKeyRemovesOnlyThatEarAndPreservesTheOtherEar()
    {
        UserGroup original = new UserGroup("PEK", Arrays.asList(
            profile("PEK", "L", "1111", "left alias", true),
            profile("PEK", "R", "2222", "right alias", false)));
        UserGroup updated = UserGroupEdit.prepare(original, "PEK", original.aliasSummary(), false,
            keys("1111", ""), active(true, true));
        assertNull(updated.get("R"));
        assertEquals(1, updated.getProfiles().size());
        assertEquals("1111", updated.get("L").passKey);
        assertTrue(updated.get("L").isActive);
        assertEquals("left alias", updated.get("L").nickname);
        assertEquals("2222", original.get("R").passKey);
        assertFalse(original.get("R").isActive);
    }

    @Test
    public void clearingTheLastKeyRejectsSaveWithoutMutatingTheExistingGroup()
    {
        UserGroup original = new UserGroup("PEK", Arrays.asList(
            profile("PEK", "L", "1111", "name", true)));
        try
        {
            UserGroupEdit.prepare(original, "PEK", "changed", true,
                keys("", ""), active(false, false));
            fail("Empty groups must use the explicit delete action");
        }
        catch (IllegalArgumentException expected)
        {
            assertTrue(expected.getMessage().contains("하나 이상"));
            assertEquals(1, original.getProfiles().size());
            assertEquals("PEK_L", original.get("L").name);
            assertEquals("1111", original.get("L").passKey);
            assertTrue(original.get("L").isActive);
        }
    }

    @Test
    public void formAndPlannerShareTheSameWireKeyEligibility()
    {
        assertTrue(UserGroupEdit.validWireKey("A!~9"));
        assertFalse(UserGroupEdit.validWireKey("가나다라"));
        assertFalse(UserGroupEdit.validWireKey(""));
        assertFalse(UserGroupEdit.validWireKey(null));
    }

    @Test
    public void sameRegisteredInitialsAllowAliasCodeAndActivationUpdates()
    {
        UserGroup original = new UserGroup("PEK", Arrays.asList(
            profile("PEK", "L", "1111", "left", true),
            profile("PEK", "R", "2222", "right", false)));
        UserGroup updated = UserGroupEdit.prepare(original, "PEK", "new alias", true,
            keys("3333", "4444"), active(false, true));
        assertEquals(Arrays.asList("PEK_L", "PEK_R"), updated.getNames());
        assertEquals("3333", updated.get("L").passKey);
        assertEquals("4444", updated.get("R").passKey);
        assertEquals("new alias", updated.get("L").nickname);
        assertEquals("new alias", updated.get("R").nickname);
        assertFalse(updated.get("L").isActive);
        assertTrue(updated.get("R").isActive);
        assertEquals("1111", original.get("L").passKey);
        assertEquals("2222", original.get("R").passKey);
    }
}
