package green_green_avk.anotherterm;

import android.app.Fragment;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

@RequiresApi(api = Build.VERSION_CODES.KITKAT)
public final class OwnDocumentsSettingsFragment extends Fragment {
    @Nullable
    @Override
    public View onCreateView(final LayoutInflater inflater, @Nullable final ViewGroup container,
                             final Bundle savedInstanceState) {
        final View wRoot = inflater.inflate(R.layout.own_documents_settings_fragment,
                container, false);
        final TextView wLocation = wRoot.findViewById(R.id.location);
        final CompoundButton wEnable = wRoot.findViewById(R.id.enable);
        wEnable.setChecked(OwnDocumentsManager.isEnabled(inflater.getContext()));
        wEnable.setOnCheckedChangeListener((buttonView, isChecked) ->
                OwnDocumentsManager.setEnabled(buttonView.getContext(), isChecked));
        wLocation.setText(OwnDocumentsManager.LOCATION_DESC);
        return wRoot;
    }
}
