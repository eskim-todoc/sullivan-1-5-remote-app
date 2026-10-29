package todoc.cochlear.remoteapp.fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import todoc.cochlear.remoteapp.activity.MainActivity;
import todoc.cochlear.remoteapp.activity.R;
import todoc.cochlear.remoteapp.params.Status;
/** The single entry point for registration, device maintenance and records. */
public class SettingsFragment extends Fragment {
 @Nullable @Override public View onCreateView(@NonNull LayoutInflater inflater,@Nullable ViewGroup container,@Nullable Bundle state){
 MainActivity activity=(MainActivity)requireActivity();ManagementUi.toolbar(activity,"더보기");LinearLayout body=ManagementUi.body(requireContext());
 ManagementUi.heading(body,"등록 관리");LinearLayout registration=ManagementUi.menuPanel(body);
 ManagementUi.menuRow(registration,R.drawable.management_ic_user,"사용자","이름 · 착용 위치 · 연결할 외부기",v->activity.replaceFragment(Status.TypeOfFragment.USER_LIST));
 ManagementUi.menuRow(registration,R.drawable.management_ic_device,"외부기","앱에 등록한 외부기 목록",v->activity.replaceFragment(Status.TypeOfFragment.DEVICE_LIST));
 ManagementUi.heading(body,"장치 관리");LinearLayout device=ManagementUi.menuPanel(body);
 ManagementUi.menuRow(device,R.drawable.toolbar_ic_settings_24dp,"제조번호 · 페어링키","현재 연결된 외부기의 정보 변경",v->open(new DeviceManagementFragment()));
 ManagementUi.menuRow(device,R.drawable.remote_nav_special,"맵 관리","시험 맵 · 기본 맵 · 공장 복원",v->open(new MapManagementFragment()));
 ManagementUi.heading(body,"기록");LinearLayout records=ManagementUi.menuPanel(body);
 ManagementUi.menuRow(records,R.drawable.management_ic_log,"통신 로그","연결 · 조회 · 설정 결과 확인",v->open(new LogFragment()));return ManagementUi.scroll(body);}
 private void open(Fragment fragment){requireActivity().getSupportFragmentManager().beginTransaction().replace(R.id.frame,fragment).commit();}
}