package com.example.record;

import android.content.ComponentName;
import android.os.Build;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

/**
 * 通知监听服务：只监听微信、QQ、小雅的通知，转发给 NotifyHelper 分发
 */
public class NotifyService extends NotificationListenerService {

    public static final String TAG = "NotifyService";

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
     * 发布通知
     *
     * @param sbn 状态栏通知
     */
    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        if (isTargetApp(sbn.getPackageName())) {
            NotifyHelper.getInstance().onReceive(sbn);
        }
    }

    /**
     * 通知已删除
     *
     * @param sbn 状态栏通知
     */
    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        if (isTargetApp(sbn.getPackageName())) {
            NotifyHelper.getInstance().onRemoved(sbn);
        }
    }

    /**
     * 监听断开
     */
    @Override
    public void onListenerDisconnected() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            // 通知侦听器断开连接 - 请求重新绑定
            requestRebind(new ComponentName(this, NotificationListenerService.class));
        }
    }
}
