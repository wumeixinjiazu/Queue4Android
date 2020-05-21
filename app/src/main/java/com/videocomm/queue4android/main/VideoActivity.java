package com.videocomm.queue4android.main;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.videocomm.mediasdk.VComMediaSDK;
import com.videocomm.mediasdk.VComSDKEvent;
import com.videocomm.queue4android.R;
import com.videocomm.queue4android.common.BaseMethod;
import com.videocomm.queue4android.common.ConfigEntity;
import com.videocomm.queue4android.common.ConfigService;
import com.videocomm.queue4android.common.CustomApplication;
import com.videocomm.queue4android.common.DialogFactory;

import java.util.Timer;
import java.util.TimerTask;

import static com.videocomm.mediasdk.VComSDKDefine.VCOM_CONFERENCE_ACTIONCODE_EXIT;
import static com.videocomm.mediasdk.VComSDKDefine.VCOM_CONFERENCE_ACTIONCODE_JOIN;
import static com.videocomm.mediasdk.VComSDKDefine.VCOM_QUEUECTRL_HANGUPVIDEO;
import static com.videocomm.mediasdk.VComSDKDefine.VCOM_QUEUEEVENT_HANGUPVIDEO;
import static com.videocomm.mediasdk.VComSDKDefine.VCOM_SDK_PARAM_TYPE_WRITELOG;
import static com.videocomm.queue4android.common.ConfigEntity.mIntLocalAudioClose;
import static com.videocomm.queue4android.common.ConfigEntity.mIntLocalAudioOpen;
import static com.videocomm.queue4android.common.ConfigEntity.mIntLocalChannelIndex;
import static com.videocomm.queue4android.common.ConfigEntity.mIntLocalVideoClose;
import static com.videocomm.queue4android.common.ConfigEntity.mIntLocalVideoOpen;
import static com.videocomm.queue4android.common.ConfigEntity.mIntRemoteAudioClose;
import static com.videocomm.queue4android.common.ConfigEntity.mIntRemoteAudioOpen;
import static com.videocomm.queue4android.common.ConfigEntity.mIntRemoteChannelIndex;
import static com.videocomm.queue4android.common.ConfigEntity.mIntRemoteVideoClose;
import static com.videocomm.queue4android.common.ConfigEntity.mIntRemoteVideoOpen;

public class VideoActivity extends Activity implements
        OnClickListener, OnTouchListener, VComSDKEvent {

    private String tag = getClass().getSimpleName();

    private SurfaceView mSurfaceSelf;
    private TextView mTxtTime;
    private Button mBtnEndSession;
    private LinearLayout llRemote;
    private Dialog dialog;

    private Handler mHandler;
    private Timer mTimerShowVideoTime;
    private TimerTask mTimerTask;
    private ConfigEntity configEntity;

    public static final int MSG_TIMEUPDATE = 2;
    public CustomApplication mApplication;
    int dwTargetUserId;
    int videocallSeconds = 0;
    private VComMediaSDK mVComMediaSDK;
    private boolean isConnectRemote = false;//记录是否连接远程成功
    private int MAX_PEOPLE = 1;//最大人数
    private int currentPeople = 0;//当前人数
    private LinearLayout llLocal;
    private SurfaceView mSurfaceRemote;
    private String targetUserName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //去掉标题栏；
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);
        //禁止熄灭屏幕
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        //初始化sdk
        initSdk();
    }

    /**
     * 初始化本地视频
     */
    private void initLocalVideo() {
        mVComMediaSDK.VCOM_SetVideoParamConfigure(0, 640, 480, 15, 450, 0);
        LocalMediaShow mLocalMediaShow = new LocalMediaShow(mSurfaceSelf, mApplication.getSelfUserName());
        mSurfaceSelf.getHolder().addCallback(mLocalMediaShow);

        mSurfaceSelf.setZOrderOnTop(true);
    }

    /**
     * 初始化远程视频
     */
    private void initRemoteVideo(String lpUserCode) {
        //防止超过人数(注意：坐席返回的会议ID不会重复 此处判断可以忽略)
        if (currentPeople < MAX_PEOPLE) {
            mSurfaceRemote = new SurfaceView(this);
            llRemote.addView(mSurfaceRemote);
            targetUserName = lpUserCode;
            Log.d(tag, "targetUserName" + targetUserName);
            RemoteMediaShow mRemoteMediaShow = new RemoteMediaShow(mSurfaceRemote, targetUserName);
            mSurfaceRemote.getHolder().addCallback(mRemoteMediaShow);

            isConnectRemote = true;
            currentPeople += 1;
        }
    }

    /**
     * 更新时间
     */
    private void updateTime() {

        mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                switch (msg.what) {
                    case MSG_TIMEUPDATE:
                        mTxtTime.setText(BaseMethod
                                .getTimeShowString(videocallSeconds++));
                        break;
                }
            }
        };
        initTimerShowTime();
    }

    @Override
    protected void onPause() {
        super.onPause();
        //关闭自己的音视频
        mVComMediaSDK.VCOM_CloseLocalMediaStream(mIntLocalChannelIndex, "");
        if (isConnectRemote) {
            //关闭其他其他用户的音视频
            mVComMediaSDK.VCOM_CloseRemoteMediaStream(targetUserName, mIntRemoteChannelIndex, "");
        }
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        //打开自己的音视频
        mVComMediaSDK.VCOM_OpenLocalMediaStream(mIntLocalChannelIndex, mIntLocalVideoOpen, mIntLocalAudioOpen, "");
        if (isConnectRemote) {
            //打开其他其他用户的音视频
            mVComMediaSDK.VCOM_GetRemoteMediaStream(targetUserName, mIntRemoteChannelIndex, mIntRemoteVideoOpen, mIntRemoteVideoOpen, "");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        com.videocomm.queue4android.main.BussinessCenter.mContext = this;
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        //挂断
        mVComMediaSDK.VCOM_QueueControl(VCOM_QUEUECTRL_HANGUPVIDEO, "");

        mVComMediaSDK.VCOM_LeaveConference();
        mVComMediaSDK.RemoveSDKEvent(this);
        mTimerShowVideoTime.cancel();
        mApplication.setTargetUserName("");//清空坐席的用户名
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }
        super.onDestroy();
    }

    private void initSdk() {
        String confid = getIntent().getStringExtra("confid");
        if (confid == null) {
            finish();
        }

        if (mVComMediaSDK == null) {
            mVComMediaSDK = VComMediaSDK.GetInstance();
        }
        mVComMediaSDK.SetSDKEvent(this);
        //加入会议
        mVComMediaSDK.VCOM_JoinConference(confid, "", "");
    }

    private void initTimerShowTime() {
        if (mTimerShowVideoTime == null)
            mTimerShowVideoTime = new Timer();
        mTimerTask = new TimerTask() {

            @Override
            public void run() {
                mHandler.sendEmptyMessage(MSG_TIMEUPDATE);
            }
        };
        mTimerShowVideoTime.schedule(mTimerTask, 100, 1000);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            alertDialog();
        }

        return super.onKeyDown(keyCode, event);
    }

    private void initView() {
        this.setContentView(R.layout.video_activity);

        mApplication = (CustomApplication) getApplication();
        dwTargetUserId = mApplication.getTargetUserId();

        mSurfaceSelf = (SurfaceView) findViewById(R.id.surface_local);
        llRemote = findViewById(R.id.ll_remote);
        llLocal = findViewById(R.id.ll_local);
        mTxtTime = (TextView) findViewById(R.id.txt_time);
        mBtnEndSession = (Button) findViewById(R.id.btn_endsession);
        mBtnEndSession.setOnClickListener(this);
        llRemote.setOnClickListener(this);
        llLocal.setOnClickListener(this);

        configEntity = ConfigService.LoadConfig(this);
        if (configEntity.videoOverlay != 0) {
            mSurfaceSelf.getHolder().setType(
                    SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        }
    }


    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_endsession:
                alertDialog();
                break;
            case R.id.ll_remote:
//                switchPreview(R.id.ll_remote);
                break;
            case R.id.ll_local:
//                switchPreview(R.id.ll_local);
                break;
            default:
                break;
        }


    }

    private void alertDialog() {

        dialog = DialogFactory.getDialog(DialogFactory.DIALOGID_ENDCALL, this, new OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
                BaseMethod.showToast("正在结束视频通话...", VideoActivity.this);
            }
        });
        dialog.show();
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        return false;
    }

    @Override
    public void OnLoginSystem(String lpUserCode, int iErrorCode, int iReConnect) {
        Log.i(tag, "OnLoginSystem--lpUserCode:" + lpUserCode + "--iErrorCode:" + iErrorCode + "--iReConnect" + iReConnect);
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
        BaseMethod.showToast(getString(R.string.str_againlogin), VideoActivity.this);
        startActivity(new Intent(VideoActivity.this, LoginActivity.class));
    }

    /**
     * 进出会议室通知
     */
    @Override
    public void OnConferenceResult(int iAction, String lpConfId, int iErrorCode) {
        Log.i(tag, "OnConferenceResult--iAction:" + iAction + "--lpConfId:" + lpConfId + "--iErrorCode" + iErrorCode);
        if (iAction == VCOM_CONFERENCE_ACTIONCODE_JOIN) {
            //自己加入会议成功后 开启本地流

            //初始化布局
            initView();
            //初始化视频
            initLocalVideo();
            //更新时间
            updateTime();
        }
    }

    /**
     * 其他用户进出会议室通知
     */
    @Override
    public void OnConferenceUser(String lpUserCode, int iAction, String lpConfId) {
        Log.i(tag, "OnConferenceUser--lpUserCode:" + lpUserCode + "--iAction:" + iAction + "--lpConfId" + lpConfId);
        if (iAction == VCOM_CONFERENCE_ACTIONCODE_EXIT) {
            currentPeople -= 1;
            BaseMethod.showToast(lpUserCode + "用户已退出", VideoActivity.this);
        } else if (iAction == VCOM_CONFERENCE_ACTIONCODE_JOIN) {
            //其他用户进来后 打开远程用户流
            initRemoteVideo(lpUserCode);
        }
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

    @Override
    public void OnQueueEvent(int iEventType, int iErrorCode, String lpUserData) {
        Log.i(tag, "OnQueueEvent--iEventType:" + iEventType + "--iErrorCode:" + iErrorCode + "--lpUserData" + lpUserData);
        if (iEventType == VCOM_QUEUEEVENT_HANGUPVIDEO) {
            //用户 坐席挂断
            BaseMethod.showToast("视频通话已结束...", VideoActivity.this);
            finish();
        }
    }

    @Override
    public void OnAIAbilityEvent(int i, int i1, String s) {

    }

    public class LocalMediaShow implements SurfaceHolder.Callback {
        private final SurfaceView mSurfaceBig;
        private final String strUserCode;

        public LocalMediaShow(SurfaceView mSurfaceBig, String strUserCode) {
            this.mSurfaceBig = mSurfaceBig;
            this.strUserCode = strUserCode;
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            System.out.println("local surfaceChanged");
        }

        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            System.out.println("local surfaceCreated");
            LocalMediaControl(strUserCode, mIntLocalVideoOpen, mIntLocalAudioOpen, mSurfaceBig);
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            System.out.println("local surfaceDestroyed");
        }
    }

    public class RemoteMediaShow implements SurfaceHolder.Callback {
        private final String strUserCode;
        private final SurfaceView mSurfaceSmall;

        public RemoteMediaShow(SurfaceView mSurfaceSmall, String strUserCode) {
            this.mSurfaceSmall = mSurfaceSmall;
            this.strUserCode = strUserCode;
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            System.out.println("remote surfaceChanged");
        }

        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            System.out.println("remote surfaceCreated");
            RemoteMediaControl(strUserCode, mIntRemoteVideoOpen, mIntRemoteAudioOpen, mSurfaceSmall);
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            System.out.println("remote surfaceDestroyed");
        }
    }

    private void LocalMediaControl(String strUserCode, int iVideo, int iAudio, SurfaceView mSurfaceBig) {
        if ((mIntLocalVideoClose == iVideo) && (mIntLocalAudioClose == iAudio)) {
            mVComMediaSDK.VCOM_SetSDKParamString(VCOM_SDK_PARAM_TYPE_WRITELOG, "Android--SwitchL_CloseLocalMediaStream--0");
            mVComMediaSDK.VCOM_CloseLocalMediaStream(mIntLocalChannelIndex, "");
            mVComMediaSDK.VCOM_SetSDKParamString(VCOM_SDK_PARAM_TYPE_WRITELOG, "Android--SwitchL_CloseLocalMediaStream--1");

        } else {
            if (iVideo == mIntLocalVideoOpen) {
                mVComMediaSDK.VCOM_SetSDKParamString(VCOM_SDK_PARAM_TYPE_WRITELOG, "Android--SwitchL_SetViewHolder-0");
                mVComMediaSDK.VCOM_SetViewHolder(strUserCode, mIntLocalChannelIndex, mSurfaceBig.getHolder());
                mVComMediaSDK.VCOM_SetSDKParamString(VCOM_SDK_PARAM_TYPE_WRITELOG, "Android--SwitchL_SetViewHolder--1");
            }
            mVComMediaSDK.VCOM_SetSDKParamString(VCOM_SDK_PARAM_TYPE_WRITELOG, "Android--SwitchL_OpenLocalMediaStream--0");
            mVComMediaSDK.VCOM_OpenLocalMediaStream(mIntLocalChannelIndex, iVideo, iAudio, "");
            mVComMediaSDK.VCOM_SetSDKParamString(VCOM_SDK_PARAM_TYPE_WRITELOG, "Android--SwitchL_OpenLocalMediaStream--1");
        }
    }

    private void RemoteMediaControl(String strUserCode, int iVideo, int iAudio, SurfaceView mSurfaceSmall) {
        mVComMediaSDK.VCOM_SetSDKParamString(VCOM_SDK_PARAM_TYPE_WRITELOG, "strUserCode" + strUserCode);

        if ((mIntRemoteVideoClose == iVideo) && (mIntRemoteAudioClose == iAudio)) {
            mVComMediaSDK.VCOM_SetSDKParamString(VCOM_SDK_PARAM_TYPE_WRITELOG, "Android--SwitchR_CloseRemoteMediaStream--0");
            mVComMediaSDK.VCOM_CloseRemoteMediaStream(strUserCode, mIntRemoteChannelIndex, "");
            mVComMediaSDK.VCOM_SetSDKParamString(VCOM_SDK_PARAM_TYPE_WRITELOG, "Android--SwitchR_CloseRemoteMediaStream--1");

        } else {
            if (iVideo == mIntRemoteVideoOpen) {
                mVComMediaSDK.VCOM_SetSDKParamString(VCOM_SDK_PARAM_TYPE_WRITELOG, "Android--SwitchR_SetViewHolder--0");
                mVComMediaSDK.VCOM_SetViewHolder(strUserCode, mIntRemoteChannelIndex, mSurfaceSmall.getHolder());
                mVComMediaSDK.VCOM_SetSDKParamString(VCOM_SDK_PARAM_TYPE_WRITELOG, "Android--SwitchR_SetViewHolder--1");
            }

            mVComMediaSDK.VCOM_SetSDKParamString(VCOM_SDK_PARAM_TYPE_WRITELOG, "Android--SwitchR_GetRemoteMediaStream--0");
            int remoteResult = mVComMediaSDK.VCOM_GetRemoteMediaStream(strUserCode, mIntRemoteChannelIndex, iVideo, iAudio, "");
            mVComMediaSDK.VCOM_SetSDKParamString(VCOM_SDK_PARAM_TYPE_WRITELOG, "Android--SwitchR_GetRemoteMediaStream--1");

            Log.i(tag, "remoteResult:" + remoteResult + "-userCode:" + strUserCode);
        }
    }

    private int mLargeViewId = R.id.ll_remote;

    /**
     * 视频切换
     *
     * @param iViewId 控件ID
     */
    private void switchPreview(int iViewId) {
        if (iViewId != mLargeViewId) {
            LinearLayout smallView = findViewById(iViewId);
            LinearLayout largeView = findViewById(mLargeViewId);
            if (smallView != null && largeView != null) {
                ViewGroup.LayoutParams smallParam = smallView.getLayoutParams();
                ViewGroup.LayoutParams largeParam = largeView.getLayoutParams();

                largeView.setLayoutParams(smallParam);
                largeView.setClickable(true);

                smallView.setLayoutParams(largeParam);
                smallView.setClickable(false);
                mLargeViewId = iViewId;

                if (mSurfaceRemote != null) {
//                    mSurfaceRemote.setZOrderOnTop(true);
                }

            }
        }
    }

}
