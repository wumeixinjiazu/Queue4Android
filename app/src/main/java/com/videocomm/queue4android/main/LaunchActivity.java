package com.videocomm.queue4android.main;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import androidx.annotation.Nullable;

/**
 * @author[wengCJ]
 * @version[创建日期，2020/3/28 0028]
 * @function[功能简介 App启动页]
 **/
public class LaunchActivity extends Activity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!isTaskRoot()){
            finish();
            return;
        }

        new Handler().postDelayed(() -> {
            startActivity(new Intent(LaunchActivity.this, LoginActivity.class));
            finish();
        },0);
    }
}
