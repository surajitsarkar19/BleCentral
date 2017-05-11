package rocks.keyless.app.android.bleclient.ui;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ListView;
import android.widget.TextView;

import rocks.keyless.app.android.bleclient.Constants;
import rocks.keyless.app.android.bleclient.R;
import rocks.keyless.app.android.bleclient.bluetooth.BleAdapterService;

public class BleCentralActivity extends AppCompatActivity {

    public static final String EXTRA_NAME = "name";
    public static final String EXTRA_ID = "id";

    private String deviceName;
    private String deviceAddress;

    private TextView textViewName;
    private ListView listView;

    private BleAdapterService bleService = null;

    private final ServiceConnection serviceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            Log.i(Constants.TAG,"BleService connected");
            bleService = ((BleAdapterService.LocalBinder) service).getService();
            bleService.setActivityHandler(serviceHandler);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            bleService = null;
            Log.i(Constants.TAG,"BleService disconnected");
        }
    };

    private Handler serviceHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            return false;
        }
    });


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ble_central);

        // read intent data
        final Intent intent = getIntent();
        deviceName = intent.getStringExtra(EXTRA_NAME);
        deviceAddress = intent.getStringExtra(EXTRA_ID);

        initView();

        textViewName.setText(deviceName+"("+deviceAddress+")");

        // connect to the Bluetooth adapter service
        Intent gattServiceIntent = new Intent(this, BleAdapterService.class);
        bindService(gattServiceIntent, serviceConnection, BIND_AUTO_CREATE);
    }

    private void initView(){
        textViewName = (TextView) findViewById(R.id.textViewName);
        listView = (ListView) findViewById(R.id.listView);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(serviceConnection);
        bleService = null;
    }

    public void onBackPressed() {
        Log.d(Constants.TAG, "onBackPressed");
        if (bleService.isConnected()) {
            try {
                bleService.disconnect();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            finish();
        }
    }
}
