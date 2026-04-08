package com.hdu.apisensitivities.utils;

import com.hankcs.hanlp.HanLP;
import com.hankcs.hanlp.seg.Segment;
import com.hankcs.hanlp.seg.common.Term;
import com.hdu.apisensitivities.entity.SensitiveEntity;
import com.hdu.apisensitivities.entity.SensitiveType;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NlpEntityDetector {

    // 启用人名、地名、机构名识别
    private static final Segment segment = HanLP.newSegment()
            .enableNameRecognize(true)    // 开启人名识别
            .enablePlaceRecognize(true)   // 开启地名识别
            .enableOrganizationRecognize(true); // 开启机构名识别

    private static final Set<String> COMMON_SURNAME_PREFIX = new HashSet<>();
    private static final Set<String> ADDRESS_SUFFIXES = new HashSet<>();

    private static final Pattern NAME_FALLBACK_PATTERN = Pattern.compile(
            "(?<![\\u4e00-\\u9fa5])([赵钱孙李周吴郑王冯陈褚卫蒋沈韩杨朱秦尤许何吕施张孔曹严华金魏陶姜戚谢邹喻柏水窦章云苏潘葛奚范彭郎鲁韦昌马苗凤花方俞任袁柳酆鲍史唐费廉岑薛雷贺倪汤滕殷罗毕郝邬安常乐于时傅皮卞齐康伍余元卜顾孟平黄和穆萧尹姚邵湛汪祁毛禹狄米贝明臧][\\u4e00-\\u9fa5]{1,3})(?![\\u4e00-\\u9fa5])");
    private static final Pattern ADDRESS_FALLBACK_PATTERN = Pattern.compile(
            "([\\u4e00-\\u9fa5]{2,6}(?:路|街|巷|道|大道|区|市|县|镇|村|号))");

    public static List<SensitiveEntity> detect(String text) {
        List<SensitiveEntity> entities = new ArrayList<>();
        List<Term> termList = segment.seg(text);

        int currentPos = 0;
        for (Term term : termList) {
            String word = term.word;
            String nature = term.nature.toString();
            
            if (word == null || word.trim().isEmpty()) {
                currentPos += word != null ? word.length() : 0;
                continue;
            }
            
            SensitiveType type = getTypeByNature(nature, word);
            if (type == null) {
                if (isPotentialPersonName(word)) {
                    type = SensitiveType.PERSON;
                } else if (isPotentialAddress(word)) {
                    type = SensitiveType.ADDRESS;
                }
            }

            if (type != null && word != null && !word.trim().isEmpty()) {
                // 查找该词在原文本中的偏移量（注意处理重复词）
                int start = text.indexOf(word, currentPos);
                if (start >= 0) {
                    entities.add(SensitiveEntity.builder()
                            .type(type)
                            .originalText(word)
                            .start(start)
                            .end(start + word.length())
                            .confidence(adjustConfidence(type, word))
                            .build());
                    currentPos = start + word.length();
                } else {
                    currentPos += word.length();
                }
            } else {
                currentPos += word.length();
            }
        }
        addFallbackEntities(text, entities);
        return entities;
    }

    private static SensitiveType getTypeByNature(String nature, String word) {
        if (nature == null || word == null || word.trim().isEmpty()) {
            return null;
        }
        switch (nature) {
            case "nr":
            case "nr1":
            case "nr2":
            case "nrj":
            case "nrf":
                return SensitiveType.PERSON;
            case "ns":
            case "nsf":
            case "nz":
                return SensitiveType.ADDRESS;
            case "nt":
                return SensitiveType.ORGANIZATION;
            default:
                return null;
        }
    }

    private static boolean isPotentialPersonName(String word) {
        if (word == null) {
            return false;
        }
        String trimmed = word.trim();
        if (trimmed.length() < 2 || trimmed.length() > 4) {
            return false;
        }
        if (!trimmed.matches("^[\u4e00-\u9fa5]+$")) {
            return false;
        }
        return COMMON_SURNAME_PREFIX.contains(trimmed.substring(0, 1));
    }

    private static boolean isPotentialAddress(String word) {
        if (word == null) {
            return false;
        }
        String trimmed = word.trim();
        if (trimmed.length() < 2 || trimmed.length() > 12) {
            return false;
        }
        if (!trimmed.matches("^[\u4e00-\u9fa50-9]+$")) {
            return false;
        }
        String lastChar = trimmed.substring(trimmed.length() - 1);
        return ADDRESS_SUFFIXES.contains(lastChar);
    }

    private static double adjustConfidence(SensitiveType type, String word) {
        if (type == SensitiveType.PERSON && isPotentialPersonName(word)) {
            return 0.85;
        }
        if (type == SensitiveType.ADDRESS && isPotentialAddress(word)) {
            return 0.80;
        }
        return 0.75;
    }

    private static void addFallbackEntities(String text, List<SensitiveEntity> entities) {
        Set<String> existingKeys = new HashSet<>();
        for (SensitiveEntity entity : entities) {
            existingKeys.add(buildEntityKey(entity));
        }

        addFallbackMatches(text, NAME_FALLBACK_PATTERN, SensitiveType.PERSON, entities, existingKeys);
        addFallbackMatches(text, ADDRESS_FALLBACK_PATTERN, SensitiveType.ADDRESS, entities, existingKeys);
    }

    private static void addFallbackMatches(String text, Pattern pattern, SensitiveType type,
            List<SensitiveEntity> entities, Set<String> existingKeys) {
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            String word = matcher.group(1);
            int start = matcher.start(1);
            int end = matcher.end(1);
            String key = type.name() + ":" + start + ":" + end;
            if (!existingKeys.contains(key)) {
                String trimmed = word.trim();
                if (!trimmed.isEmpty()) {
                    entities.add(SensitiveEntity.builder()
                            .type(type)
                            .originalText(trimmed)
                            .start(start)
                            .end(end)
                            .confidence(type == SensitiveType.PERSON ? 0.75 : 0.70)
                            .build());
                    existingKeys.add(key);
                }
            }
        }
    }

    private static String buildEntityKey(SensitiveEntity entity) {
        return entity.getType().name() + ":" + entity.getStart() + ":" + entity.getEnd();
    }
}
