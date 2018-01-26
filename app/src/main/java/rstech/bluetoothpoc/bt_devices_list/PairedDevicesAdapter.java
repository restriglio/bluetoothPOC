package rstech.bluetoothpoc.bt_devices_list;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SwitchCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.Switch;
import android.widget.TextView;

import java.util.List;

import rstech.bluetoothpoc.R;

/**
 * Created by raulstriglio on 9/16/17.
 */

public class PairedDevicesAdapter extends RecyclerView.Adapter<PairedDevicesAdapter.MyViewHolder> {

    private LayoutInflater inflater;
    private Context context;
    private List<BluetoothDeviceObject> pairedDevices;
    private PairedDevicesCallback mPairedDevicesCallback;


    public PairedDevicesAdapter(Context context, List<BluetoothDeviceObject> pairedDevicesList, PairedDevicesCallback pairedDevicesCallback) {
        inflater = LayoutInflater.from(context);
        this.context = context;
        pairedDevices = pairedDevicesList;
        mPairedDevicesCallback = pairedDevicesCallback;
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.paired_devices_item, parent, false);
        MyViewHolder holder = new MyViewHolder(view);
        return holder;
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, final int position) {

        holder.tv_address_device.setText(pairedDevices.get(position).getAddress());
        holder.tv_name_device.setText(pairedDevices.get(position).getName());
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mPairedDevicesCallback.onDeviceClick(pairedDevices.get(position), position);
            }
        });
        holder.check_connected.setChecked(pairedDevices.get(position).isConnected());
    }

    public void setList(List<BluetoothDeviceObject> pairedDevicesList) {
        pairedDevices = pairedDevicesList;
    }

    @Override
    public int getItemCount() {
        return pairedDevices.size();
    }

    public void updateElement(boolean isConnected, int pos){
        pairedDevices.get(pos).setConnected(isConnected);
        notifyItemChanged(pos);
    }

    class MyViewHolder extends RecyclerView.ViewHolder {
        TextView tv_address_device;
        TextView tv_name_device;
        View item;
        CheckBox check_connected;

        public MyViewHolder(View itemView) {
            super(itemView);
            item = itemView;
            tv_address_device = (TextView) itemView.findViewById(R.id.tv_address_device);
            tv_name_device = (TextView) itemView.findViewById(R.id.tv_name_device);
            check_connected = (CheckBox) itemView.findViewById(R.id.check_connected);
        }
    }

    public interface PairedDevicesCallback {
        void onDeviceClick(BluetoothDeviceObject bluetoothDeviceObject, int position);
    }
}