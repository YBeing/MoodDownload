package com.mooddownload.local.dal.profile;

import com.mooddownload.local.mapper.profile.SourceSiteRuleDO;
import com.mooddownload.local.mapper.profile.SourceSiteRuleMapper;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/**
 * 站点规则 Repository。
 */
@Repository
public class SourceSiteRuleRepository {

    private final SourceSiteRuleMapper sourceSiteRuleMapper;

    public SourceSiteRuleRepository(SourceSiteRuleMapper sourceSiteRuleMapper) {
        this.sourceSiteRuleMapper = sourceSiteRuleMapper;
    }

    public Optional<SourceSiteRuleDO> findById(Long id) {
        return Optional.ofNullable(sourceSiteRuleMapper.selectById(id));
    }

    public List<SourceSiteRuleDO> findAll() {
        return sourceSiteRuleMapper.selectAll();
    }

    public void saveOrUpdate(SourceSiteRuleDO sourceSiteRuleDO) {
        if (sourceSiteRuleDO.getId() == null || sourceSiteRuleMapper.selectById(sourceSiteRuleDO.getId()) == null) {
            sourceSiteRuleMapper.insert(sourceSiteRuleDO);
            return;
        }
        sourceSiteRuleMapper.updateById(sourceSiteRuleDO);
    }
}
