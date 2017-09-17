package com.example.pxshl.yc_monitior.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.example.pxshl.yc_monitior.R;
import com.example.pxshl.yc_monitior.activity.LoginActivity;
import com.example.pxshl.yc_monitior.activity.WifiActivity;
import com.example.pxshl.yc_monitior.util.Data;


/**
 * Created by pxshl on 17-7-26.
 */

public class SettingsFragment extends Fragment {


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings,null);

        Button login = (Button) view.findViewById(R.id.login);
        Button setWifi = (Button) view.findViewById(R.id.setWifi);
        Button exit = (Button) view.findViewById(R.id.exit_btn);

        if (Data.isLogin){
            login.setText(Data.account + " 欢迎您");
        }

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

        exit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                System.exit(0);
            }
        });

        return view;
    }



}
