package green_green_avk.anotherterm.ui;

// TODO: Split into UI and UI thread connector queue classes
public final class BackendUiSessionDialogs extends BackendUiDialogs
        implements BackendUiSessionBridge {

    private final int sessionKey;

    @Override
    public int getSessionKey() {
        return sessionKey;
    }

    public BackendUiSessionDialogs(final int sessionKey) {
        this.sessionKey = sessionKey;
    }
}
