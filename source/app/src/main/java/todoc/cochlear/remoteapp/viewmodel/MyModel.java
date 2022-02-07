package todoc.cochlear.remoteapp.viewmodel;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class MyModel extends ViewModel
{
    private MutableLiveData<Boolean> currentState;

    public MutableLiveData<Boolean> getCurrentState()
    {
        if (currentState == null)
        {
            currentState = new MutableLiveData<Boolean>();
        }

        return currentState;
    }

    public void setCurrentState(boolean state)
    {
        if (currentState == null)
        {
            currentState = new MutableLiveData<Boolean>();
        }

        currentState.setValue(Boolean.valueOf(state));
    }
}
