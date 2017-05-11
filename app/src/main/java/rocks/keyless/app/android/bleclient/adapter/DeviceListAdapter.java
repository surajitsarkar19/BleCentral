package rocks.keyless.app.android.bleclient.adapter;

import android.bluetooth.BluetoothDevice;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;

import rocks.keyless.app.android.bleclient.R;

/**
 * Created by Surajit Sarkar on 11/5/17.
 * Company : Bitcanny Technologies Pvt. Ltd.
 * Email   : surajit@bitcanny.com
 */

public class DeviceListAdapter extends BaseAdapter{
    private ArrayList<BluetoothDevice> bleDevices;

    public DeviceListAdapter() {
        super();
        bleDevices = new ArrayList<BluetoothDevice>();
    }

    public void addDevice(BluetoothDevice device) {
        if (!bleDevices.contains(device)) {
            bleDevices.add(device);
        }
    }

    public boolean contains(BluetoothDevice device) {
        return bleDevices.contains(device);
    }

    public BluetoothDevice getDevice(int position) {
        return bleDevices.get(position);
    }

    public void clear() {
        bleDevices.clear();
    }

    @Override
    public int getCount() {
        return bleDevices.size();
    }

    @Override
    public Object getItem(int i) {
        return bleDevices.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        ViewHolder viewHolder;
        if (view == null) {
            view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.list_row, null);
            viewHolder = new ViewHolder();
            viewHolder.text = (TextView) view.findViewById(R.id.textView);
            viewHolder.bdaddr = (TextView) view.findViewById(R.id.bdaddr);
            view.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) view.getTag();
        }
        BluetoothDevice device = bleDevices.get(i);
        String deviceName = device.getName();
        if (deviceName != null && deviceName.length() > 0) {
            viewHolder.text.setText(deviceName);
        } else {
            viewHolder.text.setText("unknown device");
        }
        viewHolder.bdaddr.setText(device.getAddress());
        return view;
    }

    static class ViewHolder {
        public TextView text;
        public TextView bdaddr;
    }
}
