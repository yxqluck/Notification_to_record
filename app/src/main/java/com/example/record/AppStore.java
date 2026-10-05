package com.example.record;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 本地存储：基于 SharedPreferences + JSON
 * - 模块启用开关（按包名）
 * - 日程格式模板列表（按包名，可多条）
 * - 最近 N 条通知消息（按包名）
 */
public class AppStore {

    private static final String PREFS_NAME = "app_store";
    private static final int MAX_MESSAGES = 5;

    private static AppStore instance;
    private final SharedPreferences sp;

    private AppStore(Context context) {
        sp = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static synchronized AppStore getInstance(Context context) {
        if (instance == null) {
            instance = new AppStore(context);
        }
        return instance;
    }

    // ---------- 模块开关 ----------

    public boolean isEnabled(String packageName) {
        // 默认全部启用
        return sp.getBoolean("enabled_" + packageName, true);
    }

    public void setEnabled(String packageName, boolean enabled) {
        sp.edit().putBoolean("enabled_" + packageName, enabled).apply();
    }

    // ---------- 日程格式模板 ----------

    public List<String> getTemplates(String packageName) {
        List<String> list = new ArrayList<>();
        String raw = sp.getString("templates_" + packageName, null);
        if (raw == null) {
            // 默认模板
            list.add("{内容}，{时间}");
            saveTemplates(packageName, list);
            return list;
        }
        try {
            JSONArray arr = new JSONArray(raw);
            for (int i = 0; i < arr.length(); i++) {
                String t = arr.getString(i);
                if (t != null && !t.isEmpty()) list.add(t);
            }
        } catch (JSONException ignored) {
        }
        return list;
    }

    public void saveTemplates(String packageName, List<String> templates) {
        JSONArray arr = new JSONArray();
        for (String t : templates) {
            arr.put(t);
        }
        sp.edit().putString("templates_" + packageName, arr.toString()).apply();
    }

    // ---------- 最近消息 ----------

    public List<NotifyMessage> getMessages(String packageName) {
        List<NotifyMessage> list = new ArrayList<>();
        String raw = sp.getString("messages_" + packageName, null);
        if (raw == null) return list;
        try {
            JSONArray arr = new JSONArray(raw);
            for (int i = 0; i < arr.length(); i++) {
                list.add(NotifyMessage.fromJson(arr.getJSONObject(i)));
            }
        } catch (JSONException ignored) {
        }
        return list;
    }

    /**
     * 新增一条消息，保留最近 MAX_MESSAGES 条（新在前）
     */
    public void addMessage(String packageName, NotifyMessage msg) {
        List<NotifyMessage> list = new ArrayList<>();
        list.add(msg);
        list.addAll(getMessages(packageName));
        while (list.size() > MAX_MESSAGES) {
            list.remove(list.size() - 1);
        }
        JSONArray arr = new JSONArray();
        try {
            for (NotifyMessage m : list) {
                arr.put(m.toJson());
            }
        } catch (JSONException ignored) {
        }
        sp.edit().putString("messages_" + packageName, arr.toString()).apply();
    }
}