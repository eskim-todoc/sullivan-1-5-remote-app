package todoc.cochlear.remoteapp.params;

import androidx.annotation.NonNull;

import java.util.Arrays;

public class ActionMessage
{
      public final static String PREFIX = "TODOC_2021_01_26_10_13_";

      public final static String ACTIVITY_FINISH = PREFIX + "ACTIVITY_FINISH";
      public final static String BLE_SCAN_START = PREFIX + "BLE_START_SCAN";
      public final static String BLE_SCAN_STOP = PREFIX + "BLE_STOP_SCAN";
      public final static String BLE_SCAN_RESTART = PREFIX + "BLE_RESTART_SCAN";
      public final static String BLE_CONNECT = PREFIX + "BLE_CONNECT";
      public final static String BLE_DISCONNECT = PREFIX + "BLE_DISCONNECT";
      public final static String NEW_FRAGMENT_HOME = PREFIX + "NEW_FRAGMENT_HOME";
      public final static String NEW_FRAGMENT_HOME_WITH_STOP_BLE_SCAN = PREFIX + "NEW_FRAGMENT_HOME_WITH_STOP_BLE_SCAN";
      public final static String NEW_FRAGMENT_SEARCH_WITH_DISCONNECT_BLE = PREFIX + "NEW_FRAGMENT_SEARCH_WITH_DISCONNECT_BLE";
      public final static String NEW_FRAGMENT_PASSWORD = PREFIX + "NEW_FRAGMENT_PASSWORD";
      public final static String DATABASE_CHECK_EMPTY = PREFIX + "DATABASE_CHECK_EMPTY";

      @NonNull
      @Override
      public String toString()
      {
            return "ActionMessage{" +
                        "PREFIX=" + PREFIX +
                        ", ACTIVITY_FINISH=" + ACTIVITY_FINISH +
                        ", BLE_SCAN_START=" + BLE_SCAN_START +
                        ", BLE_SCAN_STOP=" + BLE_SCAN_STOP +
                        ", BLE_SCAN_RESTART=" + BLE_SCAN_RESTART +
                        ", BLE_CONNECT=" + BLE_CONNECT +
                        ", BLE_DISCONNECT=" + BLE_DISCONNECT +
                        ", NEW_FRAGMENT_HOME=" + NEW_FRAGMENT_HOME +
                        ", NEW_FRAGMENT_HOME_WITH_STOP_BLE_SCAN=" + NEW_FRAGMENT_HOME_WITH_STOP_BLE_SCAN +
                        ", NEW_FRAGMENT_SEARCH_WITH_DISCONNECT_BLE=" + NEW_FRAGMENT_SEARCH_WITH_DISCONNECT_BLE +
                        ", NEW_FRAGMENT_PASSWORD=" + NEW_FRAGMENT_PASSWORD +
                        ", DATABASE_CHECK_EMPTY=" + DATABASE_CHECK_EMPTY +
                        '}';
      }
}
