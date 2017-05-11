package rocks.keyless.app.android.bleclient.ui;

import android.app.Activity;
import android.bluetooth.BluetoothGattService;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.AssetFileDescriptor;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

import java.io.IOException;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import rocks.keyless.app.android.bleclient.Constants;
import rocks.keyless.app.android.bleclient.R;
import rocks.keyless.app.android.bleclient.audio.AlarmManager;
import rocks.keyless.app.android.bleclient.bluetooth.BleAdapterService;

public class PeripheralControlActivity extends Activity {
    public static final String EXTRA_NAME = "name";
    public static final String EXTRA_ID = "id";

    private String device_name;
    private String device_address;
    private Timer mTimer;
    private boolean sound_alarm_on_disconnect = false;
    private int alert_level;
    private boolean back_requested = false;
    private boolean share_with_server = false;
    private Switch share_switch;
    private BleAdapterService bluetooth_le_adapter;

    private final ServiceConnection service_connection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            bluetooth_le_adapter = ((BleAdapterService.LocalBinder) service).getService();
            bluetooth_le_adapter.setActivityHandler(message_handler);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            bluetooth_le_adapter = null;
        }
    };

    private Handler message_handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            Bundle bundle;
            String service_uuid = "";
            String characteristic_uuid = "";
            byte[] b = null;

            switch (msg.what) {
                case BleAdapterService.MESSAGE:
                    bundle = msg.getData();
                    String text = bundle.getString(BleAdapterService.PARCEL_TEXT);
                    showMsg(text);
                    break;
                case BleAdapterService.GATT_CONNECTED:
                    ((Button) PeripheralControlActivity.this
                            .findViewById(R.id.connectButton)).setEnabled(false);
                    ((Button) PeripheralControlActivity.this.findViewById(R.id.noiseButton))
                            .setEnabled(true);
                    share_switch.setEnabled(true);
                    // we're connected
                    showMsg("CONNECTED");
                    AlarmManager am = AlarmManager.getInstance();
                    Log.d(Constants.TAG, "alarmIsSounding=" + am.alarmIsSounding());
                    if (am.alarmIsSounding()) {
                        Log.d(Constants.TAG, "Stopping alarm");
                        am.stopAlarm();
                    }
                    bluetooth_le_adapter.discoverServices();
                    break;

                case BleAdapterService.GATT_DISCONNECT:
                    ((Button) PeripheralControlActivity.this
                            .findViewById(R.id.connectButton)).setEnabled(true);
                    // we're disconnected
                    showMsg("DISCONNECTED");
                    // hide the rssi distance colored rectangle
                    ((LinearLayout) PeripheralControlActivity.this
                            .findViewById(R.id.rectangle))
                            .setVisibility(View.INVISIBLE);
                    share_switch.setEnabled(false);
                    // disable the LOW/MID/HIGH alert level selection buttons
                    ((Button) PeripheralControlActivity.this.findViewById(R.id.lowButton)).setEnabled(false);
                    ((Button) PeripheralControlActivity.this.findViewById(R.id.midButton)).setEnabled(false);
                    ((Button) PeripheralControlActivity.this.findViewById(R.id.highButton)).setEnabled(false);
                    // stop the rssi reading timer
                    stopTimer();
                    if (alert_level > 0) {
                        AlarmManager.getInstance().soundAlarm(getResources().openRawResourceFd(R.raw.alarm));
                    }
                    if (back_requested) {
                        PeripheralControlActivity.this.finish();
                    }
                    break;

                case BleAdapterService.GATT_SERVICES_DISCOVERED:

                    // validate services and if ok....
                    List<BluetoothGattService> slist = bluetooth_le_adapter.getSupportedGattServices();
                    boolean link_loss_present = false;
                    boolean immediate_alert_present = false;
                    boolean tx_power_present = false;
                    boolean proximity_monitoring_present = false;

                    for (BluetoothGattService svc : slist) {
                        Log.d(Constants.TAG, "UUID=" + svc.getUuid().toString().toUpperCase() + " INSTANCE=" + svc.getInstanceId());
                        if (svc.getUuid().toString().equalsIgnoreCase(BleAdapterService.LINK_LOSS_SERVICE_UUID)) {
                            link_loss_present = true;
                            continue;
                        }
                        if (svc.getUuid().toString().equalsIgnoreCase(BleAdapterService.IMMEDIATE_ALERT_SERVICE_UUID)) {
                            immediate_alert_present = true;
                            continue;
                        }
                        if (svc.getUuid().toString().equalsIgnoreCase(BleAdapterService.TX_POWER_SERVICE_UUID)) {
                            tx_power_present = true;
                            continue;
                        }
                        if (svc.getUuid().toString().equalsIgnoreCase(BleAdapterService.PROXIMITY_MONITORING_SERVICE_UUID)) {
                            proximity_monitoring_present = true;
                            continue;
                        }
                    }

                    if (link_loss_present && immediate_alert_present && tx_power_present && proximity_monitoring_present) {
                        showMsg("Device has expected services");

                        // show the rssi distance colored rectangle
                        ((LinearLayout) PeripheralControlActivity.this
                                .findViewById(R.id.rectangle))
                                .setVisibility(View.VISIBLE);
                        // enable the LOW/MID/HIGH alert level selection buttons
                        ((Button) PeripheralControlActivity.this.findViewById(R.id.lowButton)).setEnabled(true);
                        ((Button) PeripheralControlActivity.this.findViewById(R.id.midButton)).setEnabled(true);
                        ((Button) PeripheralControlActivity.this.findViewById(R.id.highButton)).setEnabled(true);

                        showMsg("Reading alert_level");

                        bluetooth_le_adapter.readCharacteristic(
                                BleAdapterService.LINK_LOSS_SERVICE_UUID,
                                BleAdapterService.ALERT_LEVEL_CHARACTERISTIC);

                    } else {
                        showMsg("Device does not have expected GATT services");
                    }
                    break;

                case BleAdapterService.GATT_REMOTE_RSSI:
                    bundle = msg.getData();
                    int rssi = bundle.getInt(BleAdapterService.PARCEL_RSSI);
                    PeripheralControlActivity.this.updateRssi(rssi);
                    break;

                case BleAdapterService.GATT_CHARACTERISTIC_READ:
                    bundle = msg.getData();
                    Log.d(Constants.TAG, "Service=" + bundle.get(BleAdapterService.PARCEL_SERVICE_UUID).toString().toUpperCase() + " Characteristic=" + bundle.get(BleAdapterService.PARCEL_CHARACTERISTIC_UUID).toString().toUpperCase());
                    if (bundle.get(BleAdapterService.PARCEL_CHARACTERISTIC_UUID).toString().toUpperCase()
                            .equals(BleAdapterService.ALERT_LEVEL_CHARACTERISTIC)
                            && bundle.get(BleAdapterService.PARCEL_SERVICE_UUID).toString().toUpperCase()
                            .equals(BleAdapterService.LINK_LOSS_SERVICE_UUID)) {
                        b = bundle.getByteArray(BleAdapterService.PARCEL_VALUE);
                        if (b.length > 0) {
                            PeripheralControlActivity.this.setAlertLevel((int) b[0]);
                            Log.d(Constants.TAG, "Current alert_level is set to: " + b[0]);
                            // show the rssi distance colored rectangle
                            ((LinearLayout) PeripheralControlActivity.this
                                    .findViewById(R.id.rectangle))
                                    .setVisibility(View.VISIBLE);

                            // start off the rssi reading timer
                            startReadRssiTimer();
                        }
                    }
                    break;
                case BleAdapterService.GATT_CHARACTERISTIC_WRITTEN:
                    bundle = msg.getData();
                    Log.d(Constants.TAG, "Service=" + bundle.get(BleAdapterService.PARCEL_SERVICE_UUID).toString().toUpperCase() + " Characteristic=" + bundle.get(BleAdapterService.PARCEL_CHARACTERISTIC_UUID).toString().toUpperCase());
                    if (bundle.get(BleAdapterService.PARCEL_CHARACTERISTIC_UUID).toString()
                            .toUpperCase().equals(BleAdapterService.ALERT_LEVEL_CHARACTERISTIC)
                            && bundle.get(BleAdapterService.PARCEL_SERVICE_UUID).toString()
                            .toUpperCase().equals(BleAdapterService.LINK_LOSS_SERVICE_UUID)) {
                        b = bundle.getByteArray(BleAdapterService.PARCEL_VALUE);
                        Log.d(Constants.TAG, "New alert_level set to: " + b[0]);
                        PeripheralControlActivity.this.setAlertLevel((int) b[0]);
                    }
                    break;
            }
        }
    };

    public void onLow(View view) {
        bluetooth_le_adapter.writeCharacteristic(BleAdapterService.LINK_LOSS_SERVICE_UUID, BleAdapterService.ALERT_LEVEL_CHARACTERISTIC, Constants.ALERT_LEVEL_LOW);
    }

    public void onMid(View view) {
        bluetooth_le_adapter.writeCharacteristic(BleAdapterService.LINK_LOSS_SERVICE_UUID, BleAdapterService.ALERT_LEVEL_CHARACTERISTIC, Constants.ALERT_LEVEL_MID);
    }

    public void onHigh(View view) {
        bluetooth_le_adapter.writeCharacteristic(BleAdapterService.LINK_LOSS_SERVICE_UUID, BleAdapterService.ALERT_LEVEL_CHARACTERISTIC, Constants.ALERT_LEVEL_HIGH);
    }

    public void onNoise(View view) {
        byte[] al = new byte[1];
        al[0] = (byte) alert_level;
        bluetooth_le_adapter.writeCharacteristic(BleAdapterService.IMMEDIATE_ALERT_SERVICE_UUID, BleAdapterService.ALERT_LEVEL_CHARACTERISTIC, al);
    }

    public void onBackPressed() {
        Log.d(Constants.TAG, "onBackPressed");
        back_requested = true;
        if (bluetooth_le_adapter.isConnected()) {
            try {
                bluetooth_le_adapter.disconnect();
            } catch (Exception e) {
            }
        } else {
            finish();
        }
    }

    private void setAlertLevel(int alert_level) {
        this.alert_level = alert_level;
        ((Button) this.findViewById(R.id.lowButton)).setTextColor(Color.parseColor("#000000"));

        ((Button) this.findViewById(R.id.midButton)).setTextColor(Color.parseColor("#000000"));

        ((Button) this.findViewById(R.id.highButton)).setTextColor(Color.parseColor("#000000"));

        switch (alert_level) {
            case 0:
                ((Button) this.findViewById(R.id.lowButton)).setTextColor(Color.parseColor("#FF0000"));
                ;
                break;
            case 1:
                ((Button) this.findViewById(R.id.midButton)).setTextColor(Color.parseColor("#FF0000"));
                ;
                break;
            case 2:
                ((Button) this.findViewById(R.id.highButton)).setTextColor(Color.parseColor("#FF0000"));
                ;
                break;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_peripheral_control);

        // read intent data
        final Intent intent = getIntent();
        device_name = intent.getStringExtra(EXTRA_NAME);
        device_address = intent.getStringExtra(EXTRA_ID);

        // show the device name
        ((TextView) this.findViewById(R.id.nameTextView)).setText("Device : " + device_name + " [" + device_address + "]");
        // hide the coloured rectangle used to show green/amber/red rssi distance
        ((LinearLayout) this.findViewById(R.id.rectangle)).setVisibility(View.INVISIBLE);

        // hide the coloured rectangle used to show green/amber/red rssi
        // distance
        ((LinearLayout) this.findViewById(R.id.rectangle))
                .setVisibility(View.INVISIBLE);

        // disable the noise button
        ((Button) PeripheralControlActivity.this.findViewById(R.id.noiseButton))
                .setEnabled(false);

        // disable the LOW/MID/HIGH alert level selection buttons
        ((Button) this.findViewById(R.id.lowButton)).setEnabled(false);
        ((Button) this.findViewById(R.id.midButton)).setEnabled(false);
        ((Button) this.findViewById(R.id.highButton)).setEnabled(false);

        share_switch = (Switch) this.findViewById(R.id.switch1);
        share_switch.setEnabled(false);
        share_switch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView,
                                         boolean isChecked) {
                if (bluetooth_le_adapter != null) {
                    share_with_server = isChecked;
                    if (!isChecked && bluetooth_le_adapter.isConnected()) {
                        showMsg("Switched off sharing proximity data");
                        // write 0,0 to cause Arduino to switch off all LEDs
                        if (bluetooth_le_adapter.writeCharacteristic(
                                BleAdapterService.PROXIMITY_MONITORING_SERVICE_UUID,
                                BleAdapterService.CLIENT_PROXIMITY_CHARACTERISTIC,
                                new byte[]{0, 0})) {
                        } else {
                            showMsg("Failed to inform Arduino sharing has been disabled");
                        }
                    }
                }
            }
        });

        // connect to the Bluetooth adapter service
        Intent gattServiceIntent = new Intent(this, BleAdapterService.class);
        bindService(gattServiceIntent, service_connection, BIND_AUTO_CREATE);

        showMsg("READY");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopTimer();
        unbindService(service_connection);
        bluetooth_le_adapter = null;
    }

    private void showMsg(final String msg) {
        Log.d(Constants.TAG, msg);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ((TextView) findViewById(R.id.msgTextView)).setText(msg);
            }
        });
    }

    public void onConnect(View view) {
        showMsg("onConnect");
        if (bluetooth_le_adapter != null) {
            if (bluetooth_le_adapter.connect(device_address)) {
                ((Button) PeripheralControlActivity.this
                        .findViewById(R.id.connectButton)).setEnabled(false);
            } else {
                showMsg("onConnect: failed to connect");
            }
        } else {
            showMsg("onConnect: bluetooth_le_adapter=null");
        }
    }

    private void startReadRssiTimer() {
        mTimer = new Timer();
        mTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                bluetooth_le_adapter.readRemoteRssi();
            }

        }, 0, 2000);
    }

    private void stopTimer() {
        if (mTimer != null) {
            mTimer.cancel();
            mTimer = null;
        }
    }

    private void updateRssi(int rssi) {
        ((TextView) findViewById(R.id.rssiTextView)).setText("RSSI = " + Integer.toString(rssi));
        LinearLayout layout = ((LinearLayout) PeripheralControlActivity.this.findViewById(R.id.rectangle));

        byte proximity_band = 3;
        if (rssi < -80) {
            layout.setBackgroundColor(0xFFFF0000);
        } else if (rssi < -50) {
            layout.setBackgroundColor(0xFFFF8A01);
            proximity_band = 2;
        } else {
            layout.setBackgroundColor(0xFF00FF00);
            proximity_band = 1;
        }
        layout.invalidate();

        if (share_with_server) {
            if (bluetooth_le_adapter.writeCharacteristic(
                    BleAdapterService.PROXIMITY_MONITORING_SERVICE_UUID,
                    BleAdapterService.CLIENT_PROXIMITY_CHARACTERISTIC,
                    new byte[]{proximity_band, (byte) rssi})) {
                showMsg("proximity data shared: proximity_band:" + proximity_band + ",rssi:" + rssi);
            } else {
                showMsg("Failed to share proximity data");
            }
        }

    }

}
