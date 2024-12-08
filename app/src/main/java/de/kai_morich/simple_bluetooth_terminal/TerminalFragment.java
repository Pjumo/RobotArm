package de.kai_morich.simple_bluetooth_terminal;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.method.ScrollingMovementMethod;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.ArrayDeque;
import java.util.Arrays;

public class TerminalFragment extends Fragment implements ServiceConnection, SerialListener {

    private enum Connected {False, Pending, True}

    private String deviceAddress;
    private SerialService service;

    //    private TextView receiveText;
    private TextView sendText1;
    private TextView sendText2;
    private TextView sendText3;
    private TextView sendText4;
    private TextView sendText5;
    private TextView sendText6;

    private Connected connected = Connected.False;
    private boolean initialStart = true;
    private boolean hexEnabled = false;
    private boolean pendingNewline = false;
    private String newline = TextUtil.newline_crlf;

    private String[] angle;

    /*
     * Lifecycle
     */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        setRetainInstance(true);
        deviceAddress = getArguments().getString("device");
        angle = new String[]{"90", "90", "120", "90", "90", "90"};
    }

    @Override
    public void onDestroy() {
        if (connected != Connected.False)
            disconnect();
        getActivity().stopService(new Intent(getActivity(), SerialService.class));
        super.onDestroy();
    }

    @Override
    public void onStart() {
        super.onStart();
        if (service != null)
            service.attach(this);
        else
            getActivity().startService(new Intent(getActivity(), SerialService.class)); // prevents service destroy on unbind from recreated activity caused by orientation change
    }

    @Override
    public void onStop() {
        if (service != null && !getActivity().isChangingConfigurations())
            service.detach();
        super.onStop();
    }

    @SuppressWarnings("deprecation")
    // onAttach(context) was added with API 23. onAttach(activity) works for all API versions
    @Override
    public void onAttach(@NonNull Activity activity) {
        super.onAttach(activity);
        getActivity().bindService(new Intent(getActivity(), SerialService.class), this, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onDetach() {
        try {
            getActivity().unbindService(this);
        } catch (Exception ignored) {
        }
        super.onDetach();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (initialStart && service != null) {
            initialStart = false;
            getActivity().runOnUiThread(this::connect);
        }
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder binder) {
        service = ((SerialService.SerialBinder) binder).getService();
        service.attach(this);
        if (initialStart && isResumed()) {
            initialStart = false;
            getActivity().runOnUiThread(this::connect);
        }
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        service = null;
    }

    /*
     * UI
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_angle, container, false);
//        receiveText = view.findViewById(R.id.receive_text);                          // TextView performance decreases with number of spans
//        receiveText.setTextColor(getResources().getColor(R.color.colorRecieveText)); // set as default color to reduce number of spans
//        receiveText.setMovementMethod(ScrollingMovementMethod.getInstance());

        sendText1 = view.findViewById(R.id.send_text_s1);
        sendText2 = view.findViewById(R.id.send_text_s2);
        sendText3 = view.findViewById(R.id.send_text_s3);
        sendText4 = view.findViewById(R.id.send_text_s4);
        sendText5 = view.findViewById(R.id.send_text_s5);
        sendText6 = view.findViewById(R.id.send_text_s6);

        View resetBtn = view.findViewById(R.id.reset_btn);
        resetBtn.setOnClickListener(v -> {
            send("s1 90 s2 90 s3 120 s4 90 s5 90 s6 100");
            sendText1.setText("90");
            sendText2.setText("90");
            sendText3.setText("120");
            sendText4.setText("90");
            sendText5.setText("90");
            sendText6.setText("90");
        });

        View sendBtn = view.findViewById(R.id.send_btn);
        sendBtn.setOnClickListener(v -> {
            String text1 = sendText1.getText().toString();
            String text2 = sendText2.getText().toString();
            String text3 = sendText3.getText().toString();
            String text4 = sendText4.getText().toString();
            String text5 = sendText5.getText().toString();
            String text6 = sendText6.getText().toString();
            String fullText = "";
//            try {
            if (!text1.isEmpty()) {
                if (0 < Integer.parseInt(text1) && 180 >= Integer.parseInt(text1)) {
//                        send("s1 " + text1);
                    fullText = fullText + "s1 " + text1;
                    angle[0] = text1;
                } else {
                    fullText = fullText + "s1 " + angle[0];
                }
            } else {
                fullText = fullText + "s1 " + angle[0];
            }
            if (!text2.isEmpty()) {
                if (0 < Integer.parseInt(text2) && 180 >= Integer.parseInt(text2)) {
//                        send("s2 " + text2);
                    fullText = fullText + " s2 " + text2;
                    angle[1] = text2;
                } else {
                    fullText = fullText + "s2 " + angle[1];
                }
            } else {
                fullText = fullText + "s2 " + angle[1];
            }
            if (!text3.isEmpty()) {
                if (0 < Integer.parseInt(text3) && 180 >= Integer.parseInt(text3)) {
//                        send("s3 " + text3);
                    fullText = fullText + " s3 " + text3;
                    angle[2] = text3;
                } else {
                    fullText = fullText + "s3 " + angle[2];
                }
            } else {
                fullText = fullText + "s3 " + angle[2];
            }
            if (!text4.isEmpty()) {
                if (0 < Integer.parseInt(text4) && 180 >= Integer.parseInt(text4)) {
//                        send("s4 " + text4);
                    fullText = fullText + " s4 " + text4;
                    angle[3] = text4;
                } else {
                    fullText = fullText + "s4 " + angle[3];
                }
            } else {
                fullText = fullText + "s4 " + angle[3];
            }
            if (!text5.isEmpty()) {
                if (0 < Integer.parseInt(text5) && 180 >= Integer.parseInt(text5)) {
//                        send("s5 " + text5);
                    fullText = fullText + " s5 " + text5;
                    angle[4] = text5;
                } else {
                    fullText = fullText + "s5 " + angle[4];
                }
            } else {
                fullText = fullText + "s5 " + angle[4];
            }
            if (!text6.isEmpty()) {
                if (80 <= Integer.parseInt(text6) && 160 >= Integer.parseInt(text6)) {
//                        send("s6 " + text6);
                    fullText = fullText + " s6 " + text6;
                    angle[5] = text6;
                } else {
                    fullText = fullText + "s6 " + angle[5];
                }
            } else {
                fullText = fullText + "s6 " + angle[5];
            }
            send(fullText);
//            }catch (InterruptedException e) {
//                e.printStackTrace();
//            }
        });
        return view;
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_terminal, menu);
    }

    public void onPrepareOptionsMenu(@NonNull Menu menu) {
        menu.findItem(R.id.hex).setChecked(hexEnabled);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            menu.findItem(R.id.backgroundNotification).setChecked(service != null && service.areNotificationsEnabled());
        } else {
            menu.findItem(R.id.backgroundNotification).setChecked(true);
            menu.findItem(R.id.backgroundNotification).setEnabled(false);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.clear) {
//            receiveText.setText("");
            return true;
        } else if (id == R.id.newline) {
            String[] newlineNames = getResources().getStringArray(R.array.newline_names);
            String[] newlineValues = getResources().getStringArray(R.array.newline_values);
            int pos = java.util.Arrays.asList(newlineValues).indexOf(newline);
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle("Newline");
            builder.setSingleChoiceItems(newlineNames, pos, (dialog, item1) -> {
                newline = newlineValues[item1];
                dialog.dismiss();
            });
            builder.create().show();
            return true;
        } else if (id == R.id.hex) {
            hexEnabled = !hexEnabled;
            sendText1.setText("");
            item.setChecked(hexEnabled);
            return true;
        } else if (id == R.id.backgroundNotification) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!service.areNotificationsEnabled() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 0);
                } else {
                    showNotificationSettings();
                }
            }
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    /*
     * Serial + UI
     */
    private void connect() {
        try {
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            BluetoothDevice device = bluetoothAdapter.getRemoteDevice(deviceAddress);
            status("connecting...");
            connected = Connected.Pending;
            SerialSocket socket = new SerialSocket(getActivity().getApplicationContext(), device);
            service.connect(socket);
        } catch (Exception e) {
            onSerialConnectError(e);
        }
    }

    private void disconnect() {
        connected = Connected.False;
        service.disconnect();
    }

    private void send(String str) {
        if (connected != Connected.True) {
            Toast.makeText(getActivity(), "not connected", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            String msg;
            byte[] data;
            if (hexEnabled) {
                StringBuilder sb = new StringBuilder();
                TextUtil.toHexString(sb, TextUtil.fromHexString(str));
                TextUtil.toHexString(sb, newline.getBytes());
                msg = sb.toString();
                data = TextUtil.fromHexString(msg);
            } else {
                msg = str;
                data = (str + newline).getBytes();
            }
            SpannableStringBuilder spn = new SpannableStringBuilder(msg + '\n');
            spn.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.colorSendText)), 0, spn.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
//            receiveText.append(spn);
            Toast.makeText(getActivity(), spn, Toast.LENGTH_SHORT).show();
            service.write(data);
        } catch (Exception e) {
            onSerialIoError(e);
        }
    }

    private void receive(ArrayDeque<byte[]> datas) {
        SpannableStringBuilder spn = new SpannableStringBuilder();
        for (byte[] data : datas) {
            if (hexEnabled) {
                spn.append(TextUtil.toHexString(data)).append('\n');
            } else {
                String msg = new String(data);
                if (newline.equals(TextUtil.newline_crlf) && msg.length() > 0) {
                    // don't show CR as ^M if directly before LF
                    msg = msg.replace(TextUtil.newline_crlf, TextUtil.newline_lf);
                    // special handling if CR and LF come in separate fragments
                    if (pendingNewline && msg.charAt(0) == '\n') {
                        if (spn.length() >= 2) {
                            spn.delete(spn.length() - 2, spn.length());
                        }
//                        else {
//                            Editable edt = receiveText.getEditableText();
//                            if (edt != null && edt.length() >= 2)
//                                edt.delete(edt.length() - 2, edt.length());
//                        }
                    }
                    pendingNewline = msg.charAt(msg.length() - 1) == '\r';
                }
                spn.append(TextUtil.toCaretString(msg, newline.length() != 0));
            }
        }
//        receiveText.append(spn);
    }

    private void status(String str) {
        SpannableStringBuilder spn = new SpannableStringBuilder(str + '\n');
        spn.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.colorStatusText)), 0, spn.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
//        receiveText.append(spn);
        Toast.makeText(getActivity().getApplicationContext(), spn, Toast.LENGTH_SHORT).show();
    }

    /*
     * starting with Android 14, notifications are not shown in notification bar by default when App is in background
     */

    private void showNotificationSettings() {
        Intent intent = new Intent();
        intent.setAction("android.settings.APP_NOTIFICATION_SETTINGS");
        intent.putExtra("android.provider.extra.APP_PACKAGE", getActivity().getPackageName());
        startActivity(intent);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (Arrays.equals(permissions, new String[]{Manifest.permission.POST_NOTIFICATIONS}) &&
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !service.areNotificationsEnabled())
            showNotificationSettings();
    }

    /*
     * SerialListener
     */
    @Override
    public void onSerialConnect() {
        status("connected");
        connected = Connected.True;
    }

    @Override
    public void onSerialConnectError(Exception e) {
        status("connection failed: " + e.getMessage());
        disconnect();
    }

    @Override
    public void onSerialRead(byte[] data) {
        ArrayDeque<byte[]> datas = new ArrayDeque<>();
        datas.add(data);
//        receive(datas);
    }

    public void onSerialRead(ArrayDeque<byte[]> datas) {
        receive(datas);
    }

    @Override
    public void onSerialIoError(Exception e) {
        status("connection lost: " + e.getMessage());
        disconnect();
    }

}