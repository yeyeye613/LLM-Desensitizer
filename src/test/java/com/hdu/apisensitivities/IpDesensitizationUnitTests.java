package com.hdu.apisensitivities;

import com.hdu.apisensitivities.entity.SensitiveEntity;
import com.hdu.apisensitivities.entity.SensitiveType;
import com.hdu.apisensitivities.service.Desensitization.MaskDesensitizationStrategy;
import com.hdu.apisensitivities.service.Desensitization.PartialDesensitizationStrategy;
import com.hdu.apisensitivities.utils.PatternRegistry;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.regex.Matcher;

import static org.junit.jupiter.api.Assertions.*;

class IpDesensitizationUnitTests {

    @Test
    void regexMatchesIpv4() {
        String text = "登录失败，来源IP为 192.168.10.25，请检查";
        var pattern = PatternRegistry.getPattern(SensitiveType.IP_ADDRESS);
        Matcher m = pattern.matcher(text);
        assertTrue(m.find());
        assertEquals("192.168.10.25", m.group());
    }

    @Test
    void regexMatchesIpv6() {
        String text = "访问来自 fe80:0:0:0:200:f8ff:fe21:67cf";
        var pattern = PatternRegistry.getPattern(SensitiveType.IP_ADDRESS);
        Matcher m = pattern.matcher(text);
        assertTrue(m.find());
    }

    @Test
    void maskStrategyMasksIp() {
        String text = "来源IP: 10.0.0.123";
        SensitiveEntity entity = SensitiveEntity.builder()
                .type(SensitiveType.IP_ADDRESS)
                .originalText("10.0.0.123")
                .start(text.indexOf("10.0.0.123"))
                .end(text.indexOf("10.0.0.123") + "10.0.0.123".length())
                .confidence(0.99)
                .build();
        String masked = new MaskDesensitizationStrategy().desensitize(text, List.of(entity));
        assertTrue(masked.contains("[IP]"));
    }

    @Test
    void partialStrategyMasksIp() {
        String text = "来源IP: 10.0.0.123";
        SensitiveEntity entity = SensitiveEntity.builder()
                .type(SensitiveType.IP_ADDRESS)
                .originalText("10.0.0.123")
                .start(text.indexOf("10.0.0.123"))
                .end(text.indexOf("10.0.0.123") + "10.0.0.123".length())
                .confidence(0.99)
                .build();
        String masked = new PartialDesensitizationStrategy().desensitize(text, List.of(entity));
        assertTrue(masked.contains("10.0.*.*"));
    }
}

