package com.mooddownload.local.dal.profile;

import com.mooddownload.local.mapper.profile.EngineRuntimeProfileDO;
import com.mooddownload.local.mapper.profile.EngineRuntimeProfileMapper;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/**
 * 引擎运行配置模板 Repository。
 */
@Repository
public class EngineRuntimeProfileRepository {

    private final EngineRuntimeProfileMapper engineRuntimeProfileMapper;

    public EngineRuntimeProfileRepository(EngineRuntimeProfileMapper engineRuntimeProfileMapper) {
        this.engineRuntimeProfileMapper = engineRuntimeProfileMapper;
    }

    public Optional<EngineRuntimeProfileDO> findByCode(String profileCode) {
        return Optional.ofNullable(engineRuntimeProfileMapper.selectByCode(profileCode));
    }

    public List<EngineRuntimeProfileDO> findAll() {
        return engineRuntimeProfileMapper.selectAll();
    }

    public void saveOrUpdate(EngineRuntimeProfileDO profileDO) {
        if (engineRuntimeProfileMapper.selectByCode(profileDO.getProfileCode()) == null) {
            engineRuntimeProfileMapper.insert(profileDO);
            return;
        }
        engineRuntimeProfileMapper.updateByCode(profileDO);
    }
}
