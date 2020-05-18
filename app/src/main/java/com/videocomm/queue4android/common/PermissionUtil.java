package com.videocomm.queue4android.common;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

/**
 * @author[wengCJ]
 * @version[创建日期，2019/12/27 0027]
 * @function[功能简介 关于权限申请的工具类]
 **/
public class PermissionUtil {

    public static final int REQUEST_PERMISSION_CODR = 10086;

    /**
     * 检查权限是否拥有 没有则去申请
     */
    public static boolean checkPermission(Activity activity, String[] permissions) {

        if (Build.VERSION.SDK_INT >= 23) {//6.0才用动态权限
            for (String permission : permissions) {
                //检查是否授予权限 返回的结果为PackageManager.PERMISSION_GRANTED（0）表示授予权限
                if (ContextCompat.checkSelfPermission(activity,
                        permission) != PackageManager.PERMISSION_GRANTED) {
                    //请求权限
                    ActivityCompat.requestPermissions(activity,
                            permissions,
                            REQUEST_PERMISSION_CODR);
                    return false;
                }
            }
        }
        return true;
    }
}
