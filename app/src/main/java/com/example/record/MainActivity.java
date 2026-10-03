package com.example.record;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.service.notification.StatusBarNotification;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Set;

import static com.example.record.NotifyHelper.*;

public class MainActivity extends AppCompatActivity implements NotifyListener {

    private static final int REQUEST_CODE = 9527;

    private TextView textView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        textView = findViewById(R.id.text);
        NotifyHelper.getInstance().setNotifyListener(this);
    }

    /**
     * 请求权限
     *
     * @param view
     */
    public void requestPermission(View view) {
        if (!isNLServiceEnabled()) {
            startActivityForResult(new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"), REQUEST_CODE);
        } else {
            showMsg("通知服务已开启");
            toggleNotificationListenerService(true);
        }
    }

    private void showMsg(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE) {
            if (isNLServiceEnabled()) {
                showMsg("通知服务已开启");
                toggleNotificationListenerService(true);
            } else {
                showMsg("通知服务未开启");
                toggleNotificationListenerService(false);
            }
        }
    }

    /**
     * 是否启用通知监听服务
     */
    public boolean isNLServiceEnabled() {
        Set<String> packageNames = NotificationManagerCompat.getEnabledListenerPackages(this);
        if (packageNames.contains(getPackageName())) {
            return true;
        }
        return false;
    }

    /**
     * 切换通知监听器服务
     *
     */
    public void toggleNotificationListenerService(boolean issetting) {
        PackageManager pm = getPackageManager();
        if (!issetting)
            pm.setComponentEnabledSetting(new ComponentName(getApplicationContext(), NotifyService.class),
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);

        else
            pm.setComponentEnabledSetting(new ComponentName(getApplicationContext(), NotifyService.class),
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
    }

    /**
     * 收到通知
     *
     * @param type 通知类型
     */
    @Override
    public void onReceiveMessage(int type) {
        switch (type) {
            case N_QQ:
                textView.setText("收到QQ消息");
                break;
            case N_WX:
                textView.setText("收到微信消息");
                break;
            case N_XIAOYA:
                textView.setText("收到小雅消息");
                break;
            default:
                break;
        }
    }

    /**
     * 移除通知
     *
     * @param type 通知类型
     */
    @Override
    public void onRemovedMessage(int type) {
        switch (type) {
            case N_QQ:
                textView.setText("移除QQ消息");
                break;
            case N_WX:
                textView.setText("移除微信消息");
                break;
            case N_XIAOYA:
                textView.setText("移除小雅消息");
                break;
            default:
                break;
        }
    }

    /**
     * 根据包名获取应用名称
     */
    private String getAppName(String packageName) {
        switch (packageName) {
            case NotifyService.QQ:
                return "QQ";
            case NotifyService.WX:
                return "微信";
            case NotifyService.XIAOYA:
                return "小雅";
            default:
                return packageName;
        }
    }

    /**
     * 收到通知
     *
     * @param sbn 状态栏通知
     */
    @Override
    public void onReceiveMessage(StatusBarNotification sbn) {
        if (sbn.getNotification() == null) return;
        //消息内容
        String msgContent = "";
        if (sbn.getNotification().tickerText != null) {
            msgContent = sbn.getNotification().tickerText.toString();
        }

        //消息时间
        String time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINESE).format(new Date(sbn.getPostTime()));
        textView.setText(String.format(Locale.getDefault(),
                "收到%s消息\n应用包名：%s\n消息内容：%s\n消息时间：%s\n",
                getAppName(sbn.getPackageName()), sbn.getPackageName(), msgContent, time));
    }

    /**
     * 移除通知
     *
     * @param sbn 状态栏通知
     */
    @Override
    public void onRemovedMessage(StatusBarNotification sbn) {
        textView.setText("移除" + getAppName(sbn.getPackageName()) + "消息");
    }
}
