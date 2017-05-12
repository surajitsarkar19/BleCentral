package rocks.keyless.app.android.bleclient.ui;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.nio.ByteBuffer;
import java.util.List;

import rocks.keyless.app.android.bleclient.Constants;
import rocks.keyless.app.android.bleclient.R;
import rocks.keyless.app.android.bleclient.adapter.CharacteristicsListAdapter;
import rocks.keyless.app.android.bleclient.adapter.ServicesListAdapter;
import rocks.keyless.app.android.bleclient.bluetooth.BleAdapterService;

public class BleCentralActivity extends AppCompatActivity implements View.OnClickListener, AdapterView.OnItemClickListener {

    public static final String EXTRA_NAME = "name";
    public static final String EXTRA_ID = "id";

    private String deviceName;
    private String deviceAddress;

    private TextView textViewName;
    private ListView listView;
    private Button buttonConnect;

    private BleAdapterService bleService = null;

    ServicesListAdapter serviceAdaper;
    CharacteristicsListAdapter characteristicsAdapter;
    BluetoothGattService selectedGattService;

    enum BleMode{SERVICE_CONNECTION,SERVICE_DISCOVERY,CHARACTERISTCS_DISCOVERY};

    private BleMode blemode;

    private final ServiceConnection serviceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            Log.i(Constants.TAG,"BleService connected");
            bleService = ((BleAdapterService.LocalBinder) service).getService();
            bleService.setActivityHandler(serviceHandler);
            connect();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            bleService = null;
            Log.i(Constants.TAG,"BleService disconnected");
        }
    };

    private Handler serviceHandler = new Handler(){
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
                    showMsg("Connected");
                    setConnectButtonText("Disconnect");
                    discoverServices();
                    blemode = BleMode.SERVICE_CONNECTION;
                    showToast("Discovering Services...");
                    break;
                case BleAdapterService.GATT_DISCONNECT:
                    showMsg("Disconnected");
                    setConnectButtonText("Connect");
                    serviceAdaper.clear();
                    break;
                case BleAdapterService.GATT_SERVICES_DISCOVERED:
                    showMsg("Service Discovered");
                    blemode = BleMode.SERVICE_DISCOVERY;
                    List<BluetoothGattService> slist = bleService.getSupportedGattServices();
                    serviceAdaper.addService(slist);
                    setAdapter();
                    break;
            }
        }
    };

    private void setConnectButtonText(final String txt){

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                buttonConnect.setText(txt);
            }
        });
    }

    private void showMsg(final String msg) {
        Log.d(Constants.TAG, msg);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                textViewName.setText(msg);
            }
        });
    }

    private void showToast(String msg){
        Toast.makeText(BleCentralActivity.this,msg,Toast.LENGTH_SHORT).show();
    }

    private void setAdapter(){
        if(blemode == BleMode.SERVICE_DISCOVERY)
            listView.setAdapter(serviceAdaper);
        else if(blemode == BleMode.CHARACTERISTCS_DISCOVERY)
            listView.setAdapter(characteristicsAdapter);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ble_central);

        // read intent data
        final Intent intent = getIntent();
        deviceName = intent.getStringExtra(EXTRA_NAME);
        deviceAddress = intent.getStringExtra(EXTRA_ID);

        initView();

        setTitle(deviceName+"("+deviceAddress+")");

        // connect to the Bluetooth adapter service
        Intent gattServiceIntent = new Intent(this, BleAdapterService.class);
        //bindService(gattServiceIntent, serviceConnection, BIND_AUTO_CREATE);

        // Actually set it in response to ACTION_PAIRING_REQUEST.
        IntentFilter pairingRequestFilter = new IntentFilter();
        pairingRequestFilter.addAction(BluetoothDevice.ACTION_PAIRING_REQUEST);
        pairingRequestFilter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        pairingRequestFilter.setPriority(IntentFilter.SYSTEM_HIGH_PRIORITY - 1);
        getApplicationContext().registerReceiver(mPairingRequestRecevier, pairingRequestFilter);

        pairDevice(deviceAddress,123456);
    }

    private void pairDevice(String address, int pin){
        BluetoothDevice device = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(address);

        device.createBond();


        /*Intent intent = new Intent(BluetoothDevice.ACTION_PAIRING_REQUEST);
        intent.putExtra(BluetoothDevice.EXTRA_DEVICE, device);
        intent.putExtra(BluetoothDevice.EXTRA_PAIRING_VARIANT, pin);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivityForResult(intent,7);
*/
        /*Intent intent = new Intent(BluetoothDevice.ACTION_PAIRING_REQUEST);
        intent.putExtra(BluetoothDevice.EXTRA_DEVICE, device);
        intent.putExtra(BluetoothDevice.EXTRA_PAIRING_VARIANT, pin);
        sendBroadcast(intent);*/
    }

    private void initView(){
        textViewName = (TextView) findViewById(R.id.textViewName);
        listView = (ListView) findViewById(R.id.listView);
        buttonConnect = (Button) findViewById(R.id.buttonConnect);

        buttonConnect.setOnClickListener(this);
        buttonConnect.setText("Connect");
        listView.setOnItemClickListener(this);

        serviceAdaper = new ServicesListAdapter();
        characteristicsAdapter = new CharacteristicsListAdapter();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(serviceConnection);
        bleService = null;
    }

    private void disconnect(){
        try {
            bleService.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void connect(){
        try {
            bleService.connect(deviceAddress);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void discoverServices(){
        try {
            bleService.discoverServices();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void onBackPressed() {
        Log.d(Constants.TAG, "onBackPressed");
        if (bleService.isConnected()) {
            if(blemode == BleMode.CHARACTERISTCS_DISCOVERY){
                blemode = BleMode.SERVICE_DISCOVERY;
                setAdapter();
            } else {//in service discovery mode
                disconnect();
            }
        } else {
            unregisterReceiver(mPairingRequestRecevier);
            finish();
        }
    }

    @Override
    public void onClick(View v) {
        if(v.getId() == R.id.buttonConnect){
            String txt = buttonConnect.getText().toString();
            if(txt.equalsIgnoreCase("Connect")){
                connect();
            } else{
                disconnect();
            }
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if(blemode == BleMode.SERVICE_DISCOVERY){
            blemode = BleMode.CHARACTERISTCS_DISCOVERY;
            selectedGattService = (BluetoothGattService)serviceAdaper.getItem(position);
            List<BluetoothGattCharacteristic> characteristics = bleService.getCharacteristicsForService(selectedGattService);
            characteristicsAdapter.clear();
            characteristicsAdapter.addCharacteristic(characteristics);
            setAdapter();
        }
    }

    private final BroadcastReceiver mPairingRequestRecevier = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            if (BluetoothDevice.ACTION_PAIRING_REQUEST.equals(intent.getAction()))
            {
                final BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                int type = intent.getIntExtra(BluetoothDevice.EXTRA_PAIRING_VARIANT, BluetoothDevice.ERROR);

                if (type == BluetoothDevice.PAIRING_VARIANT_PIN)
                {
                    byte[] pinBytes = getIntToByte(000016);
                    device.setPin(pinBytes);
                    abortBroadcast();
                }
                else
                {
                    showToast("Unexpected pairing type: " + type);
                }
            } else if(BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(intent.getAction())){
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                switch (device.getBondState()) {
                    case BluetoothDevice.BOND_BONDING:
                        showToast(device.getName() +" is bonding");
                        break;
                    case BluetoothDevice.BOND_BONDED:
                        showToast(device.getName() +" Bonded...");
                        break;
                    case BluetoothDevice.BOND_NONE:
                        showToast(device.getName() +" Bonding failed...");
                    default:
                        break;
                }
            }
        }

        public byte[] getIntToByte(int value){
            ByteBuffer b = ByteBuffer.allocate(4);
            //b.order(ByteOrder.BIG_ENDIAN); // optional, the initial order of a byte buffer is always BIG_ENDIAN.
            b.putInt(value);
            byte[] result = b.array();
            return result;
        }
    };


}
