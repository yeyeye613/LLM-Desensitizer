package com.hdu.apisensitivities.utils;

import com.hdu.apisensitivities.entity.SensitiveType;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/* 正则表达式规则集合 */
// TODO:  规则过窄导致漏报（手机号提取不全）
// 仅支持典型11位，缺少区分短码、座机、国际号、1800等变体。
// 没考虑输入中有中文或分隔符如138 1234 5678。

// 识别场景容易错误
// "联系我：1393250881@@qq.com"
// 脱敏后提示词
// 📋 复制
// "联系我：[PASSWORD].com"
public class PatternRegistry {

        private static final Map<SensitiveType, Pattern> PATTERN_MAP = new HashMap<>();

        static {
                // 1. 手机号：增加前后的数字断言，防止从身份证号中截取
                // 兼容空格/横杠：138-1234-5678 或 138 1234 5678
                PATTERN_MAP.put(SensitiveType.PHONE_NUMBER,
                        Pattern.compile("(?<!\\d)(?:\\+86)?1[3-9]\\d{1}([\\s-]?\\d{4}){2}(?!\\d)"));

                // 2. 邮箱：优化结尾匹配，防止截断，并确保后缀完整性
                PATTERN_MAP.put(SensitiveType.EMAIL,
                        Pattern.compile("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}(?:\\.[A-Za-z]{2,})?"));

                // 3. 银行卡号：强制要求 13-19 位，且前后不能有数字
                // 银行卡误报率高，必须排除掉可能的身份证号（身份证是18位且含校验码）
                PATTERN_MAP.put(SensitiveType.BANK_CARD,
                        Pattern.compile("(?<!\\d)[1-9]\\d{12,18}(?!\\d)"));

                // 4. 身份证号：严格 18 位格式锁定
                PATTERN_MAP.put(SensitiveType.ID_CARD,
                        Pattern.compile("(?<!\\d)[1-9]\\d{5}(?:18|19|20)\\d{2}(?:0[1-9]|1[0-2])(?:0[1-9]|[12]\\d|3[01])\\d{3}[0-9Xx](?!\\d)"));

                // 5. 密码
                // 逻辑：必须包含字母，长度 8-20，且前后不能紧跟字母数字符号
                // 这样 6222028260688877 这种纯数字就不会被识别为 PASSWORD
                PATTERN_MAP.put(SensitiveType.PASSWORD, 
                        Pattern.compile("(?<![A-Za-z0-9])(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d@$!%*#?&]{8,20}(?![A-Za-z0-9])"));

                // 6. 护照
                PATTERN_MAP.put(SensitiveType.PASSPORT,
                        Pattern.compile("(?<![A-Z0-9])[EeGg][0-9]{8}(?![0-9])"));
                // 信用卡号
                PATTERN_MAP.put(SensitiveType.CREDIT_CARD,
                                Pattern.compile("\\b(?:4[0-9]{12}(?:[0-9]{3})?|5[1-5][0-9]{14}|3[47][0-9]{13}|3(?:0[0-5]|[68][0-9])[0-9]{11}|6(?:011|5[0-9]{2})[0-9]{12}|(?:2131|1800|35\\d{3})\\d{11})\\b"));


                // 社会安全码/社保号 (以US SSN为例，也可扩展中国社保)
                PATTERN_MAP.put(SensitiveType.SOCIAL_SECURITY,
                                Pattern.compile("\\b(?!000|666|9\\d{2})\\d{3}-(?!00)\\d{2}-(?!0000)\\d{4}\\b"));

                // 出生日期 (YYYY-MM-DD, YYYY/MM/DD)
                PATTERN_MAP.put(SensitiveType.BIRTH_DATE,
                                Pattern.compile("\\b(?:19|20)\\d{2}[-/](?:0[1-9]|1[0-2])[-/](?:0[1-9]|[12]\\d|3[01])\\b"));

                // 密码 (潜在的密码字段值，通常结合上下文检测，这里提供一个匹配常见复杂字符串的宽泛正则)
                PATTERN_MAP.put(SensitiveType.PASSWORD,
                                Pattern.compile("\\b(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d@$!%*#?&]{8,}\\b"));

                // IP地址（IPv4 与常见 IPv6 表达）
                PATTERN_MAP.put(SensitiveType.IP_ADDRESS,
                                Pattern.compile(
                                                "\\b(?:(?:25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)\\.){3}(?:25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)\\b"
                                                                + "|" +
                                                                "(?:(?:[A-Fa-f0-9]{1,4}:){7}[A-Fa-f0-9]{1,4}|(?:[A-Fa-f0-9]{1,4}:){1,7}:|:(?::[A-Fa-f0-9]{1,4}){1,7})"));

                // 车牌号（普通与新能源，更加宽松兼容实际文本）
                // 普通车牌：省份简称 + 字母 + 5位字母/数字（不含 I/O）
                // 新能源：省份简称 + 字母 + D/F + 5位字母/数字 或 5位字母/数字 + D/F
                PATTERN_MAP.put(SensitiveType.LICENSE_PLATE,
                                Pattern.compile(
                                                "(?:" +
                                                                "(?:[京津沪渝冀豫云辽黑湘皖鲁新苏浙赣鄂桂甘晋蒙闽藏青川宁琼][A-Z][A-HJ-NP-Z0-9]{5})"
                                                                +
                                                                "|" +
                                                                "(?:[京津沪渝冀豫云辽黑湘皖鲁新苏浙赣鄂桂甘晋蒙闽藏青川宁琼][A-Z](?:[DF][A-HJ-NP-Z0-9]{5}|[A-HJ-NP-Z0-9]{5}[DF]))"
                                                                +
                                                                ")"));
        }

        public static Pattern getPattern(SensitiveType type) {
                return PATTERN_MAP.get(type);
        }

        public static Map<SensitiveType, Pattern> getAllPatterns() {
                return new HashMap<>(PATTERN_MAP);
        }

        // 添加自定义模式
        public static void addCustomPattern(SensitiveType type, String regex) {
                PATTERN_MAP.put(type, Pattern.compile(regex));
        }
}
