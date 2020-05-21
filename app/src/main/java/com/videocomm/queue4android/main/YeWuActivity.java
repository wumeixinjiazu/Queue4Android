package com.videocomm.queue4android.main;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.videocomm.mediasdk.VComMediaSDK;
import com.videocomm.mediasdk.VComSDKEvent;
import com.videocomm.queue4android.R;
import com.videocomm.queue4android.bean.QueueBean;
import com.videocomm.queue4android.common.BaseMethod;
import com.videocomm.queue4android.common.CustomApplication;
import com.videocomm.queue4android.common.DialogFactory;
import com.videocomm.queue4android.common.JsonUtil;

import static com.videocomm.mediasdk.VComSDKDefine.VCOM_QUEUECTRL_HANGUPVIDEO;
import static com.videocomm.mediasdk.VComSDKDefine.VCOM_QUEUECTRL_QUERYQUEUEINFO;
import static com.videocomm.mediasdk.VComSDKDefine.VCOM_QUEUEEVENT_AGENTSERVICE;
import static com.videocomm.mediasdk.VComSDKDefine.VCOM_QUEUEEVENT_ENTERRESULT;
import static com.videocomm.mediasdk.VComSDKDefine.VCOM_QUEUEEVENT_QUERYQUEUEINFO;
import static com.videocomm.mediasdk.VComSDKDefine.VCOM_QUEUEEVENT_STARTVIDEO;


public class YeWuActivity extends Activity implements OnClickListener, VComSDKEvent {
    private ListView listView;
    private com.videocomm.queue4android.main.YeWuAdapter adapter;            //适配器
    public List<Map<String, Object>> queueList = new ArrayList<Map<String, Object>>();//适配器-集合参数

    private TextView mTitleName;
    public ProgressDialog pd;                //进度提示
    public CustomApplication mApplication;    //全局变量类

    private VComMediaSDK mVComMediaSDK;
    private String tag = getClass().getSimpleName();
    private QueueBean queueBean;
    private Dialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //自定义标题栏
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_yewu);
        //初始化sdk
        InitSDK();
        //初始化布局
        initView();
    }

    /**
     * 初始化布局
     */
    private void initView() {
        //进度提示
        pd = new ProgressDialog(YeWuActivity.this);
        //标题栏

        mTitleName = (TextView) this.findViewById(R.id.titleName);
        mTitleName.setText("队列列表");
        //全局变量类初始化
        mApplication = (CustomApplication) getApplication();
        //适配界面
        listView = (ListView) findViewById(R.id.yewu_listview);
    }

    private void refreshData(QueueBean queueBean) {
        queueList.clear();
        //获取适配器的数据
        for (int i = 0; i < queueBean.getQueue_list().size(); i++) {

            Map<String, Object> map = new HashMap<String, Object>();
            map.put("name", queueBean.getQueue_list().get(i).getName());
            map.put("number", queueBean.getQueue_list().get(i).getQueueid());
            map.put("id", queueBean.getQueue_list().get(i).getQueueid());
            //集合数据；
            this.queueList.add(map);
        }

        adapter = new com.videocomm.queue4android.main.YeWuAdapter(YeWuActivity.this, this.queueList, pd, mApplication);
        listView.setAdapter(adapter);
    }


    @Override
    protected void onRestart() {
        Log.d(tag, "onRestart");
        //数据更新
        if (mVComMediaSDK == null) {
            mVComMediaSDK = VComMediaSDK.GetInstance();
        }
        mVComMediaSDK.SetSDKEvent(this);
        mVComMediaSDK.VCOM_QueueControl(VCOM_QUEUECTRL_QUERYQUEUEINFO, "");//查询队列信息

        super.onRestart();
    }


    private void InitSDK() {
        //单例模式获取sdk对象
        if (mVComMediaSDK == null) {
            mVComMediaSDK = VComMediaSDK.GetInstance();
        }
        mVComMediaSDK.SetSDKEvent(this);
        mVComMediaSDK.VCOM_QueueControl(VCOM_QUEUECTRL_QUERYQUEUEINFO, "");//查询队列信息
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            //退出营业厅（进营业厅，出营业厅，加入队列，出队列都是这个方法）
            alertDialog();
        }
        return super.onKeyDown(keyCode, event);
    }

    private void alertDialog() {
        dialog = DialogFactory.getDialog(DialogFactory.DIALOGID_EXIT_YEWU, this, new OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        dialog.show();
    }

    @Override
    public void onClick(View arg0) {
        switch (arg0.getId()) {
            case R.id.returnView://按下返回键
                alertDialog();
                break;
            default:
                break;
        }
    }

    @Override
    protected void onDestroy() {
        // TODO Auto-generated method stub
        super.onDestroy();
        if (mVComMediaSDK != null) {
            mVComMediaSDK.RemoveSDKEvent(this);
            mVComMediaSDK.VCOM_Logout();
        }
        if (dialog != null && dialog.isShowing())
            dialog.dismiss();
    }

    @Override
    public void OnLoginSystem(String lpUserCode, int iErrorCode, int iReConnect) {


    }

    @Override
    public void OnDisconnect(int iErrorCode) {

    }

    @Override
    public void OnServerKickout(int iErrorCode) {
        BaseMethod.showToast(getString(R.string.str_againlogin), YeWuActivity.this);
        startActivity(new Intent(YeWuActivity.this, LoginActivity.class));
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
        if (iErrorCode != 0) {
            return;
        }

        if (iEventType == VCOM_QUEUEEVENT_QUERYQUEUEINFO) {
            //查询队列
            queueBean = JsonUtil.jsonToBean(lpUserData, QueueBean.class);
            refreshData(queueBean);
        } else if (iEventType == VCOM_QUEUEEVENT_ENTERRESULT) {
            //进入队列
            if (pd != null) {
                pd.dismiss();
            }

            Intent in = new Intent();
            in.setClass(YeWuActivity.this, QueueActivity.class);
            startActivity(in);
        } else if (iEventType == VCOM_QUEUEEVENT_AGENTSERVICE) {
            //坐席点击 示闲 时回调
            //坐席ID
            String agentId = JsonUtil.jsonToStr(lpUserData, "agent");
            mApplication.setTargetUserName(agentId);
        }
    }

    @Override
    public void OnAIAbilityEvent(int i, int i1, String s) {

    }
}
