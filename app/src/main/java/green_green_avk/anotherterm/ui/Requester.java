package green_green_avk.anotherterm.ui;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import java.util.UUID;

public abstract class Requester {
    public static int generateRequestCode() {
        return (int) UUID.randomUUID().getLeastSignificantBits() & 0xFFFF;
    }

    protected static abstract class UiFragment extends Fragment {
        private static final String TAG = "UiFragment";

        @Override
        public void onCreate(@Nullable final Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setRetainInstance(true);
        }

        @UiThread
        protected void recycle() {
            getActivity().getSupportFragmentManager().beginTransaction().remove(this).commit();
        }
    }

    @UiThread
    @NonNull
    protected static <T extends UiFragment> T prepare(@NonNull final Context ctx,
                                                      @NonNull final T fragment) {
        final FragmentActivity a = (FragmentActivity) ctx;
        final FragmentManager m = a.getSupportFragmentManager();
        m.beginTransaction().add(fragment, UiFragment.TAG).commitNow();
        return fragment;
    }
}
