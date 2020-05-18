package com.videocomm.queue4android.common;

import android.util.Log;

import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * @author[wengCJ]
 * @version[创建日期，2020/4/13 0013]
 * @function[功能简介 Json 转换工具]
 **/
public class JsonUtil {

    /**
     *  Json 转 Bean
     * @param content
     * @param clazz
     * @param <T>
     * @return
     */
    public static <T> T jsonToBean(String content, Class<T> clazz) {
        if (content == null) {
            return null;
        }
        Gson gson = new Gson();
        T bean = gson.fromJson(content, clazz);
        return bean;
    }

    /**
     * @param src 源数据（只接收这个格式的数据解析） {"version":"V2.0.51", "build":"Build Time:Dec 19 2019 09:56:57"}
     * @param parseStr 需要解析的json字段
     * @return
     */
    public static String jsonToStr(String src, String parseStr) {
        if (parseStr == null || parseStr.isEmpty()){
            return "";
        }
        Log.d("JsonUtil",src);
        try {
            JSONObject jsonObject = new JSONObject(src);
            return jsonObject.getString(parseStr);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return "";
    }

}
