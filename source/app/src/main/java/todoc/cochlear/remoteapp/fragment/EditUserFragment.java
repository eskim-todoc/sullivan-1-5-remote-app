package todoc.cochlear.remoteapp.fragment;

public class EditUserFragment extends UserGroupForm
{
    public static final String ARG_INITIALS = "initials";
    public static final String ARG_FOCUS_EAR = "focusEar";
    public static final String ARG_NAME = "name";
    public static final String ARG_PASSKEY = "passkey";
    public static final String ARG_NICKNAME = "nickname";
    public static final String ARG_EAR = "ear";
    public static final String ARG_DEFAULT = "default";
    public static final String ARG_ACTIVE = "active";

    @Override
    protected boolean editing()
    {
        return true;
    }
}
