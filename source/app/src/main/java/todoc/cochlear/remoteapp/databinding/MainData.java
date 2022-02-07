package todoc.cochlear.remoteapp.databinding;

import androidx.databinding.ObservableField;

public class MainData {
    private static final ObservableField<String> buttonName = new ObservableField<>();

    public MainData() {
        buttonName.set("테스트 버튼");
    }

    public MainData(String buttonName) {
        this.buttonName.set(buttonName);
    }

    public String getButtonName() {
        return buttonName.get();
    }

    public void setButtonName(String buttonName) {
        this.buttonName.set(buttonName);
    }
}
