package todoc.cochlear.remoteapp.fragment;

import android.os.Bundle;

import androidx.appcompat.content.res.AppCompatResources;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import todoc.cochlear.remoteapp.activity.MainActivity;
import todoc.cochlear.remoteapp.activity.R;
import todoc.cochlear.remoteapp.activity.databinding.ActivityMainBinding;
import todoc.cochlear.remoteapp.activity.databinding.FragmentSettingsBinding;
import todoc.cochlear.remoteapp.activity.databinding.FragmentShareBinding;

public class ShareFragment extends Fragment
{
    static private final String TAG = "TODOC_" + ShareFragment.class.getSimpleName();

    private ActivityMainBinding mMainBinding;
    private FragmentShareBinding mShareBinding;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        mMainBinding = ((MainActivity) requireContext()).mBinding;
        mMainBinding.toolbar.setNavigationIcon(AppCompatResources.getDrawable(requireContext(), R.drawable.toolbar_ic_back_arrow_24dp));
        mMainBinding.toolbarNavigationMessage.setText("메뉴");
        mMainBinding.toolbarNavigationMessage.setVisibility(View.VISIBLE);
        mMainBinding.toolbar.getMenu().findItem(R.id.toolbar_settings).setVisible(false);
        mMainBinding.toolbar.getMenu().findItem(R.id.toolbar_user).setVisible(false);
        mMainBinding.toolbar.getMenu().findItem(R.id.toolbar_search).setVisible(false);
        //mMainBinding.toolbar.setTitleTextAppearance(requireContext(), R.style.TextAppearance_RemoteControl_Default_Headline6);
        mMainBinding.toolbar.setTitle("외부기 공유");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        mShareBinding = FragmentShareBinding.inflate(inflater, container, false);
        return mShareBinding.getRoot();
    }
}