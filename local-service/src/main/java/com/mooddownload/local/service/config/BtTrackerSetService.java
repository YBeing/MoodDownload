package com.mooddownload.local.service.config;

import com.mooddownload.local.common.enums.ErrorCode;
import com.mooddownload.local.common.exception.BizException;
import com.mooddownload.local.dal.profile.BtTrackerSetRepository;
import com.mooddownload.local.mapper.profile.BtTrackerSetDO;
import com.mooddownload.local.service.config.model.BtTrackerSetModel;
import com.mooddownload.local.service.config.model.UpdateBtTrackerSetCommand;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Tracker 配置集服务。
 */
@Service
public class BtTrackerSetService {

    private static final Logger LOGGER = LoggerFactory.getLogger(BtTrackerSetService.class);

    private final BtTrackerSetRepository btTrackerSetRepository;

    private final EngineRuntimeProfileService engineRuntimeProfileService;

    public BtTrackerSetService(
        BtTrackerSetRepository btTrackerSetRepository,
        EngineRuntimeProfileService engineRuntimeProfileService
    ) {
        this.btTrackerSetRepository = btTrackerSetRepository;
        this.engineRuntimeProfileService = engineRuntimeProfileService;
    }

    /**
     * 查询全部 Tracker 配置集。
     *
     * @return 列表
     */
    @Transactional(rollbackFor = Exception.class)
    public List<BtTrackerSetModel> listAll() {
        engineRuntimeProfileService.getSnapshot();
        List<BtTrackerSetModel> trackerSetModels = new ArrayList<BtTrackerSetModel>();
        for (BtTrackerSetDO trackerSetDO : btTrackerSetRepository.findAll()) {
            trackerSetModels.add(toModel(trackerSetDO));
        }
        return trackerSetModels;
    }

    /**
     * 更新 Tracker 配置集。
     *
     * @param command 更新命令
     * @return 更新后模型
     */
    @Transactional(rollbackFor = Exception.class)
    public BtTrackerSetModel update(UpdateBtTrackerSetCommand command) {
        if (command == null) {
            throw new BizException(ErrorCode.COMMON_PARAM_INVALID, "Tracker 更新命令不能为空");
        }
        String trackerSetCode = normalizeRequired(command.getTrackerSetCode(), "trackerSetCode 不能为空");
        String trackerSetName = normalizeRequired(command.getTrackerSetName(), "trackerSetName 不能为空");
        String trackerListText = normalizeRequired(command.getTrackerListText(), "trackerListText 不能为空");
        engineRuntimeProfileService.getSnapshot();

        long now = System.currentTimeMillis();
        BtTrackerSetDO existing = btTrackerSetRepository.findByCode(trackerSetCode).orElse(null);
        BtTrackerSetDO trackerSetDO = existing == null ? new BtTrackerSetDO() : existing;
        trackerSetDO.setTrackerSetCode(trackerSetCode);
        trackerSetDO.setTrackerSetName(trackerSetName);
        trackerSetDO.setSourceType(existing == null ? "BT" : existing.getSourceType());
        trackerSetDO.setTrackerListText(trackerListText);
        trackerSetDO.setTrackerSourceUrl(StringUtils.hasText(command.getSourceUrl()) ? command.getSourceUrl().trim() : null);
        trackerSetDO.setIsBuiltin(existing == null ? 0 : existing.getIsBuiltin());
        trackerSetDO.setCreatedAt(existing == null ? now : existing.getCreatedAt());
        trackerSetDO.setUpdatedAt(now);
        btTrackerSetRepository.saveOrUpdate(trackerSetDO);
        LOGGER.info("更新 Tracker 配置集成功: trackerSetCode={}", trackerSetCode);
        return toModel(trackerSetDO);
    }

    private BtTrackerSetModel toModel(BtTrackerSetDO trackerSetDO) {
        BtTrackerSetModel trackerSetModel = new BtTrackerSetModel();
        trackerSetModel.setTrackerSetCode(trackerSetDO.getTrackerSetCode());
        trackerSetModel.setTrackerSetName(trackerSetDO.getTrackerSetName());
        trackerSetModel.setTrackerListText(trackerSetDO.getTrackerListText());
        trackerSetModel.setSourceUrl(trackerSetDO.getTrackerSourceUrl());
        return trackerSetModel;
    }

    private String normalizeRequired(String value, String errorMessage) {
        if (!StringUtils.hasText(value)) {
            throw new BizException(ErrorCode.COMMON_PARAM_INVALID, errorMessage);
        }
        return value.trim();
    }
}
