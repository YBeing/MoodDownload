package com.mooddownload.local.service.config;

import com.mooddownload.local.common.enums.ErrorCode;
import com.mooddownload.local.common.exception.BizException;
import com.mooddownload.local.dal.profile.BtTrackerSetRepository;
import com.mooddownload.local.dal.profile.EngineRuntimeProfileRepository;
import com.mooddownload.local.dal.profile.SourceSiteRuleRepository;
import com.mooddownload.local.mapper.profile.SourceSiteRuleDO;
import com.mooddownload.local.service.config.model.SourceSiteRuleModel;
import com.mooddownload.local.service.config.model.UpdateSourceSiteRuleCommand;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 站点规则服务。
 */
@Service
public class SourceSiteRuleService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SourceSiteRuleService.class);

    private final SourceSiteRuleRepository sourceSiteRuleRepository;

    private final EngineRuntimeProfileRepository engineRuntimeProfileRepository;

    private final BtTrackerSetRepository btTrackerSetRepository;

    private final EngineRuntimeProfileService engineRuntimeProfileService;

    public SourceSiteRuleService(
        SourceSiteRuleRepository sourceSiteRuleRepository,
        EngineRuntimeProfileRepository engineRuntimeProfileRepository,
        BtTrackerSetRepository btTrackerSetRepository,
        EngineRuntimeProfileService engineRuntimeProfileService
    ) {
        this.sourceSiteRuleRepository = sourceSiteRuleRepository;
        this.engineRuntimeProfileRepository = engineRuntimeProfileRepository;
        this.btTrackerSetRepository = btTrackerSetRepository;
        this.engineRuntimeProfileService = engineRuntimeProfileService;
    }

    /**
     * 查询全部站点规则。
     *
     * @return 列表
     */
    @Transactional(rollbackFor = Exception.class)
    public List<SourceSiteRuleModel> listAll() {
        engineRuntimeProfileService.getSnapshot();
        List<SourceSiteRuleModel> ruleModels = new ArrayList<SourceSiteRuleModel>();
        for (SourceSiteRuleDO sourceSiteRuleDO : sourceSiteRuleRepository.findAll()) {
            ruleModels.add(toModel(sourceSiteRuleDO));
        }
        return ruleModels;
    }

    /**
     * 更新站点规则。
     *
     * @param command 更新命令
     * @return 更新后模型
     */
    @Transactional(rollbackFor = Exception.class)
    public SourceSiteRuleModel update(UpdateSourceSiteRuleCommand command) {
        if (command == null || command.getRuleId() == null) {
            throw new BizException(ErrorCode.COMMON_PARAM_INVALID, "ruleId 不能为空");
        }
        String profileCode = normalizeRequired(command.getProfileCode(), "profileCode 不能为空");
        if (!engineRuntimeProfileRepository.findByCode(profileCode).isPresent()) {
            throw new BizException(ErrorCode.SITE_RULE_INVALID, "配置模板不存在: " + profileCode);
        }
        if (StringUtils.hasText(command.getTrackerSetCode())
            && !btTrackerSetRepository.findByCode(command.getTrackerSetCode().trim()).isPresent()) {
            throw new BizException(ErrorCode.SITE_RULE_INVALID, "Tracker 集不存在: " + command.getTrackerSetCode());
        }
        SourceSiteRuleDO existing = sourceSiteRuleRepository.findById(command.getRuleId())
            .orElseThrow(() -> new BizException(ErrorCode.SITE_RULE_INVALID, "站点规则不存在: " + command.getRuleId()));

        existing.setHostPattern(normalizeRequired(command.getHostPattern(), "hostPattern 不能为空"));
        existing.setProfileCode(profileCode);
        existing.setTrackerSetCode(StringUtils.hasText(command.getTrackerSetCode()) ? command.getTrackerSetCode().trim() : null);
        existing.setUpdatedAt(System.currentTimeMillis());
        sourceSiteRuleRepository.saveOrUpdate(existing);
        LOGGER.info("更新站点规则成功: ruleId={}, hostPattern={}", existing.getId(), existing.getHostPattern());
        return toModel(existing);
    }

    private SourceSiteRuleModel toModel(SourceSiteRuleDO sourceSiteRuleDO) {
        SourceSiteRuleModel ruleModel = new SourceSiteRuleModel();
        ruleModel.setId(sourceSiteRuleDO.getId());
        ruleModel.setHostPattern(sourceSiteRuleDO.getHostPattern());
        ruleModel.setProfileCode(sourceSiteRuleDO.getProfileCode());
        ruleModel.setTrackerSetCode(sourceSiteRuleDO.getTrackerSetCode());
        return ruleModel;
    }

    private String normalizeRequired(String value, String errorMessage) {
        if (!StringUtils.hasText(value)) {
            throw new BizException(ErrorCode.COMMON_PARAM_INVALID, errorMessage);
        }
        return value.trim();
    }
}
