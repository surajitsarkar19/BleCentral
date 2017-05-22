package rocks.keyless.app.android.bleclient.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import java.nio.ByteBuffer;

import rocks.keyless.app.android.bleclient.ui.BleCentralActivity;

/**
 * Created by Surajit Sarkar on 12/5/17.
 * Company : Bitcanny Technologies Pvt. Ltd.
 * Email   : surajit@bitcanny.com
 */

public class BondingBroadcastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent)
    {
        if (BluetoothDevice.ACTION_PAIRING_REQUEST.equals(intent.getAction())) {
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
                Toast.makeText(context,"Unexpected pairing type: " + type,Toast.LENGTH_SHORT).show();
            }
        } else if(BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(intent.getAction())){
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            switch (device.getBondState()) {
                case BluetoothDevice.BOND_BONDING:
                    showToast(context,device.getName() +" is bonding");
                    break;
                case BluetoothDevice.BOND_BONDED:
                    showToast(context,device.getName() +" Bonded...");
                    break;
                case BluetoothDevice.BOND_NONE:
                    showToast(context,device.getName() +" Bonding failed...");
                default:
                    break;
            }
        }
    }

    private void showToast(Context context,String msg){
        Toast.makeText(context,msg,Toast.LENGTH_SHORT).show();
    }

    public byte[] getIntToByte(int value){
        ByteBuffer b = ByteBuffer.allocate(4);
        //b.order(ByteOrder.BIG_ENDIAN); // optional, the initial order of a byte buffer is always BIG_ENDIAN.
        b.putInt(value);
        byte[] result = b.array();
        return result;
    }
}
