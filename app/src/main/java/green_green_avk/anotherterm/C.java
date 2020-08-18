package green_green_avk.anotherterm;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

public final class C {
    private C() {
    }

    public static final String JAVA_PKG_NAME =
            BuildConfig.class.getName().replaceFirst("\\..*?$", "");
    public static final String APP_ID = BuildConfig.APPLICATION_ID;
    public static final String IFK_MSG_NEW = JAVA_PKG_NAME + ".MSG_NEW";
    public static final String IFK_MSG_NAME = JAVA_PKG_NAME + ".MSG_NAME";
    public static final String IFK_MSG_SESS_KEY = JAVA_PKG_NAME + ".MSG_SESS_KEY";
    public static final String IFK_MSG_SESS_TAIL = JAVA_PKG_NAME + ".MSG_SESS_TAIL";
    public static final String IFK_MSG_ID = JAVA_PKG_NAME + ".MSG_ID";
    public static final String IFK_MSG_INTENT = JAVA_PKG_NAME + ".MSG_INTENT";
    public static final String IFK_ACTION_NEW = JAVA_PKG_NAME + ".ACTION_NEW";
    public static final String IFK_ACTION_CANCEL = JAVA_PKG_NAME + ".ACTION_CANCEL";
    public static final String TERMSH_USER_TAG = "TERMSH_USER";
    public static final String REQUEST_USER_TAG = "REQUEST_USER";
    public static final String UNNAMED_FILE_NAME = "unnamed";
    public static final String UNDEFINED_FILE_SIZE = "null";
    public static final List<String> charsetList = new ArrayList<>(Charset.availableCharsets().keySet());
    public static final int KEYCODE_LED_WAKE_LOCK = 0x10000;
}
