package todoc.cochlear.remoteapp.fragment;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.nio.charset.StandardCharsets;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import todoc.cochlear.remoteapp.activity.MainActivity;
import todoc.cochlear.remoteapp.database.logs.EntityLog;
import todoc.cochlear.remoteapp.database.logs.UtilLog;
import todoc.cochlear.remoteapp.list.LogAdapter;

/** Searchable local log snapshot; editing and device commands are not sent here. */
public class LogFragment extends Fragment {
 private final ExecutorService io=Executors.newSingleThreadExecutor();
 private final List<EntityLog> logs=new ArrayList<>();
 private LogAdapter adapter;private EditText search;private TextView count;
 private String category="전체";
 private String pendingExportPath;
 private boolean exportBusy;
 private int viewGeneration;
 private androidx.appcompat.widget.AppCompatButton exportButton;
 private final ActivityResultLauncher<String> export=registerForActivityResult(
         new ActivityResultContracts.CreateDocument("text/plain"), this::completeExport);

 @Override public void onCreate(@Nullable Bundle state) {
  super.onCreate(state);
  if(state!=null) pendingExportPath=state.getString("pending-log-export");
  exportBusy=pendingExportPath!=null;
 }
 @Nullable @Override public View onCreateView(@NonNull LayoutInflater inflater,@Nullable ViewGroup container,@Nullable Bundle state){
  viewGeneration++;
  ManagementUi.toolbar((MainActivity)requireActivity(),"통신 로그");LinearLayout body=ManagementUi.body(requireContext());
  search=new EditText(requireContext());search.setSingleLine(true);search.setHint("기록에서 검색");search.setTextSize(14);search.setIncludeFontPadding(false);search.setTextColor(0xffe1ebec);search.setHintTextColor(0xffaebec7);search.setMinHeight(ManagementUi.dp(requireContext(),44));body.addView(search,new LinearLayout.LayoutParams(-1,ManagementUi.dp(requireContext(),44)));
  LinearLayout filters=new LinearLayout(requireContext());filters.setOrientation(LinearLayout.HORIZONTAL);body.addView(filters);
  for(String title:new String[]{"전체","제어","상태","연결","오류"}){androidx.appcompat.widget.AppCompatButton button=ManagementUi.action(filters,title,v->{category=title;applyFilter();});button.setTextSize(11);button.setPadding(0,0,0,0);button.setLayoutParams(new LinearLayout.LayoutParams(0,ManagementUi.dp(requireContext(),44),1));}
  count=ManagementUi.text(requireContext(),"로그 읽는 중…",11);body.addView(count);
  RecyclerView list=new RecyclerView(requireContext());list.setBackgroundResource(todoc.cochlear.remoteapp.activity.R.drawable.remote_glass_panel);list.setPadding(ManagementUi.dp(requireContext(),10),0,ManagementUi.dp(requireContext(),10),0);list.setClipToPadding(true);list.setClipToOutline(true);list.setLayoutManager(new LinearLayoutManager(requireContext()));adapter=new LogAdapter();list.setAdapter(adapter);body.addView(list,new LinearLayout.LayoutParams(-1,0,1));
  exportButton=ManagementUi.action(body,"내보내기",v->prepareExport());
  exportButton.setEnabled(!exportBusy);
  body.setBackgroundResource(todoc.cochlear.remoteapp.activity.R.drawable.remote_control_background_glass);
  search.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int start,int n,int after){}public void onTextChanged(CharSequence s,int start,int before,int n){applyFilter();}public void afterTextChanged(Editable e){}});
  final android.app.Activity host=requireActivity();io.execute(()->{List<EntityLog> stored=UtilLog.instance.readAllLogs();List<EntityLog> snapshot=new ArrayList<>();if(stored!=null)for(EntityLog entry:stored){if(entry.number==0)continue;EntityLog safe=new EntityLog();safe.number=entry.number;safe.date=entry.date;safe.message=redact(entry.message);snapshot.add(safe);}Collections.sort(snapshot,(a,b)->String.valueOf(b.date).compareTo(String.valueOf(a.date)));host.runOnUiThread(()->{if(adapter!=null){logs.clear();logs.addAll(snapshot);applyFilter();}});});return ManagementUi.navigation(body);
 }
 static String redact(String value){return value==null?"":value.replaceAll("(?i)(패스키|보안코드|pairingKey|passKey|페어링\\s*키)\\s*=\\s*[^,\\n]+","$1=••••");}
 private String classify(String message){String lower=message.toLowerCase(Locale.ROOT);if(lower.matches("(?s).*(실패|오류|error|timeout|시간초과).*$"))return "오류";if(lower.matches("(?s).*(연결|인증|connect|pair).*$"))return "연결";if(lower.matches("(?s).*(조회|배터리|상태|battery|status|pmic).*$"))return "상태";return "제어";}
 private List<EntityLog> filtered(){String query=search==null?"":search.getText().toString().toLowerCase(Locale.ROOT);List<EntityLog> result=new ArrayList<>();for(EntityLog item:logs)if(("전체".equals(category)||category.equals(classify(item.message)))&&(String.valueOf(item.date)+" "+item.message).toLowerCase(Locale.ROOT).contains(query))result.add(item);return result;}
 private void applyFilter(){if(adapter==null)return;List<EntityLog> result=filtered();adapter.replaceItems(result);if(count!=null)count.setText(category+" · "+result.size()+"개");}
 private String filteredText(){StringBuilder out=new StringBuilder();for(EntityLog entry:filtered())out.append(entry.date).append("\t").append(entry.message).append('\n');return out.toString();}

 /** Snapshot first; opening the document picker must never discard the selected log data. */
 private void prepareExport() {
  if(exportBusy) return;
  final String snapshot=filteredText();
  final int generation=viewGeneration;
  final android.app.Activity host=requireActivity();
  final File directory=new File(requireContext().getCacheDir(),"log-exports");
  exportBusy=true;
  exportButton.setEnabled(false);
  io.execute(()->{
   File snapshotFile=null;
   String failure=null;
   try {
    if(!directory.isDirectory()&&!directory.mkdirs()) throw new IOException("임시 저장소를 만들 수 없습니다.");
    snapshotFile=File.createTempFile("logs-",".txt",directory);
    try(OutputStream stream=new FileOutputStream(snapshotFile)) {
     stream.write(snapshot.getBytes(StandardCharsets.UTF_8));
    }
   } catch(IOException|SecurityException error) {
    failure=error.getMessage()==null?"임시 파일을 저장할 수 없습니다.":error.getMessage();
   }
   final File readyFile=snapshotFile;
   final String errorMessage=failure;
   host.runOnUiThread(()->{
    if(!isAdded()||getView()==null||generation!=viewGeneration) {
     if(readyFile!=null) readyFile.delete();
     finishExport(null);
     return;
    }
    if(errorMessage!=null) {
     if(readyFile!=null) readyFile.delete();
     finishExport("로그 준비 실패: "+errorMessage);
     return;
    }
    pendingExportPath=readyFile.getAbsolutePath();
    try {
     export.launch("TODOC-logs.txt");
    } catch(RuntimeException error) {
     readyFile.delete();
     pendingExportPath=null;
     finishExport("파일 선택기를 열 수 없습니다. 다시 시도하세요.");
    }
   });
  });
 }

 private void completeExport(@Nullable android.net.Uri destination) {
  final String snapshotPath=pendingExportPath;
  pendingExportPath=null;
  final android.app.Activity host=requireActivity();
  final android.content.ContentResolver resolver=requireContext().getContentResolver();
  final File directory=new File(requireContext().getCacheDir(),"log-exports");
  io.execute(()->{
   File snapshotFile=null;
   String message=null;
   try {
    if(snapshotPath!=null) {
     File candidate=new File(snapshotPath).getCanonicalFile();
     if(directory.getCanonicalFile().equals(candidate.getParentFile())) snapshotFile=candidate;
    }
    if(destination==null) return;
    if(snapshotFile==null||!snapshotFile.isFile()) throw new IOException("보관된 내보내기 데이터가 없습니다. 다시 내보내 주세요.");
    // Open the source before the destination so missing/evicted cache data cannot truncate a document.
    try(InputStream input=new FileInputStream(snapshotFile)) {
     try(OutputStream output=resolver.openOutputStream(destination)) {
      if(output==null) throw new IOException("저장할 파일을 열 수 없습니다.");
      byte[] buffer=new byte[8192];
      int length;
      while((length=input.read(buffer))!=-1) output.write(buffer,0,length);
     }
    }
    message="로그 저장 완료";
   } catch(IOException|SecurityException error) {
    message="로그 저장 실패: "+error.getMessage();
   } finally {
    if(snapshotFile!=null) snapshotFile.delete();
    final String resultMessage=message;
    host.runOnUiThread(()->finishExport(resultMessage));
   }
  });
 }

 private void finishExport(@Nullable String message) {
  exportBusy=false;
  if(exportButton!=null) exportButton.setEnabled(true);
  if(count!=null&&message!=null) count.setText(message);
 }

 @Override public void onSaveInstanceState(@NonNull Bundle state) {
  super.onSaveInstanceState(state);
  if(pendingExportPath!=null) state.putString("pending-log-export",pendingExportPath);
 }
 @Override public void onDestroyView(){viewGeneration++;exportButton=null;adapter=null;search=null;count=null;super.onDestroyView();}
 @Override public void onDestroy(){io.shutdown();super.onDestroy();}
}
