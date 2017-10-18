package com.example.pxshl.yc_monitior.fragment;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Toast;

import com.example.pxshl.yc_monitior.R;
import com.example.pxshl.yc_monitior.activity.LoginActivity;
import com.example.pxshl.yc_monitior.activity.WifiActivity;
import com.example.pxshl.yc_monitior.inyerface.RequestCallBack;
import com.example.pxshl.yc_monitior.inyerface.SendCallBack;
import com.example.pxshl.yc_monitior.net.tcp.TcpTool;
import com.example.pxshl.yc_monitior.util.Data;
import com.example.pxshl.yc_monitior.util.Tools;

import static android.content.Context.MODE_PRIVATE;


/**
 * Created by pxshl on 17-7-26.
 */

public class SettingsFragment extends Fragment {

    private Button mSetAlarm;


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings,null);

        Button login = (Button) view.findViewById(R.id.login);
        Button setWifi = (Button) view.findViewById(R.id.setWifi);
        mSetAlarm = (Button) view.findViewById(R.id.setAlram);
        final SeekBar bar = (SeekBar) view.findViewById(R.id.alarm_sensitivity);


        login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), LoginActivity.class);
                startActivity(intent);
                getActivity().finish();
            }
        });

        setWifi.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), WifiActivity.class);
                startActivity(intent);
            }
        });

        mSetAlarm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bar.setVisibility(View.VISIBLE);
            }
        });

        bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                final int progress = seekBar.getProgress();



                new TcpTool(Data.SERVER_IP,Data.SERVER_PORT1).send(Data.SET_ALARM_SENSITIVITY + " " + Data.account_msg + " " + progress, new SendCallBack() {
                    @Override
                    public void onFinish() {
                        Data.alarm_sensitivity = progress;
                        SharedPreferences preferences = getContext().getSharedPreferences("properties", MODE_PRIVATE);
                        SharedPreferences.Editor editor = preferences.edit();
                        editor.putInt("alarm_sensitivity", progress);
                        editor.commit();
                        bar.setVisibility(View.INVISIBLE);
                        Toast.makeText(getContext(),"设置报警灵敏度: " + progress + " 成功",Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError() {
                        Toast.makeText(getContext(),"设置报警灵敏度失败～请检查网络连接",Toast.LENGTH_SHORT).show();
                    }
                });



            }
        });



        login.setText(Data.account + " 欢迎您");

        if (Data.alarm_sensitivity == -1){
           askSensitivity();
        } else if(Data.alarm_sensitivity == 0){
            mSetAlarm.setText("未开启报警");
        }else {
            mSetAlarm.setText("报警启动 灵敏度： " + Data.alarm_sensitivity + "%");
        }

        bar.setProgress(Data.alarm_sensitivity);


        return view;
    }


    //询问服务器，该用户之前设置的报警灵敏度
    private void askSensitivity() {
        new TcpTool(Data.SERVER_IP,Data.SERVER_PORT1).connect(Data.ASK_ALARM_SENSITIVITY + " "+ Data.account_msg, new RequestCallBack() {
            @Override
            public void onFinish(String response) {
                int sensitivity = -1;
                try {
                    sensitivity  = Integer.parseInt(response.trim());
                }catch (NumberFormatException e){
                    e.printStackTrace();
                    return;
                }

                //防止由于服务器返回错误的数据
                if (sensitivity >= 0 && Data.alarm_sensitivity <= 100){
                    Data.alarm_sensitivity = sensitivity;

                    SharedPreferences preferences = getContext().getSharedPreferences("properties", MODE_PRIVATE);
                    SharedPreferences.Editor editor = preferences.edit();
                    editor.putInt("alarm_sensitivity", sensitivity);
                    editor.commit();
                }else {
                    Log.e("settingFrag", "sensitivity =  " + sensitivity);
                    return;
                }


                Activity activity = getActivity();
                if (activity != null){
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if(Data.alarm_sensitivity == 0){
                                mSetAlarm.setText("未开启报警");
                            }else {
                                mSetAlarm.setText("报警启动 灵敏度： " + Data.alarm_sensitivity + "%");
                            }
                        }
                    });
                }
            }

            @Override
            public void onError() {

            }
        });
    }



}
