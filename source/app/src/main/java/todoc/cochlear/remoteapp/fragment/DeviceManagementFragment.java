package todoc.cochlear.remoteapp.fragment;

import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import java.util.Locale;
import todoc.cochlear.remoteapp.activity.MainActivity;
import todoc.cochlear.remoteapp.params.Status;

/** Device flash settings; independent of the app's registered-device list. */
public class DeviceManagementFragment extends Fragment {
    private EditText serial,key;
    private TextView serialResult,keyResult;
    private androidx.appcompat.widget.AppCompatButton serialChange,keyChange;
    @Nullable @Override public View onCreateView(@NonNull LayoutInflater inflater,@Nullable ViewGroup container,@Nullable Bundle saved) {
        MainActivity activity=(MainActivity)requireActivity();ManagementUi.toolbar(activity,"제조번호 · 페어링키");
        LinearLayout body=ManagementUi.body(requireContext());
        LinearLayout serialPanel=ManagementUi.panel(body);ManagementUi.heading(serialPanel,"외부기 제조번호");
        serial=input(serialPanel,"외부기 제조번호",4,false);
        String current=Status.instance().connectedDevice==null?"":Status.instance().connectedDevice.serialNumber;
        serial.setText(saved==null?current:saved.getString("serial",current));
        serialPanel.addView(ManagementUi.text(requireContext(),"영문 대문자·숫자 4자",12));
        serialResult=ManagementUi.text(requireContext(),"",12);serialResult.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);serialPanel.addView(serialResult);
        serialChange=ManagementUi.action(serialPanel,"제조번호 변경",v->{String value=serial.getText().toString().trim().toUpperCase(Locale.ROOT);if(!value.matches("[A-Z0-9]{4}")){serial.setError("영문 대문자·숫자 4자를 입력하세요.");return;}serial.setText(value);serialResult.setText("장치 응답 확인 중…");setPending(true);activity.changeExternalSerial(value);});
        LinearLayout keyPanel=ManagementUi.panel(body);ManagementUi.heading(keyPanel,"BLE 페어링키");
        key=input(keyPanel,"새 페어링키 6자리",6,true);
        keyPanel.addView(ManagementUi.text(requireContext(),"숫자 6자리 · 변경 후 새 키로 재연결합니다.",12));
        keyResult=ManagementUi.text(requireContext(),"",12);keyResult.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);keyPanel.addView(keyResult);
        keyChange=ManagementUi.action(keyPanel,"페어링키 변경",v->{String value=key.getText().toString();if(!value.matches("[0-9]{6}")){key.setError("숫자 6자리를 입력하세요.");return;}keyResult.setText("장치 응답 확인 중…");setPending(true);activity.changePairingKey(value);});
        return ManagementUi.scroll(body);
    }
    @Override public void onViewCreated(@NonNull View view,@Nullable Bundle saved){super.onViewCreated(view,saved);((MainActivity)requireActivity()).managementResult.observe(getViewLifecycleOwner(),result->{if(result==null){setPending(false);return;}if("serial".equals(result.operation)){setPending(false);serialResult.setText(result.message);if(result.success&&result.appliedValue!=null)serial.setText(result.appliedValue);}else if("key".equals(result.operation)){setPending(false);keyResult.setText(result.message);if(result.success)key.setText("");}});}
    private void setPending(boolean pending){if(serialChange!=null)serialChange.setEnabled(!pending);if(keyChange!=null)keyChange.setEnabled(!pending);}
    private EditText input(LinearLayout parent,String hint,int length,boolean password){EditText edit=new EditText(requireContext());edit.setHint(hint);edit.setSingleLine(true);edit.setTypeface(androidx.core.content.res.ResourcesCompat.getFont(requireContext(),todoc.cochlear.remoteapp.activity.R.font.ibm_plex_sans_kr));if(password)edit.setSaveEnabled(false);edit.setTextSize(14);edit.setTextColor(0xffe1ebec);edit.setHintTextColor(0xffaebec7);edit.setFilters(new InputFilter[]{new InputFilter.LengthFilter(length)});edit.setInputType(password?InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_VARIATION_PASSWORD:InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);edit.setMinHeight(ManagementUi.dp(requireContext(),48));edit.setContentDescription(hint);parent.addView(edit,new LinearLayout.LayoutParams(-1,-2));return edit;}
    @Override public void onSaveInstanceState(@NonNull Bundle state){super.onSaveInstanceState(state);if(serial!=null)state.putString("serial",serial.getText().toString());}
    @Override public void onDestroyView(){serial=null;key=null;serialResult=null;keyResult=null;serialChange=null;keyChange=null;super.onDestroyView();}
}
