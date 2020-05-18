package com.videocomm.queue4android.main;


import java.util.List;
import java.util.Map;

import android.app.ProgressDialog;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.TextView;

import com.videocomm.mediasdk.VComMediaSDK;
import com.videocomm.queue4android.R;
import com.videocomm.queue4android.common.CustomApplication;

import static com.videocomm.mediasdk.VComSDKDefine.VCOM_QUEUECTRL_ENTERQUEUE;
import static com.videocomm.mediasdk.VComSDKDefine.VCOM_QUEUEEVENT_ENTERRESULT;

public class YeWuAdapter extends BaseAdapter {
    private Context mcontext;
    private LayoutInflater inflater;
    private List<Map<String, Object>> list;
    private ProgressDialog pd;
    public CustomApplication mCustomApplication;
    String tag = getClass().getSimpleName();

    @Override
    public int getCount() {
        // TODO Auto-generated method stub
        return list.size();
    }

    public YeWuAdapter(Context context, List<Map<String, Object>> list, ProgressDialog pd, CustomApplication mApplication) {
        super();
        this.mcontext = context;
        this.list = list;
        this.pd = pd;
        this.mCustomApplication = mApplication;
    }

    @Override
    public Object getItem(int arg0) {
        // TODO Auto-generated method stub
        return list.get(arg0);
    }

    @Override
    public long getItemId(int position) {
        // TODO Auto-generated method stub
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // TODO Auto-generated method stub
        ViewHolder viewHolder = null;
        if (convertView == null) {

            inflater = LayoutInflater.from(parent.getContext());
            convertView = inflater.inflate(R.layout.yewu_item, null);
            viewHolder = new ViewHolder();
            viewHolder.button = (Button) convertView.findViewById(R.id.yewu_button);
            viewHolder.people = (TextView) convertView.findViewById(R.id.yewu_tv);
            viewHolder.name = (TextView) convertView.findViewById(R.id.yewu_name);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }
        String titleStr = list.get(position).get("name").toString();
        String number = (String) list.get(position).get("number");
        final String Id = (String) list.get(position).get("id");

//		viewHolder.people.setText("当前队列有"+number+"人等待...");
        viewHolder.name.setText(titleStr);

        viewHolder.button.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                //进度提示
                showprogess();
                //保存进入队列的Id
                mCustomApplication.setCurrentQueueId(Id);
                //保存选择的业务
				mCustomApplication.setselectBussiness(titleStr);
                String lpCtrlValue = "{\"queueid\": \"" + Id + "\"}";
                Log.d(tag, lpCtrlValue);

                VComMediaSDK.GetInstance().VCOM_QueueControl(VCOM_QUEUECTRL_ENTERQUEUE, lpCtrlValue);
            }

            private void showprogess() {
                pd.setMessage("进入业务排队...");
                pd.setCancelable(true);
                pd.setProgress(ProgressDialog.STYLE_HORIZONTAL);
                pd.setIndeterminate(true);
                pd.show();
            }
        });

        return convertView;
    }

    public class ViewHolder {
        public Button button;
        public TextView people;
        public TextView name;
    }

}
