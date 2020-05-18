package com.videocomm.queue4android.main;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.os.Bundle;
import android.util.Log;

import com.videocomm.queue4android.R;
import com.videocomm.queue4android.common.BaseConst;
import com.videocomm.queue4android.common.BaseMethod;
import com.videocomm.queue4android.common.CustomApplication;
import com.videocomm.queue4android.common.ScreenInfo;

public class BussinessCenter{

	private static BussinessCenter mBussinessCenter;
	private MediaPlayer mMediaPlayer;
	public static ScreenInfo mScreenInfo;
	public static Activity mContext;
	public static int selfUserId;
	public static boolean bBack = false;
	public static String selfUserName;
	
//	private BussinessCenter() {
//		initParams();
//	}

	public static BussinessCenter getBussinessCenter() {
		if (mBussinessCenter == null)
			mBussinessCenter = new BussinessCenter();
		return mBussinessCenter;
	}

	/***
	 * 播放接收到呼叫音乐提示音
	 * @param context	上下文  
	 */
	private void playCallReceivedMusic(Context context) {
		//播放声音
		mMediaPlayer = MediaPlayer.create(context, R.raw.call);
		mMediaPlayer.setOnCompletionListener(new OnCompletionListener() {

			@Override
			public void onCompletion(MediaPlayer mp) {
				mMediaPlayer.start();
			}
		});
		mMediaPlayer.start();
	}

	/***
	 * 停止播放
	 */
	public void stopSessionMusic() {
		if (mMediaPlayer == null)
			return;
		try {
			mMediaPlayer.pause();
			mMediaPlayer.stop();
			mMediaPlayer.release();
			mMediaPlayer = null;
		} catch (Exception e) {
			Log.i("media-stop", "er");
		}
	}

public void realse() {
		mMediaPlayer = null;
		mScreenInfo = null;
		mContext = null;
		mBussinessCenter = null;
	}

	/***
	 * 视频呼叫事件处理
	 * @param dwEventType	视频呼叫事件类型       
	 * @param dwUserId		目标userid       
	 * @param dwErrorCode	出错代码       
	 * @param dwFlags		功能标志        
	 * @param dwParam		自定义参数，传给对方        
	 * @param szUserStr		自定义参数，传给对方         
	 */
	public static void VideoCallControl(int dwEventType, int dwUserId, int dwErrorCode, int dwFlags, int dwParam, String szUserStr) {
		
	}

	public void onVideoCallRequest(int dwUserId, int dwFlags,
			int dwParam, String szUserStr) {
		//播放音乐
		playCallReceivedMusic(mContext);
		
	}

	public void onVideoCallReply(int dwUserId, int dwErrorCode,
			int dwFlags, int dwParam, String szUserStr) {
		// 回复类型处理
		String strMessage = null;
		switch (dwErrorCode) {
		default:
			break;
		}
			// 如果程序在后台，通知通话结束
			if (bBack) {
				Bundle bundle = new Bundle();
				bundle.putInt("USERID", dwUserId);
				BaseMethod.sendBroadCast(mContext,
						BaseConst.ACTION_BACK_CANCELSESSION, null);
			}
			stopSessionMusic();
		
	}

	public void onVideoCallStart(int dwUserId, int dwFlags, int dwParam,
								 String szUserStr, CustomApplication mApplication) {
		//呼叫开始事件响应
		stopSessionMusic();
		mApplication.setTargetUserId(dwUserId);
		mApplication.setRoomId(dwParam);
		//界面是在这里跳转的
		Intent intent = new Intent();
		intent.setClass(mContext, VideoActivity.class);
		mContext.startActivity(intent);
	}

	public void onVideoCallEnd(int dwUserId, int dwFlags, int dwParam,
			String szUserStr) {
	
		Intent intent = new Intent();
		intent.setClass(mContext, QueueActivity.class);
		mContext.startActivity(intent);
		mContext.finish();
	
	}

}


