package com.example.record;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * 一条通知消息记录
 */
public class NotifyMessage {

    public String packageName;   // 应用包名
    public String appName;       // 应用名称
    public String title;         // 通知标题
    public String content;       // 消息内容
    public long time;            // 通知发布时间（毫秒）

    public NotifyMessage() {
    }

    public NotifyMessage(String packageName, String appName, String title, String content, long time) {
        this.packageName = packageName;
        this.appName = appName;
        this.title = title;
        this.content = content;
        this.time = time;
    }

    public JSONObject toJson() throws JSONException {
        JSONObject obj = new JSONObject();
        obj.put("packageName", packageName);
        obj.put("appName", appName);
        obj.put("title", title);
        obj.put("content", content);
        obj.put("time", time);
        return obj;
    }

    public static NotifyMessage fromJson(JSONObject obj) throws JSONException {
        NotifyMessage msg = new NotifyMessage();
        msg.packageName = obj.optString("packageName");
        msg.appName = obj.optString("appName");
        msg.title = obj.optString("title");
        msg.content = obj.optString("content");
        msg.time = obj.optLong("time");
        return msg;
    }
}