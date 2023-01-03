package green_green_avk.anotherterm;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Collection;
import java.util.Locale;
import java.util.Map;

import green_green_avk.anotherterm.backends.BackendException;
import green_green_avk.anotherterm.backends.BackendModule;
import green_green_avk.anotherterm.ui.DialogUtils;
import green_green_avk.anotherterm.utils.PreferenceStorage;

public final class BackendAdapterDialog {
    private BackendAdapterDialog() {
    }

    public static void show(@NonNull final AppCompatActivity activity,
                            @NonNull final PreferenceStorage ps,
                            @NonNull final Map<String, Integer> list,
                            @NonNull final Collection<? extends BackendModule.Meta.Requirement> requirements) {
        // TODO: Refactor!
        final AlertDialog.Builder b = new AlertDialog.Builder(activity).setCancelable(true);
        if (!list.isEmpty()) {
            final String[] ii = new String[list.size()];
            final boolean[] iib = new boolean[list.size()];
            int i = 0;
            for (final Map.Entry<String, Integer> ent : list.entrySet()) {
                ii[i] = (ent.getValue() == BackendModule.Meta.ADAPTER_READY ? ent.getKey() :
                        String.format(Locale.getDefault(), "%s [busy]", ent.getKey()));
                iib[i] = ent.getValue() != BackendModule.Meta.ADAPTER_READY;
                i++;
            }
            b.setItems(ii, (dialog, which) -> {
                if (iib[which])
                    return;
                ps.put("adapter", ii[which].split(" ", 2)[0]);
                final int key;
                try {
                    key = ConsoleService.startAnsiSession(activity, ps.get());
                } catch (final ConsoleService.Exception | BackendException e) {
                    Toast.makeText(activity, e.getMessage(),
                            Toast.LENGTH_LONG).show();
                    dialog.dismiss();
                    return;
                }
                ConsoleActivity.showSession(activity, key);
                dialog.dismiss();
            });
        }
        final LinearLayout wReqs;
        if (!requirements.isEmpty()) {
            wReqs = new LinearLayout(activity);
            wReqs.setOrientation(LinearLayout.VERTICAL);
            wReqs.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            ));
            b.setView(wReqs);
        } else
            wReqs = null;
        final AlertDialog d = b.create();
        if (wReqs != null)
            for (final BackendModule.Meta.Requirement req : requirements) {
                final BackendRequirementsUI.Solution solution =
                        BackendRequirementsUI.resolve(activity, req);
                if (solution != null) {
                    final View wReq = LayoutInflater.from(activity)
                            .inflate(R.layout.backend_req_entry,
                                    wReqs, false);
                    wReq.<ImageView>findViewById(R.id.icon).setImageResource(req.icon);
                    wReq.<TextView>findViewById(R.id.description).setText(req.description);
                    wReq.setOnClickListener((view) -> {
                        solution.solution(activity, req);
                        d.dismiss();
                    });
                    wReq.setLayoutParams(new ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                    ));
                    wReqs.addView(wReq);
                }
            }
        d.show();
        DialogUtils.wrapLeakageSafe(d, null);
    }
}
