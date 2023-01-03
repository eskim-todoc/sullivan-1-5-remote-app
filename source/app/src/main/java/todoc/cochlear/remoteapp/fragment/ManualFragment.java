package todoc.cochlear.remoteapp.fragment;

import android.graphics.drawable.Drawable;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.viewpager2.widget.ViewPager2;

import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import java.util.Objects;

import todoc.cochlear.remoteapp.activity.MainActivity;
import todoc.cochlear.remoteapp.activity.R;
import todoc.cochlear.remoteapp.activity.databinding.FragmentManualBinding;
import todoc.cochlear.remoteapp.list.ManualAdapter;
import todoc.cochlear.remoteapp.list.ManualViewPager2Adapter;

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

        //printViewPager2Manual();
        printManual();
        initDoNotShowCheckBox();
        initCloseButton();

        return view;
    }

    private void printViewPager2Manual()
    {
        ManualViewPager2Adapter manualViewPager2Adapter = new ManualViewPager2Adapter(this);
        mManualBinding.manualViewpager2.setAdapter(manualViewPager2Adapter);
        manualViewPager2Adapter.addItem(Objects.requireNonNull(AppCompatResources.getDrawable(requireContext(), R.drawable.guide_1_body)));
        manualViewPager2Adapter.addItem(Objects.requireNonNull(AppCompatResources.getDrawable(requireContext(), R.drawable.guide_2_body)));
        manualViewPager2Adapter.addItem(Objects.requireNonNull(AppCompatResources.getDrawable(requireContext(), R.drawable.guide_3_body)));

        mManualBinding.manualViewpager2.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback()
        {
            @Override
            public void onPageSelected(int position)
            {
                super.onPageSelected(position);
                setViewPager2Indicator(position);
            }
        });

        setupViewPager2Indicators(manualViewPager2Adapter.getItemCount());
    }

    private void setupViewPager2Indicators(int count)
    {
        ImageView[] views = new ImageView[count];
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(16, 8, 16, 8);

        for (int i = 0; i < views.length; i++)
        {
            views[i] = new ImageView(requireContext());
            views[i].setImageDrawable(AppCompatResources.getDrawable(requireContext(), R.drawable.manual_screen_ic_circle_normal_12dp));
            views[i].setLayoutParams(params);
            mManualBinding.manualViewpager2Indicator.addView(views[i]);
        }

        setViewPager2Indicator(0);
    }

    private void setViewPager2Indicator(int position)
    {
        int childCount = mManualBinding.manualViewpager2Indicator.getChildCount();

        for (int i = 0; i < childCount; i++)
        {
            ImageView iv = (ImageView) mManualBinding.manualViewpager2Indicator.getChildAt(i);

            if (i == position)
            {
                iv.setImageDrawable(AppCompatResources.getDrawable(requireContext(), R.drawable.manual_screen_ic_circle_color_12dp));
            }
            else
            {
                iv.setImageDrawable(AppCompatResources.getDrawable(requireContext(), R.drawable.manual_screen_ic_circle_normal_12dp));
            }
        }
    }

    private void printManual()
    {
        ManualAdapter manualAdapter = new ManualAdapter(this);
        mManualBinding.manualRecyclerview.setAdapter(manualAdapter);
        mManualBinding.manualRecyclerview.setLayoutManager(new LinearLayoutManager(requireContext()));

        /*
        manualAdapter.addItem(new Drawable[]{AppCompatResources.getDrawable(requireContext(), R.drawable.guide_1_title), AppCompatResources.getDrawable(requireContext(), R.drawable.guide_1_body)});
        manualAdapter.addItem(new Drawable[]{AppCompatResources.getDrawable(requireContext(), R.drawable.guide_2_title), AppCompatResources.getDrawable(requireContext(), R.drawable.guide_2_body)});
        manualAdapter.addItem(new Drawable[]{AppCompatResources.getDrawable(requireContext(), R.drawable.guide_3_title), AppCompatResources.getDrawable(requireContext(), R.drawable.guide_3_body)});
        manualAdapter.addItem(new Drawable[]{AppCompatResources.getDrawable(requireContext(), R.drawable.guide_4_title), AppCompatResources.getDrawable(requireContext(), R.drawable.guide_4_body)});
        manualAdapter.addItem(new Drawable[]{AppCompatResources.getDrawable(requireContext(), R.drawable.guide_5_title), AppCompatResources.getDrawable(requireContext(), R.drawable.guide_5_body)});
        manualAdapter.addItem(new Drawable[]{AppCompatResources.getDrawable(requireContext(), R.drawable.guide_6_title), AppCompatResources.getDrawable(requireContext(), R.drawable.guide_6_body)});
        */

        manualAdapter.addItem(R.drawable.item_manual_image_01);
        manualAdapter.addItem(R.drawable.item_manual_image_02);
        manualAdapter.addItem(R.drawable.item_manual_image_03);
        manualAdapter.addItem(R.drawable.item_manual_image_04);
        manualAdapter.addItem(R.drawable.item_manual_image_05);
        manualAdapter.addItem(R.drawable.item_manual_image_06);
        manualAdapter.addItem(R.drawable.item_manual_image_07);
        manualAdapter.addItem(R.drawable.item_manual_image_08);
        manualAdapter.addItem(R.drawable.item_manual_image_09);
        manualAdapter.addItem(R.drawable.item_manual_image_10);
        manualAdapter.addItem(R.drawable.item_manual_image_11);
        manualAdapter.addItem(R.drawable.item_manual_image_12);
        manualAdapter.addItem(R.drawable.item_manual_image_13);
        manualAdapter.addItem(R.drawable.item_manual_image_14);
        manualAdapter.addItem(R.drawable.item_manual_image_15);
        manualAdapter.addItem(R.drawable.item_manual_image_16);
        manualAdapter.addItem(R.drawable.item_manual_image_17);
        manualAdapter.addItem(R.drawable.item_manual_image_18);
        manualAdapter.addItem(R.drawable.item_manual_image_19);
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


            // TD2-SW-RC-UNIT-Test-ID-58 [사용설명서 이미지 표시 유닛] 순서[1 & 2] 시작.
            /*
            {
                // onCreate에서 생성한 mManualScreen을 사용해서 테스트한다.
                Log.d("TODOC_ManualFragment", "Manual screen enabled = " + ((MainActivity) requireContext()).mManualScreen.isEnabled());
            }
            */
            // TD2-SW-RC-UNIT-Test-ID-58 [사용설명서 이미지 표시 유닛] 순서[1 & 2] 끝.


        });
    }

    private void initCloseButton()
    {
        mManualBinding.manualCloseButton.setOnClickListener(view -> requireActivity().onBackPressed());
    }
}