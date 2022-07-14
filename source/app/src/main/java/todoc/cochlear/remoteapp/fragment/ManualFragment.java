package todoc.cochlear.remoteapp.fragment;

import android.graphics.drawable.Drawable;
import android.os.Bundle;

import androidx.appcompat.content.res.AppCompatResources;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;

import todoc.cochlear.remoteapp.activity.MainActivity;
import todoc.cochlear.remoteapp.activity.R;
import todoc.cochlear.remoteapp.activity.databinding.ActivityMainBinding;
import todoc.cochlear.remoteapp.activity.databinding.FragmentManualBinding;
import todoc.cochlear.remoteapp.list.ManualAdapter;
import todoc.cochlear.remoteapp.shared_preferences.ManualScreen;

public class ManualFragment extends Fragment
{
    private ActivityMainBinding mMainBinding;
    private FragmentManualBinding mManualBinding;

    public ManualFragment()
    {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        mMainBinding = ((MainActivity) requireContext()).mBinding;
        mMainBinding.toolbar.setNavigationIcon(AppCompatResources.getDrawable(requireContext(), R.drawable.toolbar_ic_back_arrow_24dp));
        mMainBinding.toolbar.getMenu().findItem(R.id.settings).setVisible(false);
        mMainBinding.toolbar.getMenu().findItem(R.id.toolbar_user).setVisible(false);
        mMainBinding.toolbar.setTitleTextAppearance(requireContext(), R.style.TextAppearance_RemoteControl_Default_Headline6);
        mMainBinding.toolbar.setTitle(requireContext().getString(R.string.toolbar_title_manual));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        mManualBinding = FragmentManualBinding.inflate(inflater, container, false);
        View view = mManualBinding.getRoot();

        printManual();
        initDoNotShowCheckBox();
        initCloseButton();

        return view;
    }

    private void printManual()
    {
        ManualAdapter manualAdapter = new ManualAdapter();
        mManualBinding.manualRecyclerview.setAdapter(manualAdapter);
        mManualBinding.manualRecyclerview.setLayoutManager(new LinearLayoutManager(requireContext()));

        manualAdapter.addItem(new Drawable[]{AppCompatResources.getDrawable(requireContext(), R.drawable.guide_1_title), AppCompatResources.getDrawable(requireContext(), R.drawable.guide_1_body)});
        manualAdapter.addItem(new Drawable[]{AppCompatResources.getDrawable(requireContext(), R.drawable.guide_2_title), AppCompatResources.getDrawable(requireContext(), R.drawable.guide_2_body)});
        manualAdapter.addItem(new Drawable[]{AppCompatResources.getDrawable(requireContext(), R.drawable.guide_3_title), AppCompatResources.getDrawable(requireContext(), R.drawable.guide_3_body)});
        manualAdapter.addItem(new Drawable[]{AppCompatResources.getDrawable(requireContext(), R.drawable.guide_4_title), AppCompatResources.getDrawable(requireContext(), R.drawable.guide_4_body)});
        manualAdapter.addItem(new Drawable[]{AppCompatResources.getDrawable(requireContext(), R.drawable.guide_5_title), AppCompatResources.getDrawable(requireContext(), R.drawable.guide_5_body)});
        manualAdapter.addItem(new Drawable[]{AppCompatResources.getDrawable(requireContext(), R.drawable.guide_6_title), AppCompatResources.getDrawable(requireContext(), R.drawable.guide_6_body)});
    }

    private void initDoNotShowCheckBox()
    {
        mManualBinding.enableCheckbox.setChecked(!((MainActivity) requireContext()).mManualScreen.isEnabled());
        mManualBinding.enableCheckbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
        {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b)
            {
                ((MainActivity) requireContext()).mManualScreen.setEnable(!b);
            }
        });
    }

    private void initCloseButton()
    {
        mManualBinding.manualCloseButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                requireActivity().onBackPressed();
            }
        });
    }
}