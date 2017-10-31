package com.example.pxshl.yc_monitior.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.widget.Toast;

import com.example.pxshl.yc_monitior.R;
import com.example.pxshl.yc_monitior.fragment.AlarmFragment;
import com.example.pxshl.yc_monitior.fragment.DownLoadFragment;
import com.example.pxshl.yc_monitior.fragment.LiveFragment;
import com.example.pxshl.yc_monitior.fragment.SettingsFragment;
import com.example.pxshl.yc_monitior.inyerface.RequestCallBack;
import com.example.pxshl.yc_monitior.net.tcp.TcpTool;
import com.example.pxshl.yc_monitior.util.Data;
import com.example.pxshl.yc_monitior.util.Tools;

import java.util.ArrayList;
import java.util.List;

/**
 * 主活动，申请各种运行时权限，并让Alarm，Live，Download，setting四个碎片依附
 * 通过Fragment+ViewPager+BottomNavigationView实现滑动与导航效果
 */
public class MainActivity extends AppCompatActivity{

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

        if (ContextCompat.checkSelfPermission(this, "android.permission.ACCESS_FINE_LOCATION") != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this,"android.permission.WRITE_EXTERNAL_STORAGE") != PackageManager.PERMISSION_GRANTED   ) {
            requestPermissions(new String[]{"android.permission.ACCESS_FINE_LOCATION","android.permission.WRITE_EXTERNAL_STORAGE"},0);

        }else{
            init();
        }
    }

    private void init() {

        if (!Data.isLogin){
            //判断是否已经登陆过（且记住密码）
            SharedPreferences preferences = getSharedPreferences("properties",MODE_PRIVATE);
            Data.isLogin = preferences.getBoolean("isLogin",false);
            Data.account = preferences.getString("account","");
            Data.password = preferences.getString("password","");
            Data.alarm_sensitivity = preferences.getInt("alarm_sensitivity",-1);

            if (!Data.isLogin){ //如果没登陆，跳转到登陆界面
                Intent intent = new Intent(this,LoginActivity.class);
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


    @Override
    public void onRequestPermissionsResult(int requestCode,  String[] permissions, int[] grantResults) {
        if (requestCode == 0) {
            if (grantResults[0] != PackageManager.PERMISSION_GRANTED || grantResults[1] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(getApplicationContext(), "请授予程序必要的权限", Toast.LENGTH_SHORT).show();
                finish();
            }else {
                init();
            }
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }


}
