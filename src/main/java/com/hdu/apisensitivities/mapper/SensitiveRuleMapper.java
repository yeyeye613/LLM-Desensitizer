package com.hdu.apisensitivities.mapper;

import com.hdu.apisensitivities.dto.SensitiveRule;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SensitiveRuleMapper {

    int insert(SensitiveRule rule);

    int update(SensitiveRule rule);

    int deleteByPatternName(@Param("patternName") String patternName);

    SensitiveRule selectByPatternName(@Param("patternName") String patternName);

    List<SensitiveRule> selectAll();

    List<SensitiveRule> selectEnabled();

    int updateStatus(@Param("patternName") String patternName, @Param("isEnabled") Boolean isEnabled);
}