package todoc.cochlear.remoteapp.fragment;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.ImageView;
import android.content.res.ColorStateList;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.text.TextUtils;
import android.view.Gravity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.content.res.ResourcesCompat;
import com.google.android.material.tabs.TabLayout;
import todoc.cochlear.remoteapp.activity.MainActivity;
import todoc.cochlear.remoteapp.activity.R;

final class ManagementUi {
    private ManagementUi() {}
    static int dp(Context context,int value){return Math.round(value*context.getResources().getDisplayMetrics().density);}
    static void toolbar(MainActivity activity,String title){
        activity.mBinding.toolbar.setNavigationIcon(AppCompatResources.getDrawable(activity,R.drawable.toolbar_ic_back_arrow_24dp));
        activity.mBinding.toolbarNavigationMessage.setVisibility(View.GONE);
        activity.mBinding.toolbar.setTitle(title);
        activity.mBinding.toolbar.getMenu().findItem(R.id.toolbar_settings).setVisible(false);
        activity.mBinding.toolbar.getMenu().findItem(R.id.toolbar_user).setVisible(false);
        activity.mBinding.toolbar.getMenu().findItem(R.id.toolbar_search).setVisible(false);
    }
    static LinearLayout body(Context context){
        LinearLayout body=new LinearLayout(context);body.setOrientation(LinearLayout.VERTICAL);body.setClipChildren(false);
        body.setPadding(dp(context,12),dp(context,8),dp(context,12),dp(context,20));return body;
    }
    static View scroll(LinearLayout body){
        ScrollView scroll=new ScrollView(body.getContext());scroll.setFillViewport(true);scroll.setBackgroundResource(R.drawable.remote_control_background_glass);scroll.setClipToPadding(false);scroll.addView(body);return navigation(scroll);
    }
    static View navigation(View content){
        Context context=content.getContext();LinearLayout shell=new LinearLayout(context);shell.setOrientation(LinearLayout.VERTICAL);shell.setBackgroundResource(R.drawable.remote_control_background_glass);
        shell.addView(content,new LinearLayout.LayoutParams(-1,0,1));
        TabLayout tabs=new TabLayout(context);tabs.setTabMode(TabLayout.MODE_FIXED);tabs.setTabGravity(TabLayout.GRAVITY_FILL);tabs.setBackgroundResource(R.drawable.remote_navigation_surface);tabs.setSelectedTabIndicatorColor(0xff98c8bf);tabs.setSelectedTabIndicatorHeight(dp(context,2));tabs.setSelectedTabIndicatorGravity(TabLayout.INDICATOR_GRAVITY_TOP);tabs.setTabRippleColor(ColorStateList.valueOf(0x1898c8bf));
        int[] icons={R.drawable.remote_nav_basic,R.drawable.remote_nav_special,R.drawable.remote_nav_ff,R.drawable.remote_nav_ota,R.drawable.remote_nav_more};String[] names={"기본 제어","특수 제어","FF","OTA","더보기"};
        for(int i=0;i<names.length;i++){TabLayout.Tab tab=tabs.newTab().setText(names[i]).setIcon(icons[i]).setContentDescription(names[i]);View item=android.view.LayoutInflater.from(context).inflate(R.layout.remote_navigation_item,tabs,false);TextView text=item.findViewById(android.R.id.text1);text.setText(names[i]);text.setTextSize(10);text.setIncludeFontPadding(false);text.setTypeface(ResourcesCompat.getFont(context,R.font.ibm_plex_sans_kr));ImageView image=item.findViewById(android.R.id.icon);image.setImageResource(icons[i]);image.setImageTintList(AppCompatResources.getColorStateList(context,R.color.remote_navigation_tint));tab.setCustomView(item);tabs.addTab(tab,i==4);}
        tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener(){public void onTabSelected(TabLayout.Tab tab){if(tab.getPosition()<4)((MainActivity)context).navigateRemotePage(tab.getPosition());else ((MainActivity)context).replaceFragment(todoc.cochlear.remoteapp.params.Status.TypeOfFragment.MENU);}public void onTabUnselected(TabLayout.Tab tab){}public void onTabReselected(TabLayout.Tab tab){if(tab.getPosition()==4)((MainActivity)context).replaceFragment(todoc.cochlear.remoteapp.params.Status.TypeOfFragment.MENU);}});
        shell.addView(tabs,new LinearLayout.LayoutParams(-1,dp(context,58)));return shell;
    }
    static TextView text(Context context,String value,int size){
        TextView text=new TextView(context);text.setText(value);text.setTextColor(Color.rgb(225,235,236));text.setTextSize(size);
        text.setTypeface(ResourcesCompat.getFont(context,R.font.ibm_plex_sans_kr));text.setIncludeFontPadding(false);text.setLineSpacing(0,1f);return text;
    }
    static void heading(LinearLayout parent,String value){
        TextView text=text(parent.getContext(),value,13);LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(-1,-2);
        params.setMargins(0,dp(parent.getContext(),8),0,dp(parent.getContext(),8));parent.addView(text,params);
    }
    static LinearLayout panel(LinearLayout parent){
        Context context=parent.getContext();LinearLayout panel=new LinearLayout(context);panel.setOrientation(LinearLayout.VERTICAL);panel.setBackgroundResource(R.drawable.remote_glass_panel);panel.setElevation(dp(context,2));panel.setPadding(dp(context,12),dp(context,10),dp(context,12),dp(context,10));
        LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(-1,-2);params.bottomMargin=dp(context,10);parent.addView(panel,params);return panel;
    }
    static AppCompatButton action(LinearLayout parent,String label,View.OnClickListener listener){
        Context context=parent.getContext();AppCompatButton button=new AppCompatButton(context);button.setAllCaps(false);button.setText(label);button.setTextSize(13);button.setIncludeFontPadding(false);button.setTextColor(Color.rgb(225,235,236));button.setTypeface(ResourcesCompat.getFont(context,R.font.ibm_plex_sans_kr));button.setBackgroundResource(R.drawable.app_glass_action);button.setSupportBackgroundTintList(null);button.setMinHeight(dp(context,44));button.setMinimumHeight(dp(context,44));button.setMinWidth(0);button.setPadding(dp(context,12),0,dp(context,12),0);
        LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(-1,dp(context,44));params.setMargins(0,dp(context,4),0,dp(context,4));parent.addView(button,params);button.setOnClickListener(listener);return button;
    }

    static LinearLayout menuPanel(LinearLayout body) {
        LinearLayout panel = panel(body);
        panel.setPadding(dp(body.getContext(), 10), 0, dp(body.getContext(), 10), 0);
        return panel;
    }

    static void menuRow(LinearLayout parent, int icon, String title, String note,
                        View.OnClickListener listener) {
        Context context = parent.getContext();
        if (parent.getChildCount() > 0) {
            View divider = new View(context);
            divider.setBackgroundColor(0x279fa8a6);
            parent.addView(divider, new LinearLayout.LayoutParams(-1, dp(context, 1)));
        }
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(context, 2), 0, dp(context, 2), 0);
        row.setClickable(true);
        row.setFocusable(true);
        row.setContentDescription(title + ", " + note);
        GradientDrawable mask = new GradientDrawable();
        mask.setColor(Color.WHITE);
        mask.setCornerRadius(dp(context, 8));
        row.setBackground(new RippleDrawable(ColorStateList.valueOf(0x2298c8bf), null, mask));
        row.setOnClickListener(listener);
        ImageView image = new ImageView(context);
        image.setImageResource(icon);
        image.setImageTintList(ColorStateList.valueOf(0xffc1d2cf));
        image.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        row.addView(image, new LinearLayout.LayoutParams(dp(context, 22), dp(context, 22)));
        LinearLayout copy = new LinearLayout(context);
        copy.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams copyParams = new LinearLayout.LayoutParams(0, -2, 1);
        copyParams.leftMargin = dp(context, 10);
        row.addView(copy, copyParams);
        TextView titleText = text(context, title, 13);
        titleText.setSingleLine(true);
        titleText.setEllipsize(TextUtils.TruncateAt.END);
        titleText.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        copy.addView(titleText);
        TextView noteText = text(context, note, 11);
        noteText.setSingleLine(true);
        noteText.setEllipsize(TextUtils.TruncateAt.END);
        noteText.setTextColor(0xffaebec7);
        noteText.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        LinearLayout.LayoutParams noteParams = new LinearLayout.LayoutParams(-1, -2);
        noteParams.topMargin = dp(context, 3);
        copy.addView(noteText, noteParams);
        TextView arrow = text(context, "›", 20);
        arrow.setGravity(Gravity.CENTER);
        arrow.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        row.addView(arrow, new LinearLayout.LayoutParams(dp(context, 16), -2));
        parent.addView(row, new LinearLayout.LayoutParams(-1, dp(context, 56)));
    }
}
