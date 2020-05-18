package com.videocomm.queue4android.main;

import android.Manifest;
import android.app.Activity;
import android.content.*;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.RadioGroup.OnCheckedChangeListener;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;

import com.videocomm.mediasdk.VComMediaSDK;
import com.videocomm.mediasdk.VComSDKDefine;
import com.videocomm.mediasdk.VComSDKEvent;
import com.videocomm.queue4android.R;
import com.videocomm.queue4android.bean.QueueBean;
import com.videocomm.queue4android.common.BaseMethod;
import com.videocomm.queue4android.common.CustomApplication;
import com.videocomm.queue4android.common.JsonUtil;
import com.videocomm.queue4android.common.PermissionUtil;
import com.videocomm.queue4android.common.ScreenInfo;
import com.videocomm.queue4android.common.ValueUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static com.videocomm.mediasdk.VComSDKDefine.VCOM_QUEUECTRL_QUERYQUEUEINFO;
import static com.videocomm.mediasdk.VComSDKDefine.VCOM_QUEUECTRL_QUREYQUEUELENGTH;
import static com.videocomm.mediasdk.VComSDKDefine.VCOM_QUEUEEVENT_AGENTSERVICE;
import static com.videocomm.mediasdk.VComSDKDefine.VCOM_QUEUEEVENT_ENTERRESULT;
import static com.videocomm.mediasdk.VComSDKDefine.VCOM_QUEUEEVENT_LEAVERESULT;
import static com.videocomm.mediasdk.VComSDKDefine.VCOM_QUEUEEVENT_QUERYQUEUEINFO;

public class LoginActivity extends Activity implements VComSDKEvent {

    /**
     * app需要用到的动态添加权限 6.0以上才会去申请
     */
    String[] permissions = new String[]{Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE};

    private static long time;//记录当前时间
    private int mSPort = 8080; // 端口
    private String mStrName = "Tom"; // 自定义
    private String mStrIP = "139.9.171.70"; // 默认ip
    private String tag = getClass().getSimpleName();
    private final int SHOWLOGINSTATEFLAG = 1; // 显示的按钮是登陆状态的标识
    private final int ACTIVITY_ID_MAINUI = 1; // MainActivity的id标致，onActivityResult返回
    private final int SHOWWAITINGSTATEFLAG = 2; // 显示的按钮是等待状态的标识

    private EditText mEditIP; // ip
    private EditText mEditPort; // 端口
    private EditText mEditName; // 用户名
    private Button mBtnStart; // 开始登录
    private Button mBtnWaiting; // 登陆等待状态
    private Toast mToast;
    private TextView mBottomConnMsg; // 连接服务器状态
    private TextView mBottomBuildMsg; // 版本编译信息
    private LinearLayout mWaitingLayout; // 登录加载层
    private LinearLayout mProgressLayout; // 加载动画层
    private CustomApplication mCustomApplication; // 存储全局变量

    private VComMediaSDK mVComMediaSDK;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.activity_main);

        setDisPlayMetrics();
        mCustomApplication = (CustomApplication) getApplication();

        InitSDK();
        InitLayout();
        // 读取登陆配置表
        readLoginDate();
        // 初始化登陆配置数据
        initLoginConfig();
        initWaitingTips();
        // 注册广播
        registerBoradcastReceiver();
    }

    // 初始化SDK
    private void InitSDK() {
        if (mVComMediaSDK == null) {
            mVComMediaSDK = VComMediaSDK.GetInstance();
        }
        mVComMediaSDK.VCOM_Initialize(0, "", getApplicationContext());//Content最好传 getApplicationContext
        mVComMediaSDK.SetSDKEvent(this);
    }

    // 初始化布局层
    private void InitLayout() {
        mEditIP = (EditText) this.findViewById(R.id.mainUIEditIP);
        mEditPort = (EditText) this.findViewById(R.id.mainUIEditPort);
        mEditName = (EditText) this.findViewById(R.id.main_et_name);
        mBottomConnMsg = (TextView) this.findViewById(R.id.mainUIbottomConnMsg);
        mBottomBuildMsg = (TextView) this.findViewById(R.id.mainUIbottomBuildMsg);
        mBtnStart = (Button) this.findViewById(R.id.mainUIStartBtn);
        mBtnWaiting = (Button) this.findViewById(R.id.mainUIWaitingBtn);
        mWaitingLayout = (LinearLayout) this.findViewById(R.id.waitingLayout);

        // 初始化bottom_tips信息
        mBottomBuildMsg.setText(JsonUtil.jsonToStr(mVComMediaSDK.VCOM_GetSDKVersion(), "version"));
        mBottomBuildMsg.setGravity(Gravity.CENTER_HORIZONTAL);
        mBtnStart.setOnClickListener(OnClickListener);
    }

    OnClickListener OnClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                // 登录
                case R.id.mainUIStartBtn:
                    //请求权限 同意才能下一步
                    if (PermissionUtil.checkPermission(LoginActivity.this, permissions)) {
                        startLogin();
                    }
                    break;
                default:
                    break;
            }
        }
    };

    private void startLogin() {
        mStrName = mEditName.getText().toString().trim();
        mStrIP = mEditIP.getText().toString().trim();
        mSPort = Integer.parseInt(mEditPort.getText().toString().trim());

        if (checkInputData()) {
            setBtnVisible(SHOWWAITINGSTATEFLAG);

            Random random = new Random();
            int num = random.nextInt(900) + 100;

            mVComMediaSDK.VCOM_SetUserConfig(mStrName + num, mStrName + num, "", "", "");
            int result = mVComMediaSDK.VCOM_Login(mStrIP + ":" + mSPort, 0, "");
            if (result != 0) {
                mBottomConnMsg.setText("登录失败" + result);
            }
        }
    }

    // 设置默认数据
    private void initLoginConfig() {
        mEditIP.setText(mStrIP);
        mEditName.setText(mStrName);
        mEditPort.setText(String.valueOf(mSPort));
    }

    // 读取登陆数据
    private void readLoginDate() {
        SharedPreferences preferences = getSharedPreferences("LoginInfo", 0);
        mStrIP = preferences.getString("UserIP", "139.9.171.70");
        mStrName = preferences.getString("UserName", "Android");
        mSPort = preferences.getInt("UserPort", 8080);
    }

    // 保存登陆相关数据
    private void saveLoginData() {
        SharedPreferences preferences = getSharedPreferences("LoginInfo", 0);
        Editor preferencesEditor = preferences.edit();
        preferencesEditor.putString("UserIP", mStrIP);
        preferencesEditor.putString("UserName", mStrName);
        preferencesEditor.putInt("UserPort", mSPort);
        preferencesEditor.commit();
    }

    // 判断ip、端口和姓名是否是空
    private boolean checkInputData() {
        String ip = mEditIP.getText().toString().trim();
        String port = mEditPort.getText().toString().trim();
        String name = mEditName.getText().toString().trim();

        if (ValueUtils.isStrEmpty(ip)) {
            mBottomConnMsg.setText("请输入IP");
            return false;
        } else if (ValueUtils.isStrEmpty(port)) {
            mBottomConnMsg.setText("请输入端口号");
            return false;
        } else if (ValueUtils.isStrEmpty(name)) {
            mBottomConnMsg.setText("请输入姓名");
            return false;
        }
        return true;
    }

    // 控制登陆，等待和登出按钮状态
    private void setBtnVisible(int index) {
        if (index == SHOWLOGINSTATEFLAG) {
            mBtnStart.setVisibility(View.VISIBLE);
            mBtnWaiting.setVisibility(View.GONE);

            mProgressLayout.setVisibility(View.GONE);
        } else if (index == SHOWWAITINGSTATEFLAG) {
            mBtnStart.setVisibility(View.GONE);
            mBtnWaiting.setVisibility(View.GONE);

            mProgressLayout.setVisibility(View.VISIBLE);
        }
    }

    // init登陆等待状态UI
    private void initWaitingTips() {
        if (mProgressLayout == null) {
            mProgressLayout = new LinearLayout(this);
            mProgressLayout.setOrientation(LinearLayout.HORIZONTAL);
            mProgressLayout.setGravity(Gravity.CENTER_VERTICAL);
            mProgressLayout.setPadding(1, 1, 1, 1);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT,
                    LayoutParams.WRAP_CONTENT);
            params.setMargins(5, 5, 5, 5);
            ProgressBar progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleLarge);
            mProgressLayout.addView(progressBar, params);
            mProgressLayout.setVisibility(View.GONE);
            mWaitingLayout.addView(mProgressLayout, new LayoutParams(params));
        }
    }

    private void setDisPlayMetrics() {
        DisplayMetrics dMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dMetrics);
        ScreenInfo.WIDTH = dMetrics.widthPixels;
        ScreenInfo.HEIGHT = dMetrics.heightPixels;
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    protected void onDestroy() {
        mVComMediaSDK.RemoveSDKEvent(this);
        mVComMediaSDK.VCOM_Release();
        super.onDestroy();

        unregisterReceiver(mBroadcastReceiver);
    }

    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        mToast = null;
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && requestCode == ACTIVITY_ID_MAINUI) {
            setBtnVisible(SHOWLOGINSTATEFLAG);
        }
    }

    // 广播
    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals("NetworkDiscon")) {
                setBtnVisible(SHOWLOGINSTATEFLAG);
                mBottomConnMsg.setText("Failed to connect to the Server");
                if (mToast == null) {
                    mToast = Toast.makeText(LoginActivity.this, "网络已断开!", Toast.LENGTH_SHORT);
                    mToast.show();
                }
            }
        }
    };

    public void registerBoradcastReceiver() {
        IntentFilter myIntentFilter = new IntentFilter();
        myIntentFilter.addAction("NetworkDiscon");
        // 注册广播
        registerReceiver(mBroadcastReceiver, myIntentFilter);
    }

    /**
     * 登陆通知
     */
    @Override
    public void OnLoginSystem(String lpUserCode, int iErrorCode, int iReConnect) {

        Log.i(tag, "OnLoginSystem--lpUserCode:" + lpUserCode + "--iErrorCode:" + iErrorCode + "--iReConnect" + iReConnect);
        if (iErrorCode == 0) {

            saveLoginData();

            // 保存用户id和用户角色信息
            mCustomApplication.setSelfUserName(lpUserCode);
            mBottomConnMsg.setText("Connect to the server success.");

            setBtnVisible(SHOWLOGINSTATEFLAG);

            Intent in = new Intent();
            in.setClass(LoginActivity.this, YeWuActivity.class);
            startActivity(in);

        } else {
            setBtnVisible(SHOWLOGINSTATEFLAG);
            mBottomConnMsg.setText("登录失败，errorCode：" + iErrorCode);
        }
    }

    /**
     * 连接断开通知
     */
    @Override
    public void OnDisconnect(int iErrorCode) {
        Log.i(tag, "OnDisconnect--iErrorCode:" + iErrorCode);
    }

    /**
     * 服务器踢人通知
     */
    @Override
    public void OnServerKickout(int iErrorCode) {
        Log.i(tag, "OnServerKickout--iErrorCode:" + iErrorCode);
        BaseMethod.showToast(getString(R.string.str_againlogin), LoginActivity.this);
    }

    /**
     * 进出会议室通知
     */
    @Override
    public void OnConferenceResult(int iAction, String lpConfId, int iErrorCode) {
        Log.i(tag, "OnConferenceResult--iAction:" + iAction + "--lpConfId:" + lpConfId + "--iErrorCode" + iErrorCode);
    }

    /**
     * 其他用户进出会议室通知
     */
    @Override
    public void OnConferenceUser(String lpUserCode, int iAction, String lpConfId) {
        Log.i(tag, "OnConferenceUser--lpUserCode:" + lpUserCode + "--iAction:" + iAction + "--lpConfId" + lpConfId);
    }

    @Override
    public void OnRemoteVideoData(String lpUserCode, int iChannelIndex, int iFrameType, long i64Timestamp, byte[] lpBuf, int iSizeInByte, int iWidth, int iHeight, int iFlags, int iRotation) {
        Log.i(tag, "OnRemoteVideoData--lpUserCode:" + lpUserCode + "--iChannelIndex:" + iChannelIndex + "--iFrameType" + iFrameType);
        Log.i(tag, "OnRemoteVideoData--i64Timestamp:" + i64Timestamp + "--lpBuf:" + lpBuf + "--iSizeInByte" + iSizeInByte);
        Log.i(tag, "OnRemoteVideoData--iWidth:" + iWidth + "--iHeight:" + iHeight + "--iFlags" + iFlags + "--iRotation:" + iRotation);
    }

    @Override
    public void OnRemoteAudioData(String lpUserCode, int iChannelIndex, long i64Timestamp, byte[] lpBuf, int iSizeInByte, int iFlags) {
        Log.i(tag, "OnRemoteAudioData--lpUserCode:" + lpUserCode + "--iChannelIndex:" + iChannelIndex + "--i64Timestamp" + i64Timestamp);
        Log.i(tag, "OnRemoteAudioData--lpBuf:" + lpBuf + "--iSizeInByte:" + iSizeInByte + "--iFlags" + iFlags);
    }

    @Override
    public void OnRecordResult(String lpUserCode, int iRecordId, int iErrorCode, String lpFileName, int iFileLength, int iDuration, String lpMD5, String lpBusinessParam) {
        Log.i(tag, "OnRecordResult--iRecordId:" + iRecordId + "--iErrorCode:" + iErrorCode + "--lpFileName" + lpFileName);
        Log.i(tag, "OnRecordResult--iFileLength:" + iFileLength + "--iDuration:" + iDuration + "--lpMD5" + lpMD5 + "--lpBusinessParam" + lpBusinessParam);
    }

    @Override
    public void OnSnapShotResult(String lpUserCode, int iChannelIndex, int iErrorCode, String lpFileName, String lpBusinessParam, String lpExtParam) {
        Log.i(tag, "OnSnapShotResult--lpUserCode:" + lpUserCode + "--iChannelIndex:" + iChannelIndex + "--iErrorCode" + iErrorCode + "--lpBusinessParam" + lpBusinessParam + "--lpExtParam" + lpExtParam);
    }

    @Override
    public void OnSendFileStatus(int iHandle, int iErrorCode, int iProgress, String lpFileName, long iFileLength, int iFlags, String lpParam) {
        Log.i(tag, "OnSendFileStatus--iHandle:" + iHandle + "--iErrorCode:" + iErrorCode + "--iProgress" + iProgress + "--iFileLength" + iFileLength + "--iFlags" + iFlags + "--lpParam" + lpParam);
    }

    @Override
    public void OnReceiveMessage(String lpUserCode, int iMsgType, String lpMessage) {
        Log.i(tag, "OnReceiveMessage--lpUserCode:" + lpUserCode + "--iMsgType:" + iMsgType + "--lpMessage" + lpMessage);
    }

    // 发送消息回调（用于发送回执）
    @Override
    public void OnSendMessage(int iMsgId, int iErrorCode) {

    }

    // 媒体文件控制回调
    @Override
    public void OnMediaFileControlEvent(int iMediaFileId, int iEventType, int iParam, String lpParam) {

    }

    // 媒体资源回调
    @Override
    public void OnMediaResourceResult(String lpResourceGuid, int iErrorCode, int iResourceType, String lpBusinessParam, String lpMd5, String lpUserData) {

    }

    //排队回调
    @Override
    public void OnQueueEvent(int iEventType, int iErrorCode, String lpUserData) {
        Log.i(tag, "OnQueueEvent--iEventType:" + iEventType + "--iErrorCode:" + iErrorCode + "--lpUserData" + lpUserData);

    }

    @Override
    public void OnAIAbilityEvent(int i, int i1, String s) {

    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PermissionUtil.REQUEST_PERMISSION_CODR
                && grantResults.length == this.permissions.length
                && grantResults[0] == PERMISSION_GRANTED
                && grantResults[1] == PERMISSION_GRANTED
                && grantResults[2] == PERMISSION_GRANTED
                && grantResults[3] == PERMISSION_GRANTED) {
            //全部请求成功后 不再走这里
            startLogin();

        } else {
            //某个权限拒绝
            for (int i = 0; i < permissions.length; i++) {
                Log.i("onRequestPermission", "授权的权限: " + permissions[i]);
                Log.i("onRequestPermission", "授权的结果: " + grantResults[i]);
                //判断用户拒绝权限是是否勾选don't ask again选项，若勾选需要客户手动打开权限
                if (!ActivityCompat.shouldShowRequestPermissionRationale(LoginActivity.this, permissions[i])) {
                    BaseMethod.showToast("请打开相应的权限，防止影响体验", LoginActivity.this);

                }
            }
        }
    }

    @Override
    public void onBackPressed() {
        Log.i(tag, "onBackPressed");
        long currentTime = System.currentTimeMillis();
        if (currentTime - time > 2000) {
            time = currentTime;
            if (mToast == null) {
                mToast = Toast.makeText(LoginActivity.this, "再按一次退出!", Toast.LENGTH_SHORT);
                mToast.show();
            }
            return;
        }
        mToast.cancel();
        super.onBackPressed();
    }
}