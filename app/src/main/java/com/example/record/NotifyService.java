package com.example.record;

import android.app.Notification;
import android.content.ComponentName;
import android.os.Build;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.content.pm.PackageManager;

import java.util.List;

/**
 * 通知监听服务：只监听微信、QQ、小雅的通知
 * - 受 AppStore 开关控制（关闭则不监听、不写日程）
 * - 存最近5条到本地，并按模板写入系统日历
 */
public class NotifyService extends NotificationListenerService {

    public static final String TAG = "NotifyService";

    /** 监听服务是否与系统真正连接（进程被杀后会被置 false） */
    private static volatile boolean connected = false;

    public static boolean isConnected() {
        return connected;
    }

    /** 通知监听服务与系统连接的入口 */
    @Override
    public void onListenerConnected() {
        connected = true;
    }

    public static final String QQ = "com.tencent.mobileqq";//qq信息
    public static final String WX = "com.tencent.mm";//微信信息
    public static final String XIAOYA = "com.ccnu.jx.xiaoya";//小雅

    /**
     * 是否为需要监听的应用
     */
    private boolean isTargetApp(String packageName) {
        return QQ.equals(packageName) || WX.equals(packageName) || XIAOYA.equals(packageName);
    }

    /**
     * 获取应用显示名称
     */
    private String getAppName(String packageName) {
        try {
            PackageManager pm = getPackageManager();
            CharSequence label = getPackageManager().getApplicationLabel(
                    pm.getApplicationInfo(packageName, 0));
            if (label != null) return label.toString();
        } catch (Exception ignored) {
        }
        switch (packageName) {
            case QQ:
                return "QQ";
            case WX:
                return "微信";
            case XIAOYA:
                return "小雅";
            default:
                return packageName;
        }
    }

    /**
     * 提取通知消息内容
     */
    private NotifyMessage buildMessage(StatusBarNotification sbn) {
        Notification n = sbn.getNotification();
        String title = "";
        String content = "";
        if (n.extras != null) {
            Bundle extras = n.extras;
            CharSequence t = extras.getCharSequence(Notification.EXTRA_TITLE);
            CharSequence c = extras.getCharSequence(Notification.EXTRA_TEXT);
            if (t != null) title = t.toString();
            if (c != null) content = c.toString();
        }
        if (content.isEmpty() && n.tickerText != null) {
            content = n.tickerText.toString();
        }
        if (title.isEmpty() && n.tickerText != null) {
            title = n.tickerText.toString();
        }
        return new NotifyMessage(
                sbn.getPackageName(),
                getAppName(sbn.getPackageName()),
                title,
                content,
                sbn.getPostTime());
    }

    /**
     * 保存最近5条（受开关控制）
     */
    private void saveMessage(NotifyMessage msg) {
        if (msg == null) return;
        AppStore store = AppStore.getInstance(this);
        if (!store.isEnabled(msg.packageName)) return;
        store.addMessage(msg.packageName, msg);
    }

    /**
     * 写入系统日历（受开关控制）
     */
    private void writeSchedule(NotifyMessage msg) {
        if (msg == null) return;
        AppStore store = AppStore.getInstance(this);
        if (!store.isEnabled(msg.packageName)) return;
        List<String> templates = store.getTemplates(msg.packageName);
        String remindMode = store.getRemindMode(msg.packageName);
        int remindHours = store.getRemindHours(msg.packageName);
        CalendarHelper.writeSchedules(this, msg, templates, remindMode, remindHours);
    }

    /**
     * 发布通知
     *
     * @param sbn 状态栏通知
     */
    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        if (sbn == null || !isTargetApp(sbn.getPackageName())) return;
        NotifyMessage msg = buildMessage(sbn);
        saveMessage(msg);
        writeSchedule(msg);
        NotifyHelper.getInstance().onReceive(sbn);
    }

    /**
     * 通知已删除
     *
     * @param sbn 状态栏通知
     */
    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        if (sbn == null || !isTargetApp(sbn.getPackageName())) return;
        NotifyHelper.getInstance().onRemoved(sbn);
    }

    /**
     * 监听断开
     */
    @Override
    public void onListenerDisconnected() {
        connected = false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            // 通知侦听器断开连接 - 请求重新绑定
            requestRebind(new ComponentName(this, NotificationListenerService.class));
        }
    }
}