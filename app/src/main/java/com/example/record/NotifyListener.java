package com.example.record;

import android.service.notification.StatusBarNotification;

/**
 * 通知监听回调接口
 */
public interface NotifyListener {

    /**
     * 接收到通知栏消息
     * @param type 消息类型
     */
    void onReceiveMessage(int type);

    /**
     * 移除掉通知栏消息
     * @param type 消息类型
     */
    void onRemovedMessage(int type);

    /**
     * 接收到通知栏消息
     * @param sbn 状态栏通知
     */
    void onReceiveMessage(StatusBarNotification sbn);

    /**
     * 移除掉通知栏消息
     * @param sbn 状态栏通知
     */
    void onRemovedMessage(StatusBarNotification sbn);
}
