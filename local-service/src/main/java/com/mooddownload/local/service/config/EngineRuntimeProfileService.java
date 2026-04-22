package com.mooddownload.local.service.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mooddownload.local.client.aria2.Aria2RpcClient;
import com.mooddownload.local.common.enums.ErrorCode;
import com.mooddownload.local.common.exception.BizException;
import com.mooddownload.local.dal.config.DownloadConfigRepository;
import com.mooddownload.local.dal.profile.BtTrackerSetRepository;
import com.mooddownload.local.dal.profile.EngineRuntimeProfileRepository;
import com.mooddownload.local.mapper.config.DownloadConfigDO;
import com.mooddownload.local.mapper.profile.BtTrackerSetDO;
import com.mooddownload.local.mapper.profile.EngineRuntimeProfileDO;
import com.mooddownload.local.service.config.model.EngineRuntimeApplyResultModel;
import com.mooddownload.local.service.config.model.EngineRuntimeProfileItemModel;
import com.mooddownload.local.service.config.model.EngineRuntimeSnapshotModel;
import com.mooddownload.local.service.config.model.UpdateDownloadConfigCommand;
import com.mooddownload.local.service.config.model.UpdateEngineRuntimeProfileCommand;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * BT 动态配置中心服务。
 */
@Service
public class EngineRuntimeProfileService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EngineRuntimeProfileService.class);

    private static final String DEFAULT_PROFILE_CODE = "default";

    private static final String DEFAULT_PROFILE_NAME = "默认配置";

    private static final String DEFAULT_TRACKER_SET_CODE = "builtin-default";

    private static final String DEFAULT_TRACKER_SET_NAME = "内置默认 Tracker";

    private static final String DEFAULT_TRACKER_LIST_TEXT =
        "udp://tracker.opentrackr.org:1337/announce\n"
            + "udp://tracker.torrent.eu.org:451/announce\n"
            + "udp://open.stealth.si:80/announce\n"
            + "udp://tracker.tryhackx.org:6969/announce\n"
            + "udp://tracker.qu.ax:6969/announce\n"
            + "https://tracker.opentrackr.org:443/announce";

    private static final String DEFAULT_PROFILE_JSON = "{\n"
        + "  \"enable-dht\": \"true\",\n"
        + "  \"enable-dht6\": \"true\",\n"
        + "  \"bt-enable-lpd\": \"true\",\n"
        + "  \"enable-peer-exchange\": \"true\",\n"
        + "  \"listen-port\": \"51413\",\n"
        + "  \"dht-listen-port\": \"51413\",\n"
        + "  \"seed-time\": \"0\",\n"
        + "  \"follow-torrent\": \"true\",\n"
        + "  \"follow-metalink\": \"true\",\n"
        + "  \"allow-overwrite\": \"true\"\n"
        + "}";

    private static final String APPLY_STATUS_IDLE = "IDLE";

    private static final String APPLY_STATUS_UPDATED = "UPDATED";

    private static final String APPLY_STATUS_APPLIED = "APPLIED";

    private static final String APPLY_STATUS_PARTIALLY_APPLIED = "PARTIALLY_APPLIED";

    private static final String APPLY_STATUS_RESTART_REQUIRED = "RESTART_REQUIRED";

    private static final Set<String> HOT_UPDATE_OPTION_KEYS = Collections.unmodifiableSet(new LinkedHashSet<String>() {{
        add("max-concurrent-downloads");
        add("max-overall-download-limit");
        add("max-overall-upload-limit");
    }});

    private static final TypeReference<LinkedHashMap<String, Object>> PROFILE_JSON_TYPE =
        new TypeReference<LinkedHashMap<String, Object>>() { };

    private final EngineRuntimeProfileRepository engineRuntimeProfileRepository;

    private final BtTrackerSetRepository btTrackerSetRepository;

    private final DownloadConfigRepository downloadConfigRepository;

    private final ConfigService configService;

    private final Aria2RpcClient aria2RpcClient;

    private final ObjectMapper objectMapper;

    public EngineRuntimeProfileService(
        EngineRuntimeProfileRepository engineRuntimeProfileRepository,
        BtTrackerSetRepository btTrackerSetRepository,
        DownloadConfigRepository downloadConfigRepository,
        ConfigService configService,
        Aria2RpcClient aria2RpcClient,
        ObjectMapper objectMapper
    ) {
        this.engineRuntimeProfileRepository = engineRuntimeProfileRepository;
        this.btTrackerSetRepository = btTrackerSetRepository;
        this.downloadConfigRepository = downloadConfigRepository;
        this.configService = configService;
        this.aria2RpcClient = aria2RpcClient;
        this.objectMapper = objectMapper;
    }

    /**
     * 查询引擎运行配置中心快照。
     *
     * @return 快照模型
     */
    @Transactional(rollbackFor = Exception.class)
    public EngineRuntimeSnapshotModel getSnapshot() {
        return buildSnapshot(APPLY_STATUS_IDLE);
    }

    /**
     * 更新配置模板并设置为当前激活模板。
     *
     * @param command 更新命令
     * @return 快照模型
     */
    @Transactional(rollbackFor = Exception.class)
    public EngineRuntimeSnapshotModel updateProfile(UpdateEngineRuntimeProfileCommand command) {
        if (command == null) {
            throw new BizException(ErrorCode.COMMON_PARAM_INVALID, "配置模板更新命令不能为空");
        }
        String profileCode = normalizeRequired(command.getProfileCode(), "profileCode 不能为空");
        Map<String, String> normalizedProfileOptions = parseAndNormalizeProfileJson(command.getProfileJson());
        ensureDefaultRecords();

        long now = System.currentTimeMillis();
        EngineRuntimeProfileDO existingProfile = engineRuntimeProfileRepository.findByCode(profileCode).orElse(null);
        EngineRuntimeProfileDO profileDO = existingProfile == null ? new EngineRuntimeProfileDO() : existingProfile;
        profileDO.setProfileCode(profileCode);
        profileDO.setProfileName(existingProfile == null ? buildProfileName(profileCode) : existingProfile.getProfileName());
        profileDO.setTrackerSetCode(resolveTrackerSetCode(existingProfile));
        profileDO.setApplyScope(existingProfile == null ? "GLOBAL" : existingProfile.getApplyScope());
        profileDO.setProfileJson(writeProfileJson(normalizedProfileOptions));
        profileDO.setEnabled(existingProfile == null ? 1 : defaultInt(existingProfile.getEnabled(), 1));
        profileDO.setIsDefault(DEFAULT_PROFILE_CODE.equals(profileCode) ? 1 : defaultInt(profileDO.getIsDefault(), 0));
        profileDO.setVersion(defaultInt(profileDO.getVersion(), 0) + 1);
        profileDO.setCreatedAt(existingProfile == null ? now : existingProfile.getCreatedAt());
        profileDO.setUpdatedAt(now);
        engineRuntimeProfileRepository.saveOrUpdate(profileDO);

        UpdateDownloadConfigCommand updateDownloadConfigCommand = new UpdateDownloadConfigCommand();
        updateDownloadConfigCommand.setActiveEngineProfileCode(profileCode);
        configService.updateConfig(updateDownloadConfigCommand);
        LOGGER.info("更新引擎运行配置模板成功: profileCode={}", profileCode);
        return buildSnapshot(APPLY_STATUS_UPDATED);
    }

    /**
     * 应用配置模板。
     *
     * @param profileCode 配置模板编码
     * @param forceRestart 是否强制重启
     * @return 应用结果
     */
    @Transactional(rollbackFor = Exception.class)
    public EngineRuntimeApplyResultModel applyProfile(String profileCode, Boolean forceRestart) {
        String normalizedProfileCode = normalizeRequired(profileCode, "profileCode 不能为空");
        ensureDefaultRecords();
        EngineRuntimeProfileDO profileDO = engineRuntimeProfileRepository.findByCode(normalizedProfileCode)
            .orElseThrow(() -> new BizException(ErrorCode.ENGINE_PROFILE_INVALID, "配置模板不存在: " + normalizedProfileCode));
        Map<String, String> profileOptions = parseAndNormalizeProfileJson(profileDO.getProfileJson());

        Map<String, String> hotOptions = new LinkedHashMap<String, String>();
        List<String> restartOnlyKeys = new ArrayList<String>();
        for (Map.Entry<String, String> entry : profileOptions.entrySet()) {
            if (HOT_UPDATE_OPTION_KEYS.contains(entry.getKey())) {
                hotOptions.put(entry.getKey(), entry.getValue());
                continue;
            }
            restartOnlyKeys.add(entry.getKey());
        }
        if (!hotOptions.isEmpty()) {
            aria2RpcClient.changeGlobalOption(hotOptions);
        }
        UpdateDownloadConfigCommand updateDownloadConfigCommand = new UpdateDownloadConfigCommand();
        updateDownloadConfigCommand.setActiveEngineProfileCode(normalizedProfileCode);
        configService.updateConfig(updateDownloadConfigCommand);

        boolean restartRequired = !restartOnlyKeys.isEmpty() || Boolean.TRUE.equals(forceRestart);
        EngineRuntimeApplyResultModel resultModel = new EngineRuntimeApplyResultModel();
        resultModel.setProfileCode(normalizedProfileCode);
        resultModel.setRestartRequired(restartRequired);
        if (restartOnlyKeys.isEmpty()) {
            resultModel.setApplyStatus(APPLY_STATUS_APPLIED);
        } else if (!hotOptions.isEmpty()) {
            resultModel.setApplyStatus(APPLY_STATUS_PARTIALLY_APPLIED);
        } else {
            resultModel.setApplyStatus(APPLY_STATUS_RESTART_REQUIRED);
        }
        LOGGER.info("应用引擎运行配置模板完成: profileCode={}, hotOptionSize={}, restartOnlySize={}, forceRestart={}",
            normalizedProfileCode, hotOptions.size(), restartOnlyKeys.size(), Boolean.TRUE.equals(forceRestart));
        return resultModel;
    }

    private EngineRuntimeSnapshotModel buildSnapshot(String applyStatus) {
        ensureDefaultRecords();
        DownloadConfigDO configDO = downloadConfigRepository.findSingleton()
            .orElseThrow(() -> new BizException(ErrorCode.INTERNAL_ERROR, "默认下载配置不存在"));
        List<EngineRuntimeProfileItemModel> profileItems = new ArrayList<EngineRuntimeProfileItemModel>();
        for (EngineRuntimeProfileDO profileDO : engineRuntimeProfileRepository.findAll()) {
            EngineRuntimeProfileItemModel itemModel = new EngineRuntimeProfileItemModel();
            itemModel.setProfileCode(profileDO.getProfileCode());
            itemModel.setProfileName(profileDO.getProfileName());
            itemModel.setTrackerSetCode(profileDO.getTrackerSetCode());
            itemModel.setProfileJson(profileDO.getProfileJson());
            itemModel.setIsDefault(defaultInt(profileDO.getIsDefault(), 0) == 1);
            itemModel.setEnabled(defaultInt(profileDO.getEnabled(), 1) == 1);
            profileItems.add(itemModel);
        }
        EngineRuntimeSnapshotModel snapshotModel = new EngineRuntimeSnapshotModel();
        snapshotModel.setActiveProfileCode(resolveActiveProfileCode(configDO));
        snapshotModel.setProfiles(profileItems);
        snapshotModel.setApplyStatus(applyStatus);
        return snapshotModel;
    }

    private void ensureDefaultRecords() {
        long now = System.currentTimeMillis();
        if (!btTrackerSetRepository.findByCode(DEFAULT_TRACKER_SET_CODE).isPresent()) {
            BtTrackerSetDO trackerSetDO = new BtTrackerSetDO();
            trackerSetDO.setTrackerSetCode(DEFAULT_TRACKER_SET_CODE);
            trackerSetDO.setTrackerSetName(DEFAULT_TRACKER_SET_NAME);
            trackerSetDO.setSourceType("BT");
            trackerSetDO.setTrackerListText(DEFAULT_TRACKER_LIST_TEXT);
            trackerSetDO.setTrackerSourceUrl(null);
            trackerSetDO.setIsBuiltin(1);
            trackerSetDO.setCreatedAt(now);
            trackerSetDO.setUpdatedAt(now);
            btTrackerSetRepository.saveOrUpdate(trackerSetDO);
        } else {
            BtTrackerSetDO existingTrackerSet = btTrackerSetRepository.findByCode(DEFAULT_TRACKER_SET_CODE).get();
            if (!StringUtils.hasText(existingTrackerSet.getTrackerListText())) {
                existingTrackerSet.setTrackerListText(DEFAULT_TRACKER_LIST_TEXT);
                existingTrackerSet.setUpdatedAt(now);
                btTrackerSetRepository.saveOrUpdate(existingTrackerSet);
            }
        }
        if (!engineRuntimeProfileRepository.findByCode(DEFAULT_PROFILE_CODE).isPresent()) {
            EngineRuntimeProfileDO profileDO = new EngineRuntimeProfileDO();
            profileDO.setProfileCode(DEFAULT_PROFILE_CODE);
            profileDO.setProfileName(DEFAULT_PROFILE_NAME);
            profileDO.setTrackerSetCode(DEFAULT_TRACKER_SET_CODE);
            profileDO.setApplyScope("GLOBAL");
            profileDO.setProfileJson(DEFAULT_PROFILE_JSON);
            profileDO.setEnabled(1);
            profileDO.setIsDefault(1);
            profileDO.setVersion(0);
            profileDO.setCreatedAt(now);
            profileDO.setUpdatedAt(now);
            engineRuntimeProfileRepository.saveOrUpdate(profileDO);
        } else {
            EngineRuntimeProfileDO existingProfile = engineRuntimeProfileRepository.findByCode(DEFAULT_PROFILE_CODE).get();
            if (!StringUtils.hasText(existingProfile.getProfileJson())
                || "{}".equals(existingProfile.getProfileJson().trim())) {
                existingProfile.setProfileJson(DEFAULT_PROFILE_JSON);
                existingProfile.setUpdatedAt(now);
                engineRuntimeProfileRepository.saveOrUpdate(existingProfile);
            }
        }
        DownloadConfigDO configDO = downloadConfigRepository.findSingleton()
            .orElseThrow(() -> new BizException(ErrorCode.INTERNAL_ERROR, "默认下载配置不存在"));
        if (!StringUtils.hasText(configDO.getActiveEngineProfileCode())) {
            configDO.setActiveEngineProfileCode(DEFAULT_PROFILE_CODE);
            configDO.setUpdatedAt(now);
            downloadConfigRepository.saveOrUpdate(configDO);
        }
    }

    private String resolveActiveProfileCode(DownloadConfigDO configDO) {
        return StringUtils.hasText(configDO.getActiveEngineProfileCode())
            ? configDO.getActiveEngineProfileCode().trim()
            : DEFAULT_PROFILE_CODE;
    }

    private Map<String, String> parseAndNormalizeProfileJson(String profileJson) {
        String normalizedJson = normalizeRequired(profileJson, "profileJson 不能为空");
        try {
            Map<String, Object> rawMap = objectMapper.readValue(normalizedJson, PROFILE_JSON_TYPE);
            Map<String, String> normalizedMap = new LinkedHashMap<String, String>();
            for (Map.Entry<String, Object> entry : rawMap.entrySet()) {
                if (!StringUtils.hasText(entry.getKey()) || entry.getValue() == null) {
                    continue;
                }
                normalizedMap.put(entry.getKey().trim(), String.valueOf(entry.getValue()).trim());
            }
            return normalizedMap;
        } catch (IOException exception) {
            throw new BizException(ErrorCode.ENGINE_PROFILE_INVALID, "profileJson 必须是合法 JSON 对象");
        }
    }

    private String writeProfileJson(Map<String, String> profileOptions) {
        try {
            return objectMapper.writeValueAsString(profileOptions);
        } catch (IOException exception) {
            throw new BizException(ErrorCode.ENGINE_PROFILE_INVALID, "序列化配置模板失败");
        }
    }

    private String normalizeRequired(String value, String errorMessage) {
        if (!StringUtils.hasText(value)) {
            throw new BizException(ErrorCode.COMMON_PARAM_INVALID, errorMessage);
        }
        return value.trim();
    }

    private String buildProfileName(String profileCode) {
        return DEFAULT_PROFILE_CODE.equals(profileCode) ? DEFAULT_PROFILE_NAME : profileCode;
    }

    private String resolveTrackerSetCode(EngineRuntimeProfileDO existingProfile) {
        if (existingProfile != null && StringUtils.hasText(existingProfile.getTrackerSetCode())) {
            return existingProfile.getTrackerSetCode().trim();
        }
        return DEFAULT_TRACKER_SET_CODE;
    }

    private int defaultInt(Integer value, int defaultValue) {
        return value == null ? defaultValue : value;
    }
}
