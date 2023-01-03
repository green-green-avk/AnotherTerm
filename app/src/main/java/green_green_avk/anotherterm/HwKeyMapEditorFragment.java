package green_green_avk.anotherterm;

import android.app.Fragment;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;

import java.util.AbstractList;
import java.util.Arrays;
import java.util.List;

import green_green_avk.anotherterm.ui.DialogUtils;
import green_green_avk.anotherterm.ui.HwKeyMap;

public final class HwKeyMapEditorFragment extends Fragment {
    private static final class DevIdLabel {
        @DrawableRes
        final int icon;
        @StringRes
        final int desc;

        public DevIdLabel(@DrawableRes final int icon, @StringRes final int desc) {
            this.icon = icon;
            this.desc = desc;
        }
    }

    private static final DevIdLabel devIdLabelUnknown =
            new DevIdLabel(R.drawable.ic_keys_unknown, R.string.label_dev_unknown);

    private static final DevIdLabel[] devIdLabels = new DevIdLabel[]{
            new DevIdLabel(R.drawable.ic_keys_built_in, R.string.label_dev_built_in),
            new DevIdLabel(R.drawable.ic_keys_external, R.string.label_dev_external)
    };

    private static DevIdLabel getDevIdLabel(final int devId) {
        if (devId < 0 || devId >= devIdLabels.length) return devIdLabelUnknown;
        return devIdLabels[devId];
    }

    private final class ToKeycode {
        public final int value;
        public final int toggleMode;
        @NonNull
        private final Object label;

        @Override
        @NonNull
        public String toString() {
            final String r;
            if (label instanceof String) r = (String) label;
            else if (label instanceof Integer && getActivity() != null)
                r = getActivity().getString((Integer) label);
            else r = "-";
            switch (toggleMode) {
                case HwKeyMap.TOGGLE_ONESHOT:
                    return r + "\uD83D\uDD12¹";
                case HwKeyMap.TOGGLE_ON_OFF:
                    return r + "\uD83D\uDD12";
            }
            return r;
        }

        public ToKeycode(final int value, final int toggleMode, @NonNull final Object label) {
            this.value = value;
            this.toggleMode = toggleMode;
            this.label = label;
        }

        private ToKeycode(final int value, @NonNull final Object label) {
            this(value, HwKeyMap.TOGGLE_NONE, label);
        }

        private ToKeycode(final int value) {
            this(value, TermKeyMap.keyCodeToString(value));
        }
    }

    private final List<ToKeycode> toKeycodeListForModifiers = Arrays.asList(
            new ToKeycode(HwKeyMap.KEYCODE_ACTION_BYPASS, R.string.label_key_bypass), // Modifiers only.
            new ToKeycode(KeyEvent.KEYCODE_UNKNOWN, R.string.label_key_block),
            new ToKeycode(KeyEvent.KEYCODE_CTRL_LEFT, R.string.label_mod_control),
            new ToKeycode(KeyEvent.KEYCODE_CTRL_LEFT, HwKeyMap.TOGGLE_ONESHOT, R.string.label_mod_control),
            new ToKeycode(KeyEvent.KEYCODE_CTRL_LEFT, HwKeyMap.TOGGLE_ON_OFF, R.string.label_mod_control),
            new ToKeycode(KeyEvent.KEYCODE_ALT_LEFT, R.string.label_mod_alt),
            new ToKeycode(KeyEvent.KEYCODE_ALT_LEFT, HwKeyMap.TOGGLE_ONESHOT, R.string.label_mod_alt),
            new ToKeycode(KeyEvent.KEYCODE_ALT_LEFT, HwKeyMap.TOGGLE_ON_OFF, R.string.label_mod_alt),
            new ToKeycode(KeyEvent.KEYCODE_ESCAPE),
            new ToKeycode(KeyEvent.KEYCODE_FORWARD_DEL),
            new ToKeycode(KeyEvent.KEYCODE_INSERT),
            new ToKeycode(KeyEvent.KEYCODE_TAB),
            new ToKeycode(KeyEvent.KEYCODE_F1),
            new ToKeycode(KeyEvent.KEYCODE_F2),
            new ToKeycode(KeyEvent.KEYCODE_F3),
            new ToKeycode(KeyEvent.KEYCODE_F4),
            new ToKeycode(KeyEvent.KEYCODE_F5),
            new ToKeycode(KeyEvent.KEYCODE_F6),
            new ToKeycode(KeyEvent.KEYCODE_F7),
            new ToKeycode(KeyEvent.KEYCODE_F8),
            new ToKeycode(KeyEvent.KEYCODE_F9),
            new ToKeycode(KeyEvent.KEYCODE_F10),
            new ToKeycode(KeyEvent.KEYCODE_F11),
            new ToKeycode(KeyEvent.KEYCODE_F12),
            new ToKeycode(KeyEvent.KEYCODE_MOVE_HOME),
            new ToKeycode(KeyEvent.KEYCODE_MOVE_END),
            new ToKeycode(KeyEvent.KEYCODE_PAGE_UP),
            new ToKeycode(KeyEvent.KEYCODE_PAGE_DOWN)
    );

    private final List<ToKeycode> toKeycodeList = new AbstractList<ToKeycode>() {
        @Override
        public ToKeycode get(final int index) {
            return toKeycodeListForModifiers.get(index + 1);
        }

        @Override
        public int size() {
            return toKeycodeListForModifiers.size() - 1;
        }
    };

    private HwKeyMapTable keymap = new HwKeyMapTable();

    private final class Adapter extends BaseAdapter {

        @Override
        public int getCount() {
            return keymap.getSize();
        }

        @Override
        public Object getItem(final int position) {
            return keymap.getEntry(position);
        }

        @Override
        public long getItemId(final int position) {
            return position;
        }

        @Override
        public View getView(final int position, final View convertView, final ViewGroup parent) {
            final View v;
            if (convertView != null) v = convertView;
            else v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.hw_key_map_editor_entry, parent, false);
            final HwKeyMapTable.Entry entry = keymap.getEntry(position);
            v.<TextView>findViewById(R.id.keycode)
                    .setText(TermKeyMap.keyCodeToString(entry.keycode));
            final ImageView wDevId = v.findViewById(R.id.devId);
            wDevId.setImageResource(getDevIdLabel(entry.devType).icon);
            wDevId.setContentDescription(getString(getDevIdLabel(entry.devType).desc));
            final Spinner wToKeycode = v.findViewById(R.id.toKeycode);
            final List<ToKeycode> al = KeyEvent.isModifierKey(entry.keycode) ?
                    toKeycodeListForModifiers : toKeycodeList;
            final ArrayAdapter<ToKeycode> a = new ArrayAdapter<>(parent.getContext(),
                    R.layout.spinner_autosize_entry, al);
            a.setDropDownViewResource(R.layout.spinner_autosize_dropdown_entry);
            wToKeycode.setAdapter(a);
            int ap = 0;
            for (int i = 0; i < al.size(); i++) {
                final ToKeycode ae = al.get(i);
                if (ae.value == entry.toKeycode && ae.toggleMode == entry.toggleMode) {
                    ap = i;
                    break;
                }
            }
            wToKeycode.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(final AdapterView<?> parent, final View view,
                                           final int position, final long id) {
                    final ToKeycode ae = a.getItem(position);
                    if (keymap.get(entry.keycode, entry.devType) == ae.value
                            && keymap.getToggleMode(entry.keycode, entry.devType) == ae.toggleMode)
                        return;
                    keymap.set(entry.keycode, entry.devType, ae.value);
                    keymap.setToggleMode(entry.keycode, entry.devType, ae.toggleMode);
                    HwKeyMapManager.set(keymap);
                    notifyDataSetChanged();
                }

                @Override
                public void onNothingSelected(final AdapterView<?> parent) {
                }
            });
            wToKeycode.setSelection(ap);
            v.findViewById(R.id.action_delete).setOnClickListener(v1 -> {
                keymap.set(entry.keycode, entry.devType, HwKeyMap.KEYCODE_ACTION_DEFAULT);
                keymap.setToggleMode(entry.keycode, entry.devType, HwKeyMap.TOGGLE_NONE);
                HwKeyMapManager.set(keymap);
                notifyDataSetChanged();
            });
            return v;
        }
    }

    private final Adapter adapter = new Adapter();

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
                             final Bundle savedInstanceState) {
        final View v = inflater.inflate(R.layout.hw_key_map_editor_fragment, container,
                false);
        final HwKeyMap km = HwKeyMapManager.get();
        if (km instanceof HwKeyMapTable) keymap = (HwKeyMapTable) HwKeyMapManager.get();
        final ListView wList = v.findViewById(R.id.list);
        wList.setAdapter(adapter);
        wList.setFocusable(false);
        v.findViewById(R.id.action_add).setOnClickListener(this::onAdd);
        return v;
    }

    public void onAdd(final View view) {
        final View v = LayoutInflater.from(this.getActivity())
                .inflate(R.layout.hw_key_map_editor_prompt, null);
        v.setFocusableInTouchMode(true);
        final AlertDialog d = new AlertDialog.Builder(getActivity()).setView(v)
                .setNegativeButton(android.R.string.cancel, null).create();
        v.requestFocus();
        v.setOnKeyListener((v1, keyCode, event) -> {
            final int devId = keymap.getDevType(event);
            if (devId < 0 || HwKeyMapManager.isBypassKey(event)
                    || keyCode == KeyEvent.KEYCODE_UNKNOWN) return false;
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                final int km = keymap.get(keyCode, devId);
                if (km == HwKeyMap.KEYCODE_ACTION_DEFAULT) {
                    keymap.set(keyCode, devId, KeyEvent.KEYCODE_UNKNOWN);
                    HwKeyMapManager.set(keymap);
                    adapter.notifyDataSetChanged();
                }
                d.dismiss();
            }
            return true;
        });
        d.show();
        DialogUtils.wrapLeakageSafe(d, null);
    }
}
