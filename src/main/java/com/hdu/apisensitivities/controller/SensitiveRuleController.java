package com.hdu.apisensitivities.controller;

import com.hdu.apisensitivities.dto.SensitiveRule;
import com.hdu.apisensitivities.service.SensitiveDetection.SensitiveRuleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/rules")
public class SensitiveRuleController {

    private final SensitiveRuleService sensitiveRuleService;

    @Autowired
    public SensitiveRuleController(SensitiveRuleService sensitiveRuleService) {
        this.sensitiveRuleService = sensitiveRuleService;
    }

    @GetMapping
    public ResponseEntity<List<SensitiveRule>> getAllRules() {
        return ResponseEntity.ok(sensitiveRuleService.getAllRules());
    }

    @PostMapping
    public ResponseEntity<String> addRule(@RequestBody SensitiveRule rule) {
        try {
            sensitiveRuleService.addRule(rule);
            return ResponseEntity.ok("规则添加成功");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{patternName}")
    public ResponseEntity<String> updateRule(@PathVariable String patternName, @RequestBody SensitiveRule rule) {
        try {
            rule.setPatternName(patternName);
            sensitiveRuleService.updateRule(rule);
            return ResponseEntity.ok("规则更新成功");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{patternName}")
    public ResponseEntity<String> deleteRule(@PathVariable String patternName) {
        try {
            sensitiveRuleService.deleteRule(patternName);
            return ResponseEntity.ok("规则删除成功");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PatchMapping("/{patternName}/status")
    public ResponseEntity<String> toggleRuleStatus(@PathVariable String patternName, @RequestParam boolean enabled) {
        try {
            sensitiveRuleService.toggleRule(patternName, enabled);
            return ResponseEntity.ok(enabled ? "规则已启用" : "规则已禁用");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
