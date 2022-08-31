package todoc.cochlear.remoteapp.fragment;

import android.graphics.drawable.Drawable;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import todoc.cochlear.remoteapp.activity.MainActivity;
import todoc.cochlear.remoteapp.activity.R;
import todoc.cochlear.remoteapp.activity.databinding.FragmentManualBinding;
import todoc.cochlear.remoteapp.list.ManualAdapter;

public class ManualFragment extends Fragment
{
    static public final String ARG_FIRST_SCREEN = "first_screen";

    private FragmentManualBinding mManualBinding;
    private boolean mTouchAvailable;

    public ManualFragment()
    {
        // Required empty public constructor
    }

    @Override
    public void onDestroyView()
    {
        super.onDestroyView();

        mTouchHandler.removeCallbacks(mTouchRunner);
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        todoc.cochlear.remoteapp.activity.databinding.ActivityMainBinding mainBinding = ((MainActivity) requireContext()).mBinding;
        mainBinding.toolbar.setNavigationIcon(AppCompatResources.getDrawable(requireContext(), R.drawable.toolbar_ic_back_arrow_24dp));
        Bundle bundle = getArguments();
        if (bundle != null && bundle.getBoolean(ManualFragment.ARG_FIRST_SCREEN))
        {
            mainBinding.toolbarNavigationMessage.setText("주화면");
        }
        else
        {
            mainBinding.toolbarNavigationMessage.setText("메뉴");
        }
        mainBinding.toolbarNavigationMessage.setVisibility(View.VISIBLE);
        //mainBinding.toolbar.setNavigationIcon(null);
        mainBinding.toolbar.getMenu().findItem(R.id.toolbar_settings).setVisible(false);
        mainBinding.toolbar.getMenu().findItem(R.id.toolbar_user).setVisible(false);
        mainBinding.toolbar.getMenu().findItem(R.id.toolbar_search).setVisible(false);
        mainBinding.toolbar.setTitle(requireContext().getString(R.string.toolbar_title_manual));
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        mManualBinding = FragmentManualBinding.inflate(inflater, container, false);
        View view = mManualBinding.getRoot();

        mTouchAvailable = true;

        printManual();
        initDoNotShowCheckBox();
        initCloseButton();

        return view;
    }

    private void printManual()
    {
        ManualAdapter manualAdapter = new ManualAdapter(this);
        mManualBinding.manualRecyclerview.setAdapter(manualAdapter);
        mManualBinding.manualRecyclerview.setLayoutManager(new LinearLayoutManager(requireContext()));

        manualAdapter.addItem(new Drawable[]{AppCompatResources.getDrawable(requireContext(), R.drawable.guide_1_title), AppCompatResources.getDrawable(requireContext(), R.drawable.guide_1_body)});
        manualAdapter.addItem(new Drawable[]{AppCompatResources.getDrawable(requireContext(), R.drawable.guide_2_title), AppCompatResources.getDrawable(requireContext(), R.drawable.guide_2_body)});
        manualAdapter.addItem(new Drawable[]{AppCompatResources.getDrawable(requireContext(), R.drawable.guide_3_title), AppCompatResources.getDrawable(requireContext(), R.drawable.guide_3_body)});
        manualAdapter.addItem(new Drawable[]{AppCompatResources.getDrawable(requireContext(), R.drawable.guide_4_title), AppCompatResources.getDrawable(requireContext(), R.drawable.guide_4_body)});
        manualAdapter.addItem(new Drawable[]{AppCompatResources.getDrawable(requireContext(), R.drawable.guide_5_title), AppCompatResources.getDrawable(requireContext(), R.drawable.guide_5_body)});
        manualAdapter.addItem(new Drawable[]{AppCompatResources.getDrawable(requireContext(), R.drawable.guide_6_title), AppCompatResources.getDrawable(requireContext(), R.drawable.guide_6_body)});
    }

    private final Handler mTouchHandler = new Handler();
    private final Runnable mTouchRunner = () ->
            mTouchAvailable = true;

    public void onTouchEvent()
    {
        if (mTouchAvailable)
        {
            mTouchAvailable = false;
            mTouchHandler.postDelayed(mTouchRunner, 250);

            // 장시간 미사용 핸들러 업데이트
            ((MainActivity) requireActivity()).longTimeIdleHandlerUpdate(true);
        }
    }

    private void initDoNotShowCheckBox()
    {
        mManualBinding.enableCheckbox.setChecked(!((MainActivity) requireContext()).mManualScreen.isEnabled());
        mManualBinding.enableCheckbox.setOnCheckedChangeListener((compoundButton, b) ->
        {
            // 장시간 미사용 핸들러 업데이트
            ((MainActivity) requireActivity()).longTimeIdleHandlerUpdate(true);

            ((MainActivity) requireContext()).mManualScreen.setEnable(!b);
        });
    }

    private void initCloseButton()
    {
        mManualBinding.manualCloseButton.setOnClickListener(view -> requireActivity().onBackPressed());
    }
}