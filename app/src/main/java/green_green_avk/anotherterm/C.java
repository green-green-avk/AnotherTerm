package green_green_avk.anotherterm;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

public final class C {
    private C() {
    }

    public static final String IFK_MSG_NEW = BuildConfig.NAMESPACE + ".MSG_NEW";
    public static final String IFK_MSG_NAME = BuildConfig.NAMESPACE + ".MSG_NAME";
    public static final String IFK_MSG_SESS_KEY = BuildConfig.NAMESPACE + ".MSG_SESS_KEY";
    public static final String IFK_MSG_ID = BuildConfig.NAMESPACE + ".MSG_ID";
    public static final String IFK_MSG_INTENT = BuildConfig.NAMESPACE + ".MSG_INTENT";
    public static final String IFK_ACTION_NEW = BuildConfig.NAMESPACE + ".ACTION_NEW";
    public static final String IFK_ACTION_CANCEL = BuildConfig.NAMESPACE + ".ACTION_CANCEL";
    public static final String TERMSH_UI_TAG = "TERMSH_UI";
    public static final String TERMSH_USER_TAG = "TERMSH_USER";
    public static final String REQUEST_USER_TAG = "REQUEST_USER";
    public static final String UNNAMED_FILE_NAME = "unnamed";
    public static final String UNDEFINED_FILE_SIZE = "null";
    public static final String UNDEFINED_FILE_MIME = "*/*";
    public static final List<String> charsetList =
            new ArrayList<>(Charset.availableCharsets().keySet());
    public static final int KEYCODE_LED_WAKE_LOCK = 0x10000;
    public static final String COND_STR_PROCESS_EXIT_STATUS_0 = "pes=0";
    public static final String LOG_TAG_SECURITY = "SECURITY";
}
