package com.example.pxshl.yc_monitior.activity;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import com.example.pxshl.yc_monitior.R;
import com.example.pxshl.yc_monitior.fragment.AlarmFragment;
import com.example.pxshl.yc_monitior.fragment.DownLoadFragment;
import com.example.pxshl.yc_monitior.fragment.LiveFragment;
import com.example.pxshl.yc_monitior.fragment.SettingsFragment;

import com.example.pxshl.yc_monitior.util.Data;
import com.tbruyelle.rxpermissions2.RxPermissions;


import java.util.ArrayList;
import java.util.List;

import io.reactivex.functions.Consumer;


/**
 * 主活动，申请各种运行时权限，并让Alarm，Live，Download，setting四个碎片依附
 * 通过Fragment+ViewPager+BottomNavigationView实现滑动与导航效果
 */
public class MainActivity extends AppCompatActivity {

    private List<Fragment> mList;
    private ViewPager mViewPager;
    private BottomNavigationView mNavigation;

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {

            mViewPager.setCurrentItem(item.getOrder());
            return true;
        }

    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        requestPermissions();
    }

    private void requestPermissions() {


        RxPermissions rxPermissions = new RxPermissions(this);
        rxPermissions.request(Manifest.permission.WRITE_EXTERNAL_STORAGE).subscribe(new Consumer<Boolean>() {
            @Override
            public void accept(Boolean aBoolean) throws Exception {
                if (aBoolean) {
                    init();
                } else {
                    Toast.makeText(MainActivity.this, "请授予程序访问储存空间的权限（下载监控视频时需要）", Toast.LENGTH_LONG).show();
                    finish();
                }
            }
        });

    }


    private void init() {



        if (!Data.isLogin) {
            //判断是否已经登陆过（且记住密码）
            SharedPreferences preferences = getSharedPreferences("properties", MODE_PRIVATE);
            Data.isLogin = preferences.getBoolean("isLogin", false);
            Data.account = preferences.getString("account", "");
            Data.password = preferences.getString("password", "");
            //   Data.alarm_sensitivity = preferences.getInt("alarm_sensitivity",-1);
            Data.phone_num = preferences.getString("phone_num", "");

            Data.BSSID = preferences.getString("bssid","");
            Log.e("bssid",Data.BSSID);


            if (!Data.isLogin) { //如果没登陆，跳转到登陆界面
                Intent intent = new Intent(this, LoginActivity.class);
                startActivity(intent);
                finish();
                return;
            }
        }


        mNavigation = (BottomNavigationView) findViewById(R.id.navigation);

        mNavigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);
        mViewPager = (ViewPager) findViewById(R.id.viewPager);
        mList = new ArrayList<>();

        mList.add(new LiveFragment());
        mList.add(new DownLoadFragment());
        mList.add(new AlarmFragment());
        mList.add(new SettingsFragment());


        mViewPager.setAdapter(new FragmentPagerAdapter(getSupportFragmentManager()) {
            @Override
            public Fragment getItem(int position) {
                return mList.get(position);
            }

            @Override
            public int getCount() {
                return mList.size();
            }
        });

        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {

                mNavigation.getMenu().getItem(position).setChecked(true);
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });


    }


}
