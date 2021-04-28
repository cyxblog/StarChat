package com.example.starchat.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.starchat.R;
import com.example.starchat.bean.DeviceBean;

import java.util.List;

/**
 * 设备信息适配器
 */
public class DeviceAdapter extends BaseAdapter {

    private Context mContext;
    private List<DeviceBean> mListDevice;

    public DeviceAdapter(List<DeviceBean> listDevice, Context context){
        mListDevice = listDevice;
        mContext = context;
    }

    @Override
    public int getCount() {
        return mListDevice.size();
    }

    @Override
    public Object getItem(int position) {
        return mListDevice.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = LayoutInflater.from(mContext);
        ViewHolder holder;
        if(convertView==null){
            convertView = inflater.inflate(R.layout.device_list_item,null);
            holder = new ViewHolder();
            holder.textDeviceName = convertView.findViewById(R.id.text_device_name);
            holder.textDeviceStatus = convertView.findViewById(R.id.text_device_status);
            holder.imgDeviceType = convertView.findViewById(R.id.img_device_type);
            convertView.setTag(holder);
        }else{
            holder = (ViewHolder) convertView.getTag();
        }
        holder.textDeviceName.setText(mListDevice.get(position).deviceName);
        holder.textDeviceStatus.setText(mListDevice.get(position).deviceStatus);
        holder.imgDeviceType.setImageResource(mListDevice.get(position).deviceType);
        return convertView;
    }

    public class ViewHolder{
        private ImageView imgDeviceType;
        private TextView textDeviceName;
        private TextView textDeviceStatus;
    }
}
