package com.example.pxshl.yc_monitior.adapter;

import android.content.Context;
import android.content.DialogInterface;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

import com.example.pxshl.yc_monitior.R;
import com.example.pxshl.yc_monitior.activity.WifiActivity;
import com.example.pxshl.yc_monitior.widget.IconTextView;

import java.util.List;


/**
 * wifiActivity界面对应的适配器
 */

public class WifiAdapter extends RecyclerView.Adapter {

    private Context mContext;

    private List<ScanResult> mDataList;
    private LayoutInflater mLayoutInflater;


    public WifiAdapter(Context context, List<ScanResult> dataList) {
        this.mContext = context;
        this.mDataList = dataList;
        this.mLayoutInflater = LayoutInflater.from(context);
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = mLayoutInflater.inflate(R.layout.wifi_list_item, parent, false);
        return new MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, final int position) {
        if (holder instanceof MyViewHolder) {
            MyViewHolder viewHolder = (MyViewHolder) holder;
            ScanResult result = mDataList.get(position);
            //设置wifi名称
            if (result.SSID.equals("")) { //针对那些把SSID隐藏的wifi设备
                viewHolder.wifiName.setText("隐藏的wifi");
            } else {
                viewHolder.wifiName.setText(result.SSID);
            }

            //设置是否显示加密图标
            if (result.capabilities.contains("WEP") || result.capabilities.contains("WPA")) {
                viewHolder.ivNeedCode.setVisibility(View.VISIBLE);
            } else {
                viewHolder.ivNeedCode.setVisibility(View.INVISIBLE);
            }
            //设置item的点击事件
            viewHolder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    final EditText editText = new EditText(mContext);
                    new AlertDialog.Builder(mContext).setTitle("请输入 " + mDataList.get(position).SSID + " 的密码").setView(editText)
                            .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    String msg = mDataList.get(position).SSID + " " + editText.getText().toString();
                                    ((WifiActivity) (mContext)).sendToMonitor(msg); //发送密码
                                }
                            })
                            .setNegativeButton("取消", null).show();
                }
            });

            //设置显示的信号强度
            switch (WifiManager.calculateSignalLevel(result.level, 5)) {
                case 0:
                    viewHolder.ivIntensity.setText(R.string.icon_signal_off);
                    break;
                case 1:
                    viewHolder.ivIntensity.setText(R.string.icon_signal_one);
                    break;
                case 2:
                    viewHolder.ivIntensity.setText(R.string.icon_signal_two);
                    break;
                case 3:
                    viewHolder.ivIntensity.setText(R.string.icon_signal_three);
                    break;
                case 4:
                    viewHolder.ivIntensity.setText(R.string.icon_signal_four);
                    break;
                default:
                    break;
            }
        }
    }

    @Override
    public int getItemCount() {
        return mDataList.size();
    }

    /**
     * viewholder
     */
    class MyViewHolder extends RecyclerView.ViewHolder {

        IconTextView ivIntensity; //信号强度
        TextView wifiName;  //wifi名称
        IconTextView ivNeedCode;    //是否加密

        public MyViewHolder(View itemView) {
            super(itemView);
            ivIntensity = (IconTextView) itemView.findViewById(R.id.ivIntensity);
            wifiName = (TextView) itemView.findViewById(R.id.tvWifiName);
            ivNeedCode = (IconTextView) itemView.findViewById(R.id.ivNeedCode);
        }
    }

}
