package todoc.cochlear.remoteapp.connection;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import todoc.cochlear.remoteapp.database.users.EntityUser;

import static org.junit.Assert.*;

public class SessionUserProfileTest
{
    private static EntityUser user(String initials, String ear, String alias, String key, boolean active)
    {
        EntityUser profile = new EntityUser();
        profile.name = initials + "_" + ear;
        profile.ear = ear;
        profile.nickname = alias;
        profile.passKey = key;
        profile.isActive = active;
        return profile;
    }

    @Test
    public void groupRenameMatchesBothEarsByOldMembershipWithoutTouchingAnotherUser()
    {
        List<String> oldNames = Arrays.asList("OLD_L", "OLD_R");
        List<EntityUser> changed = Arrays.asList(user("NEW", "R", "alias", "right-new", true),
            user("NEW", "L", "alias", "left-new", true));
        EntityUser left = SessionUserProfile.replacementFor(user("OLD", "L", "", "left-old", true), oldNames, changed);
        EntityUser right = SessionUserProfile.replacementFor(user("OLD", "R", "", "right-old", true), oldNames, changed);
        assertEquals("NEW_L", left.name);
        assertEquals("left-new", left.passKey);
        assertEquals("NEW_R", right.name);
        assertEquals("right-new", right.passKey);
        assertNull(SessionUserProfile.replacementFor(user("OTHER", "L", "", "other", true), oldNames, changed));
    }

    @Test
    public void currentAuthenticationKeepsItsSnapshotWhileTheNextConnectionUsesEditedRegistration()
    {
        EntityUser original = user("OLD", "L", "before", "old-auth", true);
        SessionUserProfile current = new SessionUserProfile(original);
        original.passKey = "caller-change";
        EntityUser edited = user("NEW", "L", "after", "future-auth", true);
        current.updateRegistration(edited);
        edited.passKey = "later-caller-change";
        assertEquals("old-auth", current.authenticationPassKey());
        assertEquals("NEW_L", current.registrationSnapshot().name);
        assertEquals("after", current.registrationSnapshot().nickname);
        assertEquals("future-auth", current.registrationSnapshot().passKey);
        SessionUserProfile reconnect = new SessionUserProfile(current.registrationSnapshot());
        assertEquals("future-auth", reconnect.authenticationPassKey());
        EntityUser obtained = current.registrationSnapshot();
        obtained.name = "mutated-read";
        assertEquals("NEW_L", current.registrationSnapshot().name);
    }

    @Test
    public void exclusiveWorkBlocksDeactivationButAllowsMetadataAndKeyChanges()
    {
        SessionUserProfile profile = new SessionUserProfile(user("OLD", "L", "", "auth", true));
        assertTrue(profile.canUpdate(user("NEW", "L", "changed", "future", true), true));
        assertFalse(profile.canUpdate(user("NEW", "L", "changed", "future", false), true));
        assertTrue(profile.canUpdate(user("NEW", "L", "changed", "future", false), false));
        assertFalse(profile.canUpdate(user("NEW", "R", "changed", "future", true), false));
        assertTrue(profile.canUpdate(null, false));
        assertFalse(profile.canUpdate(null, true));
    }

    @Test
    public void removingOneEarOnlyBlocksItsExclusiveWorkAndPreservesTheOtherEarRename()
    {
        EntityUser originalLeft = user("OLD", "L", "", "left-auth", true);
        EntityUser originalRight = user("OLD", "R", "", "right-auth", true);
        SessionUserProfile left = new SessionUserProfile(originalLeft);
        SessionUserProfile right = new SessionUserProfile(originalRight);
        List<String> oldNames = Arrays.asList("OLD_L", "OLD_R");
        List<EntityUser> updated = Arrays.asList(user("NEW", "L", "new alias", "future-auth", true));
        EntityUser keptEar = SessionUserProfile.replacementFor(originalLeft, oldNames, updated);
        EntityUser removedEar = SessionUserProfile.replacementFor(originalRight, oldNames, updated);
        assertNotNull(keptEar);
        assertNull(removedEar);
        assertTrue(SessionUserProfile.belongsToGroup(originalRight, oldNames));
        assertTrue(left.canUpdate(keptEar, true));
        assertFalse(right.canUpdate(removedEar, true));
        assertTrue(right.canUpdate(removedEar, false));
        right.updateRegistration(removedEar);
        left.updateRegistration(keptEar);
        assertNull(right.registrationSnapshot());
        assertEquals("right-auth", right.authenticationPassKey());
        assertEquals("NEW_L", left.registrationSnapshot().name);
        assertEquals("left-auth", left.authenticationPassKey());
    }

    @Test
    public void anUnrelatedProfileIsNotTreatedAsARemovedEarOfTheEditedGroup()
    {
        EntityUser unrelated = user("OTHER", "R", "", "other-auth", true);
        List<String> oldNames = Arrays.asList("OLD_L", "OLD_R");
        List<EntityUser> updated = Arrays.asList(user("NEW", "L", "", "future-auth", true));
        assertFalse(SessionUserProfile.belongsToGroup(unrelated, oldNames));
        assertNull(SessionUserProfile.replacementFor(unrelated, oldNames, updated));
    }
}
