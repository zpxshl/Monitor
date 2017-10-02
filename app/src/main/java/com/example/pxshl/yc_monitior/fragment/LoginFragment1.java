package com.example.pxshl.yc_monitior.fragment;

import android.app.Activity;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Toast;


import com.example.pxshl.yc_monitior.R;
import com.example.pxshl.yc_monitior.activity.MainActivity;
import com.example.pxshl.yc_monitior.inyerface.RequestCallBack;
import com.example.pxshl.yc_monitior.net.tcp.TcpTool;
import com.example.pxshl.yc_monitior.util.Data;
import com.example.pxshl.yc_monitior.util.Tools;

import static android.content.Context.MODE_PRIVATE;

/**
 * Created by pxshl on 2017/7/28.
 */

public class LoginFragment1 extends Fragment{


    private String account; //帐号
    private String password; //密码
    private ProgressDialog pd;

    private Activity mActivity;



    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mActivity = getActivity();
    }

    @Override   //兼容低版本安卓系统
    public void onAttach(Activity activity){
        super.onAttach(activity);
        mActivity = getActivity();
    }


    @Override
    public void onDetach() {
        super.onDetach();
        mActivity = null;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_login1,null);

        final EditText accountEt = (EditText) view.findViewById(R.id.account_et);
        final EditText passwordEt = (EditText) view.findViewById(R.id.password_et);
        final CheckBox rembPass = (CheckBox) view.findViewById(R.id.remb_pass);

        final CheckBox showPass = (CheckBox) view.findViewById(R.id.show_pass);
        showPass.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (showPass.isChecked()) {
                    //设置为可见
                    passwordEt.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                } else {
                    //设置为不可见
                    passwordEt.setInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD |InputType.TYPE_CLASS_TEXT);
                }
            }
        });



        Button loginBtn = (Button) view.findViewById(R.id.login_btn);
        loginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String s= accountEt.getText().toString();
                if (!accountEt.getText().toString().equals("") && !passwordEt.getText().toString().equals("")) {
                    account = accountEt.getText().toString();
                    password = passwordEt.getText().toString();

                    pd = new ProgressDialog(mActivity);
                    pd.setTitle("验证用户");
                    pd.setMessage("正在验证帐号密码，请稍后......");
                    pd.show();

                    new TcpTool().connect(Data.LOGIN + " " +  account + " " + Tools.pwdToMd5(password),new RequestCallBack() {

                        String msg = "";

                        @Override
                        public void onFinish(String response) {
                            pd.cancel();
                            if (response.contains("true")) {
                                Data.isLogin = true;
                                Data.account = account;
                                Data.password = password;


                                if (rembPass.isChecked()) {
                                    //储存帐号密码
                                    SharedPreferences preferences = getContext().getSharedPreferences("properties", MODE_PRIVATE);
                                    SharedPreferences.Editor editor = preferences.edit();
                                    editor.putString("account", account);
                                    editor.putString("password", password);
                                    editor.putBoolean("isLogin", true);
                                    editor.commit();
                                }

                                runOnUIThreadToast(Data.account + " 欢迎您");

                                Intent intent = new Intent(mActivity, MainActivity.class);
                                startActivity(intent);
                                mActivity.finish();
                            } else {
                                runOnUIThreadToast("帐号或密码错误，请重新输入");
                            }
                        }
                        @Override
                        public void onError() {
                            runOnUIThreadToast("联网验证失败，请稍后重试");
                        }
                    });

                }else {
                    Toast.makeText(getContext(),"请输入完整的帐号和密码",Toast.LENGTH_SHORT).show();
                }
            }
        });

        return view;
    }

    private void runOnUIThreadToast(final String msg){
        if (mActivity == null){
            return;
        }else {
            mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getContext(),msg,Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

}
