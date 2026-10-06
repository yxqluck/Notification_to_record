package com.example.record;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.service.notification.StatusBarNotification;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.Space;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static com.example.record.NotifyHelper.*;

public class MainActivity extends AppCompatActivity implements NotifyListener {

    private static final int REQUEST_PERMISSION = 9527;
    private static final int REQUEST_CALENDAR = 9528;
    private static final int REQUEST_NOTIFICATION = 9529;

    private Button btnPermission;
    private ScrollView scrollModules;
    private LinearLayout moduleContainer;
    private AppStore store;

    /** 模块定义（显示名 -> 包名），保持竖向展示顺序 */
    private final String[][] MODULES = {
            {"微信", NotifyService.WX},
            {"QQ", NotifyService.QQ},
            {"小雅", NotifyService.XIAOYA},
    };

    /** 包名 -> 模块消息列表区，用于刷新 */
    private final Map<String, TextView> messageViews = new HashMap<>();

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

        btnPermission = findViewById(R.id.btn_permission);
        scrollModules = findViewById(R.id.scroll_modules);
        moduleContainer = findViewById(R.id.module_container);
        store = AppStore.getInstance(this);
        NotifyHelper.getInstance().setNotifyListener(this);

        buildModules();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshUi();
    }

    // ---------------- 权限与界面切换 ----------------

    /**
     * 根据通知监听权限状态切换界面
     * 无权限：只显示获取权限按钮；有权限：隐藏按钮，只显示三模块
     */
    private void refreshUi() {
        boolean enabled = isNLServiceEnabled();
        btnPermission.setVisibility(enabled ? View.GONE : View.VISIBLE);
        scrollModules.setVisibility(enabled ? View.VISIBLE : View.GONE);
        if (enabled) {
            // 已授权但监听服务未真正连接（如进程被杀后）→ 强制触发系统重连
            if (!NotifyService.isConnected()) {
                forceRebindNotifyService();
            }
            refreshAllMessages();
            requestCalendarPermissionIfNeeded();
            requestNotificationPermissionIfNeeded();
        }
    }

    /**
     * 已获得通知监听权限但服务断开（进程被杀等场景）时，
     * 通过禁用再启用组件的方式强制系统重新绑定监听服务。
     */
    private void forceRebindNotifyService() {
        PackageManager pm = getPackageManager();
        ComponentName cn = new ComponentName(getApplicationContext(), NotifyService.class);
        pm.setComponentEnabledSetting(cn, PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP);
        pm.setComponentEnabledSetting(cn, PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP);
    }

    /**
     * 请求权限（按钮点击）
     */
    public void requestPermission(View view) {
        if (!isNLServiceEnabled()) {
            startActivityForResult(new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"), REQUEST_PERMISSION);
        } else {
            showMsg("通知服务已开启");
        }
    }

    /**
     * 日历写权限（写日程必需，动态请求）
     */
    private void requestCalendarPermissionIfNeeded() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_CALENDAR)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.READ_CALENDAR, android.Manifest.permission.WRITE_CALENDAR},
                    REQUEST_CALENDAR);
        }
    }

    /**
     * Android 13+ 通知权限（POST_NOTIFICATIONS），不请求则默认拒绝
     */
    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.POST_NOTIFICATIONS},
                    REQUEST_NOTIFICATION);
        }
    }

    private void showMsg(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_PERMISSION) {
            if (isNLServiceEnabled()) {
                showMsg("通知服务已开启");
            } else {
                showMsg("通知服务未开启");
            }
        }
    }

    /**
     * 是否启用通知监听服务
     */
    public boolean isNLServiceEnabled() {
        Set<String> packageNames = NotificationManagerCompat.getEnabledListenerPackages(this);
        return packageNames.contains(getPackageName());
    }

    // ---------------- 模块构建 ----------------

    private void buildModules() {
        moduleContainer.removeAllViews();
        messageViews.clear();
        for (String[] module : MODULES) {
            moduleContainer.addView(buildModuleCard(module[0], module[1]));
            moduleContainer.addView(new Space(this));
        }
    }

    /**
     * 构建单个模块卡片
     * 顶部：app图标（可获取才显示）、app名称、启用开关、设置按钮
     * 内部：最近5条消息
     */
    private View buildModuleCard(String appName, String packageName) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(12), dp(10), dp(12), dp(10));
        card.setBackgroundResource(R.drawable.bg_card);
        card.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // ---- 顶栏 ----
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setClickable(false);
        card.addView(header, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // 图标（能获取到才显示）
        ImageView icon = new ImageView(this);
        Drawable d = loadAppIcon(packageName);
        if (d != null) {
            int iconSize = dp(40);
            icon.setImageDrawable(d);
            header.addView(icon, new LinearLayout.LayoutParams(iconSize, iconSize));
        }

        // 名称
        TextView nameTv = new TextView(this);
        nameTv.setText(appName);
        nameTv.setTextSize(18);
        nameTv.setPadding(dp(10), 0, dp(10), 0);
        LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        header.addView(nameTv, nameParams);

        // 启用开关
        Switch sw = new Switch(this);
        sw.setChecked(store.isEnabled(packageName));
        sw.setOnCheckedChangeListener((buttonView, isChecked) -> {
            store.setEnabled(packageName, isChecked);
            showMsg(isChecked ? appName + "已启用" : appName + "已停用");
        });
        header.addView(sw);

        // 设置按钮（日程格式模板）——紧凑图标按钮
        ImageButton btnSettings = new ImageButton(this);
        btnSettings.setImageResource(R.drawable.ic_settings);
        TypedValue tv = new TypedValue();
        if (getTheme().resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, tv, true)) {
            btnSettings.setBackgroundResource(tv.resourceId);
        }
        btnSettings.setContentDescription("设置");
        btnSettings.setOnClickListener(v -> showTemplateDialog(packageName));
        header.addView(btnSettings, new LinearLayout.LayoutParams(dp(40), dp(40)));

        // ---- 消息列表区 ----
        TextView msgTv = new TextView(this);
        msgTv.setTextSize(14);
        msgTv.setPadding(0, dp(8), 0, 0);
        msgTv.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray));
        LinearLayout.LayoutParams msgParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        card.addView(msgTv, msgParams);
        messageViews.put(packageName, msgTv);
        return card;
    }

    /**
     * 加载应用图标，失败返回 null
     */
    @Nullable
    private Drawable loadAppIcon(String packageName) {
        try {
            return getPackageManager().getApplicationIcon(packageName);
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }

    private int dp(int value) {
        return Math.round(getResources().getDisplayMetrics().density * value);
    }

    // ---------------- 消息刷新 ----------------

    private void refreshAllMessages() {
        for (String packageName : messageViews.keySet()) {
            refreshMessages(packageName);
        }
    }

    private void refreshMessages(String packageName) {
        TextView tv = messageViews.get(packageName);
        if (tv == null) return;
        List<NotifyMessage> list = store.getMessages(packageName);
        if (list.isEmpty()) {
            tv.setText("暂无消息");
            return;
        }
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINESE);
        StringBuilder sb = new StringBuilder();
        for (NotifyMessage m : list) {
            String time = fmt.format(new Date(m.time));
            sb.append('[').append(time).append("] ");
            if (m.title != null && !m.title.isEmpty() && !m.title.equals(m.content)) {
                sb.append(m.title).append("：");
            }
            if (m.content != null) sb.append(m.content);
            sb.append('\n');
        }
        // 去掉末尾换行
        if (sb.length() > 0) sb.setLength(sb.length() - 1);
        tv.setText(sb.toString());
    }

    // ---------------- 日程格式模板设置 ----------------

    /**
     * 设置弹窗：日程提醒设置 + 通知文本提取规则（模板）
     */
    private void showTemplateDialog(String packageName) {
        View content = getLayoutInflater().inflate(R.layout.dialog_settings, null);

        RadioGroup rgRemind = content.findViewById(R.id.rg_remind);
        EditText etHours = content.findViewById(R.id.et_hours);
        View tilHours = content.findViewById(R.id.til_hours);
        LinearLayout templateArea = content.findViewById(R.id.template_area);
        MaterialButton btnAdd = content.findViewById(R.id.btn_add_template);

        String remindMode = store.getRemindMode(packageName);
        int remindHours = store.getRemindHours(packageName);

        rgRemind.check(AppStore.REMIND_ALERT.equals(remindMode) ? R.id.rb_alert : R.id.rb_none);
        etHours.setText(String.valueOf(remindHours));

        // 无提醒时隐藏"提前提醒"输入
        rgRemind.setOnCheckedChangeListener((group, checkedId) ->
                tilHours.setVisibility(checkedId == R.id.rb_alert ? View.VISIBLE : View.GONE));
        tilHours.setVisibility(remindMode.equals(AppStore.REMIND_ALERT) ? View.VISIBLE : View.GONE);

        for (String t : store.getTemplates(packageName)) {
            templateArea.addView(createTemplateRow(t));
        }
        btnAdd.setOnClickListener(v -> templateArea.addView(createTemplateRow("")));

        new MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_App_SettingsDialog)
                .setTitle("设置")
                .setView(content)
                .setPositiveButton("保存", (dialog, which) ->
                        saveTemplateSettings(packageName, rgRemind, etHours, templateArea))
                .setNegativeButton("取消", null)
                .show();
    }

    /**
     * 单个模板编辑行：输入框 + 删除按钮
     */
    private View createTemplateRow(String text) {
        View row = getLayoutInflater().inflate(R.layout.item_template_row, null);
        EditText et = row.findViewById(R.id.et_template);
        et.setText(text);
        row.findViewById(R.id.btn_del_template)
                .setOnClickListener(v -> ((ViewGroup) row.getParent()).removeView(row));
        return row;
    }

    /**
     * 保存模板与提醒设置
     */
    private void saveTemplateSettings(String packageName, RadioGroup rgRemind,
                                      EditText etHours, LinearLayout templateArea) {
        java.util.ArrayList<String> list = new java.util.ArrayList<>();
        for (int i = 0; i < templateArea.getChildCount(); i++) {
            View row = templateArea.getChildAt(i);
            EditText et = row.findViewById(R.id.et_template);
            if (et != null) {
                String t = et.getText().toString().trim();
                if (!t.isEmpty()) list.add(t);
            }
        }
        store.saveTemplates(packageName, list);

        // 提醒方式：无提醒 / 通知提醒
        store.setRemindMode(packageName,
                rgRemind.getCheckedRadioButtonId() == R.id.rb_alert
                        ? AppStore.REMIND_ALERT : AppStore.REMIND_NONE);

        // 提前小时（正整数，非法输入回退为 1）
        int hours;
        try {
            hours = Integer.parseInt(etHours.getText().toString().trim());
            if (hours <= 0) hours = 1;
        } catch (NumberFormatException e) {
            hours = 1;
        }
        store.setRemindHours(packageName, hours);

        showMsg("已保存");
    }

    // ---------------- 通知回调 ----------------

    @Override
    public void onReceiveMessage(int type) {
    }

    @Override
    public void onRemovedMessage(int type) {
    }

    @Override
    public void onReceiveMessage(StatusBarNotification sbn) {
        if (sbn == null) return;
        refreshMessages(sbn.getPackageName());
    }

    @Override
    public void onRemovedMessage(StatusBarNotification sbn) {
        if (sbn != null) {
            refreshMessages(sbn.getPackageName());
        }
    }
}