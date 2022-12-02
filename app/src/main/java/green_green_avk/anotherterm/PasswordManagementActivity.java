package green_green_avk.anotherterm;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class PasswordManagementActivity extends AppCompatActivity {
    private static final class Adapter extends RecyclerView.Adapter<Adapter.ViewHolder> {
        private static final class ViewHolder extends RecyclerView.ViewHolder {
            public ViewHolder(@NonNull final View itemView) {
                super(itemView);
            }
        }

        private final List<String> list = new ArrayList<>();

        public void reload() {
            list.clear();
            list.addAll(PasswordService.enumerate());
            Collections.sort(list);
            notifyDataSetChanged();
        }

        @Override
        @NonNull
        public ViewHolder onCreateViewHolder(@NonNull final ViewGroup parent, final int viewType) {
            final View wRoot = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.password_management_entry,
                            parent, false);
            return new ViewHolder(wRoot);
        }

        @Override
        public void onBindViewHolder(@NonNull final ViewHolder holder, final int position) {
            final String target = list.get(position);
            final TextView wTarget = holder.itemView.findViewById(R.id.target);
            final View wDelete = holder.itemView.findViewById(R.id.action_delete);
            wTarget.setText(target);
            wDelete.setOnClickListener(view -> {
                PasswordService.remove(target, null);
                final int p = list.indexOf(target);
                if (p >= 0) {
                    list.remove(p);
                    notifyItemRemoved(p);
                }
            });
        }

        @Override
        public int getItemCount() {
            return list.size();
        }
    }

    private Adapter adapter = null;

    @Override
    protected void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.password_management_activity);
        adapter = new Adapter();
        this.<RecyclerView>findViewById(R.id.list).setAdapter(adapter);
        findViewById(R.id.action_delete).setOnClickListener(view -> {
            PasswordService.clear();
            final int l = adapter.list.size();
            adapter.list.clear();
            adapter.notifyItemRangeRemoved(0, l);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        adapter.reload();
    }
}
