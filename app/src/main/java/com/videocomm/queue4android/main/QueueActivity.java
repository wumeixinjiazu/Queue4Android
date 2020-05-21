package com.videocomm.queue4android.main;

import java.util.Timer;
import java.util.TimerTask;


import com.videocomm.mediasdk.VComMediaSDK;
import com.videocomm.mediasdk.VComSDKDefine;
import com.videocomm.mediasdk.VComSDKEvent;
import com.videocomm.queue4android.R;
import com.videocomm.queue4android.bean.QueueStateBean;
import com.videocomm.queue4android.common.BaseMethod;
import com.videocomm.queue4android.common.CustomApplication;
import com.videocomm.queue4android.common.DialogFactory;
import com.videocomm.queue4android.common.JsonUtil;

import android.app.Dialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.app.Activity;
import android.content.Intent;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import static com.videocomm.mediasdk.VComSDKDefine.VCOM_QUEUECTRL_LEAVEQUEUE;
import static com.videocomm.mediasdk.VComSDKDefine.VCOM_QUEUECTRL_QUREYQUEUELENGTH;
import static com.videocomm.mediasdk.VComSDKDefine.VCOM_QUEUEEVENT_AGENTSERVICE;
import static com.videocomm.mediasdk.VComSDKDefine.VCOM_QUEUEEVENT_HANGUPVIDEO;
import static com.videocomm.mediasdk.VComSDKDefine.VCOM_QUEUEEVENT_QUREYQUEUELENGTH;
import static com.videocomm.mediasdk.VComSDKDefine.VCOM_QUEUEEVENT_STARTVIDEO;

public class QueueActivity extends Activity implements VComSDKEvent {
    private Button queueButton;
    private TextView showTextView;
    private ImageButton mImgBtnReturn;
    private TextView mTitleName, timeshow;
    public CustomApplication mApplication;        //全局变量类
    private final int TIME_UPDATE = 291;        //Handler发送消息,队列人数的实时更新

    private VComMediaSDK mVComMediaSDK;
    private String tag = getClass().getSimpleName();
    private QueueStateBean queueStateBean;
    private Timer timer;
    private Handler mHandler;
    private Dialog dialog;
    private boolean isStartVideo = false;//记录是否开启视频通话
    private boolean isDoExit;//记录是否主动退出
    private String agentId;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //自定义标题栏
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_queue);
        //禁止熄灭屏幕
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        //初始化SDK
        initSdk();
        //初始化布局
        initView();
    }

    /**
     * 初始化布局
     */
    private void initView() {
        //全局变量类的对象初始化
        mApplication = (CustomApplication) getApplication();
        //标题栏的设置
        queueButton = findViewById(R.id.queue_btn);
        mImgBtnReturn = this.findViewById(R.id.returnImgBtn);
        mImgBtnReturn.setVisibility(View.INVISIBLE);
        mTitleName = this.findViewById(R.id.titleName);
        mTitleName.setText(mApplication.getselectBussiness() + "-排队等待中");
        showTextView = findViewById(R.id.queue_show);
        //实时更新显示时间
        timeshow = findViewById(R.id.queue_time);
        String targetUserName = mApplication.getTargetUserName();
        if (targetUserName.length() > 0) {
            //表示 用户还没进入队列之前 坐席已经示闲（等待用户）
            mTitleName.setText("正在呼叫坐席" + targetUserName);
            queueButton.setText(getString(R.string.finish_call));
            showTextView.setVisibility(View.GONE);
            timeshow.setVisibility(View.GONE);
        }

        mHandler = new Handler() {

            @Override
            public void handleMessage(Message msg) {
                if (msg.what == TIME_UPDATE) {
                    mVComMediaSDK.VCOM_QueueControl(VCOM_QUEUECTRL_QUREYQUEUELENGTH, "");//获取队列人数、排队时长、排第几位
                }
            }
        };
        timer = new Timer();
        timer.schedule(new TimerTask() {

            @Override
            public void run() {

                mHandler.sendEmptyMessage(TIME_UPDATE);
            }
        }, 0, 1000);

        queueButton.setOnClickListener(v -> alertDialog());
    }

    /**
     * 刷新数据
     */
    private void refreshData(QueueStateBean queueStateBean) {
        Log.d(tag, "当前排队人数共:" + queueStateBean.getLength() + "人,您现在排在第 " + queueStateBean.getIndex() + " 位");
        showTextView.setText("当前排队人数共:" + queueStateBean.getLength() + "人,您现在排在第 " + queueStateBean.getIndex() + " 位");
        timeshow.setText("您已等待了 " + BaseMethod.getTimeShowStringTwo(queueStateBean.getTime()));
    }

    private void alertDialog() {
        dialog = DialogFactory.getDialog(DialogFactory.DIALOGID_EXIT_QUEUE, queueButton.getText().toString(), this, v -> {
            isDoExit = true;
            finish();
        });
        dialog.show();
    }


    //sdk 初始化
    private void initSdk() {
        if (mVComMediaSDK == null) {
            mVComMediaSDK = VComMediaSDK.GetInstance();
        }
        mVComMediaSDK.SetSDKEvent(this);
    }

    @Override
    protected void onResume() {
        // TODO Auto-generated method stub
        Log.d(tag, "onResume");
        com.videocomm.queue4android.main.BussinessCenter.mContext = QueueActivity.this;
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.d(tag, "onRestart");
    }

    @Override
    protected void onDestroy() {
        // TODO Auto-generated method stub
        super.onDestroy();

        com.videocomm.queue4android.main.BussinessCenter.getBussinessCenter().realse();
        if (mVComMediaSDK != null) {
            mVComMediaSDK.VCOM_QueueControl(VCOM_QUEUECTRL_LEAVEQUEUE, "");
            mVComMediaSDK.RemoveSDKEvent(this);
        }
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
        if (mHandler != null) {
            mHandler.removeCallbacksAndMessages(null);
            mHandler = null;
        }

        if (dialog != null && dialog.isShowing())
            dialog.dismiss();

        mApplication.setTargetUserName("");//清除对方用户名
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        // TODO Auto-generated method stub
        if (event.getAction() == KeyEvent.ACTION_DOWN
                && event.getKeyCode() == KeyEvent.KEYCODE_BACK) {

            alertDialog();
        }
        return super.dispatchKeyEvent(event);
    }

    @Override
    public void OnLoginSystem(String lpUserCode, int iErrorCode, int iReConnect) {

    }

    @Override
    public void OnDisconnect(int iErrorCode) {

    }

    @Override
    public void OnServerKickout(int i) {
        BaseMethod.showToast(getString(R.string.str_againlogin), QueueActivity.this);
        startActivity(new Intent(QueueActivity.this, LoginActivity.class));
    }

    /**
     * 进出会议室通知
     */
    @Override
    public void OnConferenceResult(int iAction, String lpConfId, int iErrorCode) {
        if (VComSDKDefine.VCOM_CONFERENCE_ACTIONCODE_JOIN == iAction) {
            if (iErrorCode == 0) {

            }
        }
    }

    /**
     * @param confid 会议ID
     */
    private void startVideoActvity(String confid) {
        Log.d(tag,"agentId-intent"+agentId);
        isStartVideo = true;
        Intent intent = new Intent();
        intent.putExtra("confid", confid);
        intent.setClass(this, VideoActivity.class);
        this.startActivity(intent);
        finish();
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

        if (iEventType == VCOM_QUEUEEVENT_QUREYQUEUELENGTH) {
            if (iErrorCode != 0) {
                return;
            }
            //进入排队后，可以获取队列人数、排队时长、排第几位
            queueStateBean = JsonUtil.jsonToBean(lpUserData, QueueStateBean.class);
            runOnUiThread(() -> refreshData(queueStateBean));
        } else if (iEventType == VCOM_QUEUEEVENT_AGENTSERVICE) {
            if (iErrorCode != 0) {
                return;
            }
            //坐席点击 示闲 时回调
            //坐席ID
            agentId = JsonUtil.jsonToStr(lpUserData, "agent");
            mApplication.setTargetUserName(agentId);
            mTitleName.setText("正在呼叫坐席" + agentId);
            queueButton.setText(R.string.finish_call);
            showTextView.setVisibility(View.GONE);
            timeshow.setVisibility(View.GONE);
        } else if (iEventType == VCOM_QUEUEEVENT_STARTVIDEO) {
            if (iErrorCode != 0) {
                return;
            }
            //获取会议ID
            String confid = JsonUtil.jsonToStr(lpUserData, "confid");
            //加入会议
//            mVComMediaSDK.VCOM_JoinConference(confid, "", "");
            startVideoActvity(confid);
        } else if (iEventType == VCOM_QUEUEEVENT_HANGUPVIDEO) {
            //用户 坐席挂断 （注意： 这里的ErrorCode 是 408或者0）
            BaseMethod.showToast("对方已结束通话...", QueueActivity.this);
            mApplication.setTargetUserName("");
            finish();
        }
    }

    @Override
    public void OnAIAbilityEvent(int i, int i1, String s) {

    }

}
