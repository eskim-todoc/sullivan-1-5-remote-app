package todoc.cochlear.remoteapp.list;

import android.content.Context;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import todoc.cochlear.remoteapp.activity.R;
import todoc.cochlear.remoteapp.database.users.EntityUser;
import todoc.cochlear.remoteapp.database.users.UserGroup;
import todoc.cochlear.remoteapp.database.users.UserGroupEdit;
import todoc.cochlear.remoteapp.database.users.UtilUser;

public class UserAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>
{
    public interface Host
    {
        Context requireContext();

        String saveGroup(UserGroup original, UserGroup updated);

        void deleteGroup(UserGroup group, Runnable onDeleted);

        void listChanged();
    }

    private static final int TYPE_USER = 0;
    private static final int TYPE_ADD = 1;
    private final ArrayList<UserGroup> items = new ArrayList<>();
    private final Host host;
    private InlineListUi.DialogForm activeForm;

    public UserAdapter(Host host)
    {
        this.host = host;
    }

    @Override
    public int getItemViewType(int position)
    {
        return position == items.size() ? TYPE_ADD : TYPE_USER;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
    {
        if (viewType == TYPE_ADD)
        {
            return new RecyclerView.ViewHolder(InlineListUi.footer(parent, "새 사용자 등록", clicked -> openDialog(null, null)))
            {
            };
        }
        return new UserHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_user, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position)
    {
        if (holder instanceof UserHolder)
        {
            ((UserHolder) holder).bind(items.get(position));
        }
    }

    @Override
    public int getItemCount()
    {
        return items.size() + 1;
    }

    public int getRegisteredCount()
    {
        return items.size();
    }

    public void addItem(UserGroup item)
    {
        if (item != null)
        {
            items.add(item);
            notifyItemInserted(items.size() - 1);
        }
    }

    private void reload()
    {
        items.clear();
        items.addAll(UserGroup.fromUsers(UtilUser.instance.getUsers()));
        notifyDataSetChanged();
        host.listChanged();
    }

    public void openEditor(UserGroup group, String focusEar)
    {
        UserGroup latest = UtilUser.instance.getGroup(group.initials);
        if (latest != null)
        {
            openDialog(latest, focusEar);
        }
        else
        {
            reload();
        }
    }

    public void dismissEditor()
    {
        if (activeForm != null)
        {
            activeForm.dismiss();
            activeForm = null;
        }
    }

    private final class UserHolder extends RecyclerView.ViewHolder
    {
        private final TextView initials;
        private final TextView alias;
        private final TextView left;
        private final TextView right;
        private final TextView factory;
        private UserGroup group;

        UserHolder(View view)
        {
            super(view);
            initials = view.findViewById(R.id.name);
            alias = view.findViewById(R.id.user_alias);
            left = view.findViewById(R.id.user_left_active);
            right = view.findViewById(R.id.user_right_active);
            factory = view.findViewById(R.id.user_factory_active);
            View.OnClickListener edit = clicked ->
            {
                if (group != null)
                {
                    openEditor(group, null);
                }
            };
            view.setOnClickListener(edit);
            left.setOnClickListener(edit);
            right.setOnClickListener(edit);
            factory.setOnClickListener(edit);
        }

        void bind(UserGroup item)
        {
            group = item;
            initials.setText(item.initials);
            String nickname = item.aliasSummary();
            alias.setText(nickname.isEmpty() ? "—" : nickname);
            alias.setTextColor(nickname.isEmpty()
                ? itemView.getResources().getColor(R.color.glass_muted_text, itemView.getContext().getTheme())
                : itemView.getResources().getColor(R.color.white_90, itemView.getContext().getTheme()));
            bindBadge(left, EntityUser.EAR_LEFT);
            bindBadge(right, EntityUser.EAR_RIGHT);
            bindBadge(factory, EntityUser.EAR_FACTORY_RESET);
            itemView.setContentDescription(item.displayName() + ". 눌러 사용자 정보 수정");
        }

        private void bindBadge(TextView badge, String ear)
        {
            EntityUser profile = group.get(ear);
            badge.setVisibility(profile == null ? View.INVISIBLE : View.VISIBLE);
            badge.setActivated(profile != null && profile.isActive);
            badge.setTextColor(itemView.getResources().getColor(profile != null && profile.isActive
                ? R.color.glass_accent_text : R.color.glass_muted_text, itemView.getContext().getTheme()));
            badge.setContentDescription(UtilUser.getEarKorean(ear) + " 등록됨, 자동 연결 "
                + (profile != null && profile.isActive ? "사용. 눌러 사용자 정보 수정" : "사용 안 함. 눌러 사용자 정보 수정"));
        }
    }

    private void openDialog(UserGroup original, String focusEar)
    {
        dismissEditor();
        InlineListUi.DialogForm form = new InlineListUi.DialogForm(host.requireContext(),
            original == null ? "사용자 등록" : "사용자 수정", original != null);
        activeForm = form;
        AppCompatEditText initials = InlineListUi.field(form.editor, "이니셜", 0, false).input;
        InlineListUi.identify(initials, R.id.group_initials, "사용자 이니셜");
        initials.setText(original == null ? "" : original.initials);
        if (original != null)
        {
            initials.setKeyListener(null);
            initials.setFocusable(false);
            initials.setCursorVisible(false);
        }
        AppCompatEditText alias = InlineListUi.field(form.editor, "별칭", 0, false).input;
        InlineListUi.identify(alias, R.id.group_alias, "사용자 별칭");
        String originalAlias = original == null ? "" : original.aliasSummary();
        alias.setText(originalAlias);
        Map<String, InlineListUi.Field> ears = new LinkedHashMap<>();
        for (String ear : UserGroup.EARS)
        {
            String label = EntityUser.EAR_LEFT.equals(ear) ? "L 왼쪽"
                : EntityUser.EAR_RIGHT.equals(ear) ? "R 오른쪽" : "F 초기화";
            InlineListUi.Field field = InlineListUi.field(form.editor, label, 4, true);
            field.input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            field.input.setTypeface(alias.getTypeface());
            InlineListUi.identify(field.input, EntityUser.EAR_LEFT.equals(ear) ? R.id.group_left_key
                    : EntityUser.EAR_RIGHT.equals(ear) ? R.id.group_right_key : R.id.group_factory_key,
                label + " 내부기 코드 4자");
            field.active.setId(EntityUser.EAR_LEFT.equals(ear) ? R.id.group_left_active
                : EntityUser.EAR_RIGHT.equals(ear) ? R.id.group_right_active : R.id.group_factory_active);
            EntityUser profile = original == null ? null : original.get(ear);
            field.input.setText(profile == null ? "" : profile.passKey);
            field.active.setChecked(profile != null && profile.isActive);
            field.active.setEnabled(UserGroupEdit.validWireKey(InlineListUi.value(field.input)));
            field.input.addTextChangedListener(new TextWatcher()
            {
                @Override
                public void beforeTextChanged(CharSequence value, int start, int count, int after)
                {
                }

                @Override
                public void onTextChanged(CharSequence value, int start, int before, int count)
                {
                    field.active.setEnabled(UserGroupEdit.validWireKey(value.toString()));
                    if (value.length() == 0)
                    {
                        field.active.setChecked(false);
                    }
                }

                @Override
                public void afterTextChanged(Editable value)
                {
                }
            });
            ears.put(ear, field);
        }
        form.show("사용자", () ->
        {
            Map<String, String> keys = new LinkedHashMap<>();
            Map<String, Boolean> active = new LinkedHashMap<>();
            for (String ear : UserGroup.EARS)
            {
                keys.put(ear, InlineListUi.value(ears.get(ear).input));
                active.put(ear, ears.get(ear).active.isChecked());
            }
            UserGroup updated;
            try
            {
                String nickname = InlineListUi.value(alias);
                updated = UserGroupEdit.prepare(original, InlineListUi.value(initials), nickname,
                    original == null || !Objects.equals(nickname, originalAlias), keys, active);
            }
            catch (IllegalArgumentException invalid)
            {
                View target = invalid instanceof UserGroupEdit.KeyException
                    ? ears.get(((UserGroupEdit.KeyException) invalid).ear).input : original == null ? initials : alias;
                form.showError(invalid.getMessage(), target);
                return;
            }
            String failure = host.saveGroup(original, updated);
            if (failure != null)
            {
                form.showError(failure, original == null ? initials : alias);
                return;
            }
            form.dismiss();
            reload();
        }, () ->
        {
            if (original != null)
            {
                host.deleteGroup(original, () ->
                {
                    form.dismiss();
                    reload();
                });
            }
        }, () ->
        {
            if (activeForm == form)
            {
                activeForm = null;
            }
        });
        if (focusEar != null && ears.containsKey(focusEar))
        {
            InlineListUi.focus(ears.get(focusEar).input);
        }
    }
}
