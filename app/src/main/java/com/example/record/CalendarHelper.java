package com.example.record;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CalendarContract;

import androidx.core.content.ContextCompat;

import java.util.List;
import java.util.TimeZone;

/**
 * 系统日历写入工具：将通知按格式模板写入日历事件
 */
public class CalendarHelper {

    /** 日程默认时长（毫秒） */
    private static final long DEFAULT_DURATION = 60 * 1000L;

    /** 是否已授予日历读写权限 */
    public static boolean hasCalendarPermission(Context context) {
        return ContextCompat.checkSelfPermission(context, android.Manifest.permission.WRITE_CALENDAR)
                == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * 将一条通知按模板列表写入日程，并按提醒设置添加提醒。
     * 模板为提取规则：从通知文本中提取 {时间} 作为日程开始时间、{内容} 作为标题；
     * 无法提取（未命中）的模板跳过不写入。多个模板命中则各写一条日程。
     *
     * @param remindMode  提醒方式：REMIND_NONE（无提醒）/ REMIND_ALERT（通知提醒）
     * @param remindHours 提前多少小时提醒（remindMode 为 NONE 时忽略）
     * @return 成功写入的条数
     */
    public static int writeSchedules(Context context, NotifyMessage msg, List<String> templates,
                                     String remindMode, int remindHours) {
        if (msg == null || templates == null || templates.isEmpty() || !hasCalendarPermission(context)) {
            return 0;
        }
        long calendarId = getDefaultCalendarId(context);
        if (calendarId < 0) return 0;

        // 用于提取的文本：标题 + 内容
        String text = (safe(msg.title) + " " + safe(msg.content)).trim();

        int count = 0;
        for (String template : templates) {
            if (template == null || template.trim().isEmpty()) continue;
            TemplateParser.ExtractResult r = TemplateParser.extract(template, text);
            if (r == null) continue; // 未命中 → 跳过

            ContentValues v = new ContentValues();
            v.put(CalendarContract.Events.CALENDAR_ID, calendarId);
            v.put(CalendarContract.Events.DTSTART, r.timeMillis);
            v.put(CalendarContract.Events.DTEND, r.timeMillis + DEFAULT_DURATION);
            v.put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().getID());
            v.put(CalendarContract.Events.TITLE, r.content);
            v.put(CalendarContract.Events.DESCRIPTION, text);
            Uri uri = context.getContentResolver().insert(CalendarContract.Events.CONTENT_URI, v);
            if (uri != null) {
                addReminder(context, ContentUris.parseId(uri), remindMode, remindHours);
                count++;
            }
        }
        return count;
    }

    /**
     * 为日程添加提醒（通知提醒）
     */
    public static void addReminder(Context context, long eventId, String remindMode, int remindHours) {
        if (eventId < 0 || AppStore.REMIND_NONE.equals(remindMode)) return;

        ContentValues remindVals = new ContentValues();
        remindVals.put(CalendarContract.Reminders.EVENT_ID, eventId);
        remindVals.put(CalendarContract.Reminders.MINUTES, (long) remindHours * 60); // 提前 N 小时
        remindVals.put(CalendarContract.Reminders.METHOD, CalendarContract.Reminders.METHOD_ALERT); // 通知提醒
        context.getContentResolver().insert(CalendarContract.Reminders.CONTENT_URI, remindVals);
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    /**
     * 获取可写的默认日历ID，找不到返回 -1
     */
    private static long getDefaultCalendarId(Context context) {
        ContentResolver resolver = context.getContentResolver();
        Uri uri = CalendarContract.Calendars.CONTENT_URI;
        String[] projection = new String[]{
                CalendarContract.Calendars._ID,
                CalendarContract.Calendars.IS_PRIMARY,
                CalendarContract.Calendars.VISIBLE
        };
        // 优先主日历，其次可见日历
        Cursor cursor = resolver.query(uri, projection, null, null, null);
        long primaryId = -1;
        long visibleId = -1;
        if (cursor != null) {
            while (cursor.moveToNext()) {
                long id = cursor.getLong(0);
                int isPrimary = cursor.getInt(1);
                int visible = cursor.getInt(2);
                if (isPrimary == 1) {
                    primaryId = id;
                    break;
                }
                if (visible == 1 && visibleId < 0) {
                    visibleId = id;
                }
            }
            cursor.close();
        }
        return primaryId >= 0 ? primaryId : visibleId;
    }
}