package todoc.cochlear.remoteapp.fragment;

import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.fragment.app.Fragment;

import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import todoc.cochlear.remoteapp.activity.MainActivity;
import todoc.cochlear.remoteapp.activity.R;
import todoc.cochlear.remoteapp.activity.databinding.ActivityMainBinding;
import todoc.cochlear.remoteapp.activity.databinding.FragmentUserGroupBinding;
import todoc.cochlear.remoteapp.database.logs.UtilLog;
import todoc.cochlear.remoteapp.database.users.EntityUser;
import todoc.cochlear.remoteapp.database.users.UserGroup;
import todoc.cochlear.remoteapp.database.users.UserGroupEdit;
import todoc.cochlear.remoteapp.database.users.UtilUser;
import todoc.cochlear.remoteapp.ui.GlassAlertDialogBuilder;

/**
 * One form edits a person's ear profiles atomically while keeping ear-specific BLE identities.
 */
public abstract class UserGroupForm extends Fragment
{
    private FragmentUserGroupBinding binding;
    private UserGroup original;
    private String originalAlias = "";
    private boolean missingOriginal;

    protected abstract boolean editing();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        if (!editing())
        {
            return;
        }
        Bundle arguments = getArguments();
        String initials = arguments == null ? null : arguments.getString(EditUserFragment.ARG_INITIALS);
        if (initials == null && arguments != null)
        {
            EntityUser legacy = UtilUser.instance.getUserByName(arguments.getString(EditUserFragment.ARG_NAME));
            if (legacy != null)
            {
                initials = UserGroup.initialsOf(legacy);
            }
        }
        original = initials == null ? null : UtilUser.instance.getGroup(initials);
        missingOriginal = original == null;
        originalAlias = original == null ? "" : original.aliasSummary();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        binding = FragmentUserGroupBinding.inflate(inflater, container, false);
        return ManagementUi.navigation(binding.getRoot());
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState)
    {
        super.onViewCreated(view, savedInstanceState);
        ActivityMainBinding main = ((MainActivity) requireActivity()).mBinding;
        main.toolbar.setNavigationIcon(AppCompatResources.getDrawable(requireContext(), R.drawable.toolbar_ic_back_arrow_24dp));
        main.toolbarNavigationMessage.setVisibility(View.GONE);
        main.toolbar.getMenu().findItem(R.id.toolbar_settings).setVisible(false);
        main.toolbar.getMenu().findItem(R.id.toolbar_user).setVisible(false);
        main.toolbar.getMenu().findItem(R.id.toolbar_search).setVisible(false);
        main.toolbar.setTitle(editing() ? R.string.toolbar_title_edit_user : R.string.toolbar_title_add_user);
        if (missingOriginal)
        {
            Toast.makeText(requireContext(), "등록 정보를 찾을 수 없습니다. 목록에서 다시 열어주세요.", Toast.LENGTH_SHORT).show();
            view.post(() ->
            {
                if (isAdded())
                {
                    requireActivity().onBackPressed();
                }
            });
            return;
        }
        binding.groupInitials.setText(original == null ? "" : original.initials);
        if (original != null)
        {
            binding.groupInitials.setKeyListener(null);
            binding.groupInitials.setFocusable(false);
            binding.groupInitials.setFocusableInTouchMode(false);
            binding.groupInitials.setCursorVisible(false);
            binding.groupInitialsLayout.setHelperText("이니셜은 등록 후 변경할 수 없습니다.");
        }
        binding.groupAlias.setText(originalAlias);
        configureEar(EntityUser.EAR_LEFT, binding.groupLeftKey, binding.groupLeftActive);
        configureEar(EntityUser.EAR_RIGHT, binding.groupRightKey, binding.groupRightActive);
        configureEar(EntityUser.EAR_FACTORY_RESET, binding.groupFactoryKey, binding.groupFactoryActive);
        boolean hasFactory = original != null && original.get(EntityUser.EAR_FACTORY_RESET) != null
            || savedInstanceState != null && savedInstanceState.getBoolean("factoryExpanded", false);
        binding.groupFactoryPanel.setVisibility(hasFactory ? View.VISIBLE : View.GONE);
        binding.groupAddFactory.setVisibility(hasFactory ? View.GONE : View.VISIBLE);
        binding.groupAddFactory.setOnClickListener(clicked ->
        {
            binding.groupFactoryPanel.setVisibility(View.VISIBLE);
            binding.groupAddFactory.setVisibility(View.GONE);
            binding.groupFactoryKey.requestFocus();
        });
        binding.groupDelete.setVisibility(editing() ? View.VISIBLE : View.GONE);
        binding.groupSave.setText(editing() ? "수정" : "등록");
        binding.groupSave.setOnClickListener(clicked -> save());
        binding.groupDelete.setOnClickListener(clicked -> confirmDelete());
        String focusEar = getArguments() == null ? null : getArguments().getString(EditUserFragment.ARG_FOCUS_EAR);
        if (EntityUser.EAR_LEFT.equals(focusEar))
        {
            binding.groupLeftKey.requestFocus();
        }
        else if (EntityUser.EAR_RIGHT.equals(focusEar))
        {
            binding.groupRightKey.requestFocus();
        }
        else if (EntityUser.EAR_FACTORY_RESET.equals(focusEar))
        {
            binding.groupFactoryPanel.setVisibility(View.VISIBLE);
            binding.groupAddFactory.setVisibility(View.GONE);
            binding.groupFactoryKey.requestFocus();
        }
    }

    private void configureEar(String ear, TextInputEditText key, MaterialCheckBox active)
    {
        EntityUser profile = original == null ? null : original.get(ear);
        key.setText(profile == null ? "" : profile.passKey);
        active.setChecked(profile != null && profile.isActive);
        active.setEnabled(UserGroupEdit.validWireKey(text(key)));
        key.addTextChangedListener(new TextWatcher()
        {
            @Override
            public void beforeTextChanged(CharSequence value, int start, int count, int after)
            {
            }

            @Override
            public void onTextChanged(CharSequence value, int start, int before, int count)
            {
                active.setEnabled(UserGroupEdit.validWireKey(value.toString()));
                if (value.length() == 0)
                {
                    active.setChecked(false);
                }
            }

            @Override
            public void afterTextChanged(Editable value)
            {
            }
        });
    }

    private void save()
    {
        clearInputErrors();
        Map<String, String> keys = new LinkedHashMap<>();
        keys.put(EntityUser.EAR_LEFT, text(binding.groupLeftKey));
        keys.put(EntityUser.EAR_RIGHT, text(binding.groupRightKey));
        keys.put(EntityUser.EAR_FACTORY_RESET, text(binding.groupFactoryKey));
        Map<String, Boolean> active = new LinkedHashMap<>();
        active.put(EntityUser.EAR_LEFT, binding.groupLeftActive.isChecked());
        active.put(EntityUser.EAR_RIGHT, binding.groupRightActive.isChecked());
        active.put(EntityUser.EAR_FACTORY_RESET, binding.groupFactoryActive.isChecked());
        UserGroup updated;
        try
        {
            String alias = text(binding.groupAlias);
            updated = UserGroupEdit.prepare(original, text(binding.groupInitials), alias,
                original == null || !Objects.equals(alias, originalAlias), keys, active);
        }
        catch (IllegalArgumentException error)
        {
            if (error instanceof UserGroupEdit.KeyException)
            {
                String ear = ((UserGroupEdit.KeyException) error).ear;
                TextInputEditText key = EntityUser.EAR_LEFT.equals(ear) ? binding.groupLeftKey
                    : EntityUser.EAR_RIGHT.equals(ear) ? binding.groupRightKey : binding.groupFactoryKey;
                com.google.android.material.textfield.TextInputLayout keyLayout = EntityUser.EAR_LEFT.equals(ear)
                    ? binding.groupLeftKeyLayout : EntityUser.EAR_RIGHT.equals(ear)
                                                   ? binding.groupRightKeyLayout : binding.groupFactoryKeyLayout;
                keyLayout.setError(error.getMessage());
                key.requestFocus();
            }
            Toast.makeText(requireContext(), error.getMessage(), Toast.LENGTH_SHORT).show();
            return;
        }
        UserGroup collision = UtilUser.instance.getGroup(updated.initials);
        if (collision != null && (original == null || !Objects.equals(original.initials, updated.initials)))
        {
            showNameCollision();
            return;
        }
        MainActivity activity = (MainActivity) requireActivity();
        List<String> oldNames = original == null ? Collections.emptyList() : original.getNames();
        if (!activity.canUpdateUserGroup(oldNames, updated.getProfiles()))
        {
            Toast.makeText(requireContext(), "진행 중인 작업이 끝난 뒤 연결 사용 여부를 바꾸세요.", Toast.LENGTH_SHORT).show();
            return;
        }
        UtilUser.GroupWriteResult result = UtilUser.instance.saveGroup(original, updated);
        if (result != UtilUser.GroupWriteResult.SAVED)
        {
            if (result == UtilUser.GroupWriteResult.NAME_IN_USE)
            {
                showNameCollision();
            }
            else
            {
                Toast.makeText(requireContext(), result == UtilUser.GroupWriteResult.STALE
                    ? "등록 정보가 변경되었습니다. 목록에서 다시 열어주세요." : "저장하지 못했습니다. 등록 정보는 그대로 유지됩니다.", Toast.LENGTH_LONG).show();
            }
            return;
        }
        UserGroup saved = UtilUser.instance.getGroup(updated.initials);
        UserGroup applied = saved == null ? updated : saved;
        activity.onUserGroupUpdated(oldNames, applied.getProfiles());
        for (EntityUser profile : applied.getProfiles())
        {
            EntityUser before = original == null ? null : original.get(profile.ear);
            if (profile.isActive && (before == null || !before.isActive))
            {
                // Activation takes effect on the next explicit + search.
                break;
            }
        }
        UtilLog.instance.writeLog((editing() ? "사용자 수정 : " : "사용자 등록 : ") + updated.initials);
        requireActivity().onBackPressed();
    }

    private void showNameCollision()
    {
        binding.groupInitialsLayout.setError("이미 등록된 사용자 이니셜입니다.");
        binding.groupInitials.requestFocus();
    }

    private boolean rejectDeleteInUse()
    {
        MainActivity activity = (MainActivity) requireActivity();
        for (String name : original.getNames())
        {
            if (activity.isUserInUse(name))
            {
                new GlassAlertDialogBuilder(requireContext()).setTitle("연결 중인 사용자")
                    .setMessage("이 사용자의 장치 연결을 모두 해제한 뒤 삭제하세요.")
                    .setPositiveButton("확인", null).show();
                return true;
            }
        }
        return false;
    }

    private void confirmDelete()
    {
        if (original == null || rejectDeleteInUse())
        {
            return;
        }
        new GlassAlertDialogBuilder(requireContext()).setTitle("사용자 삭제")
            .setMessage(original.displayName() + "에 등록된 모든 내부기와 키를 함께 삭제하시겠습니까?")
            .setNegativeButton("취소", null).setPositiveButton("삭제", (dialog, which) ->
            {
                if (rejectDeleteInUse())
                {
                    return;
                }
                if (!UtilUser.instance.deleteGroup(original))
                {
                    Toast.makeText(requireContext(), "삭제하지 못했습니다. 목록에서 등록 정보를 다시 확인하세요.", Toast.LENGTH_SHORT).show();
                    return;
                }
                UtilLog.instance.writeLog("사용자 삭제 : " + original.initials);
                requireActivity().onBackPressed();
            }).show();
    }

    public void clearInputErrors()
    {
        if (binding == null)
        {
            return;
        }
        binding.groupInitialsLayout.setError(null);
        binding.groupLeftKeyLayout.setError(null);
        binding.groupRightKeyLayout.setError(null);
        binding.groupFactoryKeyLayout.setError(null);
    }

    private static String text(TextInputEditText view)
    {
        return view.getText() == null ? "" : view.getText().toString();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState)
    {
        if (binding != null)
        {
            outState.putBoolean("factoryExpanded", binding.groupFactoryPanel.getVisibility() == View.VISIBLE);
        }
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onDestroyView()
    {
        if (binding != null)
        {
            InputMethodManager keyboard = (InputMethodManager) requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            keyboard.hideSoftInputFromWindow(binding.getRoot().getWindowToken(), 0);
        }
        binding = null;
        super.onDestroyView();
    }
}
