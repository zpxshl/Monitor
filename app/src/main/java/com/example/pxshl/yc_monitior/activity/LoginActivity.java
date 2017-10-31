package com.example.pxshl.yc_monitior.activity;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import com.example.pxshl.yc_monitior.R;
import com.example.pxshl.yc_monitior.fragment.LoginFragment1;
import com.example.pxshl.yc_monitior.fragment.LoginFragment2;
import com.example.pxshl.yc_monitior.util.Data;


/**
 * 登陆使用的Activity
 */
public class LoginActivity extends AppCompatActivity {


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        changeFragment();
    }

    /**
     * 根据时候登陆，切换不同的界面
     */
    public void changeFragment(){
        FragmentManager manager = getFragmentManager();
        FragmentTransaction transaction = manager.beginTransaction();

        Fragment fragment;
        if (Data.isLogin == true){
            fragment = new LoginFragment2();  //退出账号，修改密码界面
        }else {
            fragment = new LoginFragment1();  //登陆界面
        }

        transaction.replace(R.id.activity_login,fragment);
        transaction.commitAllowingStateLoss();

    }

}
