package green_green_avk.wayland.server;

import android.util.SparseArray;

import androidx.annotation.NonNull;

import java.util.HashSet;
import java.util.Set;

public final class WlDisplay {
    final Set<WlClient> clients = new HashSet<>();

    public final SparseArray<WlGlobal> globals = new SparseArray<>();

    private int getFreeGlobalId() {
        int i;
        for (i = 0; i < globals.size(); i++)
            if (globals.keyAt(i) != i + 1)
                break;
        return i + 1;
    }

    public int addGlobal(@NonNull final WlGlobal global) {
        final int newId = getFreeGlobalId();
        globals.put(newId, global);
        for (final WlClient client : clients)
            client.onGlobal(newId, global);
        return newId;
    }

    public void removeGlobal(final int name) {
        for (final WlClient client : clients)
            client.onGlobalRemove(name);
        globals.remove(name);
    }

    private int serial = 0;

    /* Deprecated for the Greater Good */
/*
    public int getSerial() {
        return serial;
    }
*/

    public int nextSerial() {
        return serial++;
    }
}
