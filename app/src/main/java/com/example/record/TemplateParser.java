package com.example.record;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 日程格式（提取规则）解析器
 *
 * 模板文本示例：你好{内容}，{时间}
 * - {时间}：从通知文本中提取日期时间，作为日程开始时间
 * - {内容}：从通知文本中提取内容，作为日程标题
 */
public class TemplateParser {

    /** 解析结果 */
    public static class ExtractResult {
        public long timeMillis; // 提取到的日程时间
        public String content;  // 提取到的内容
    }

    /** {时间} 匹配：完整日期时间（冒号或中文时/分/秒） | 时分 */
    private static final String TIME_GROUP =
            "(?<time>(?:19|20)\\d{2}[-/.年]\\d{1,2}[-/.月]\\d{1,2}日?(?:\\d{1,2}时\\d{1,2}分(?:\\d{1,2}秒)?|[ T]\\d{1,2}:\\d{2}(?::\\d{2})?)?|\\d{1,2}:\\d{2}(?::\\d{2})?)";
    /** {内容} 匹配：非贪婪任意字符 */
    private static final String CONTENT_GROUP = "(?<content>[\\s\\S]+?)";

    private static final String[] TIME_FORMATS = {
            "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd HH:mm", "yyyy-MM-dd",
            "yyyy/MM/dd HH:mm:ss", "yyyy/MM/dd HH:mm", "yyyy/MM/dd",
            "yyyy.MM.dd HH:mm:ss", "yyyy.MM.dd HH:mm", "yyyy.MM.dd",
            "yyyy年M月d日 HH:mm:ss", "yyyy年M月d日 HH:mm", "yyyy年M月d日",
            "yyyy年M月d日HH时mm分ss秒", "yyyy年M月d日HH时mm分"
    };

    /**
     * 用模板从通知文本中提取日程信息
     *
     * @param template 格式模板（必须同时包含 {时间} 和 {内容}）
     * @param text     通知文本（标题+内容拼接）
     * @return 提取成功返回结果，否则返回 null（跳过不写入）
     */
    public static ExtractResult extract(String template, String text) {
        if (template == null || text == null || text.isEmpty()) return null;
        if (!template.contains("{时间}") || !template.contains("{内容}")) return null;

        Pattern p = buildPattern(template);
        Matcher m = p.matcher(text);
        if (!m.find()) return null;

        String timeStr = null, contentStr = null;
        try { timeStr = m.group("time"); } catch (IllegalArgumentException ignored) { }
        try { contentStr = m.group("content"); } catch (IllegalArgumentException ignored) { }
        if (timeStr == null || contentStr == null) return null;

        contentStr = contentStr.trim();
        if (contentStr.isEmpty()) return null;

        long time = parseTime(timeStr.trim());
        if (time <= 0) return null;

        ExtractResult r = new ExtractResult();
        r.timeMillis = time;
        r.content = contentStr;
        return r;
    }

    /**
     * 将模板转换为正则：普通文本转义，{时间}/{内容} 替换为捕获组
     */
    private static Pattern buildPattern(String template) {
        StringBuilder sb = new StringBuilder();
        Matcher mk = Pattern.compile("\\{时间\\}|\\{内容\\}").matcher(template);
        int last = 0;
        while (mk.find()) {
            sb.append(Pattern.quote(template.substring(last, mk.start())));
            String g = mk.group();
            sb.append(g.equals("{时间}") ? TIME_GROUP : CONTENT_GROUP);
            last = mk.end();
        }
        sb.append(Pattern.quote(template.substring(last)));
        return Pattern.compile(sb.toString());
    }

    /**
     * 解析时间字符串为毫秒时间戳；仅 HH:mm 时按当天日期补全
     */
    private static long parseTime(String s) {
        // 仅时分（如 23:59）→ 取当天日期
        if (s.matches("\\d{1,2}:\\d{2}(?::\\d{2})?")) {
            Calendar cal = Calendar.getInstance();
            String[] hm = s.split(":");
            cal.set(Calendar.HOUR_OF_DAY, Integer.parseInt(hm[0]));
            cal.set(Calendar.MINUTE, Integer.parseInt(hm[1]));
            return cal.getTimeInMillis();
        }
        for (String fmt : TIME_FORMATS) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(fmt, Locale.CHINESE);
                sdf.setLenient(false);
                return sdf.parse(s).getTime();
            } catch (ParseException ignored) {
            }
        }
        return -1;
    }
}