package com.mooddownload.local.dal.provider;

import com.mooddownload.local.mapper.provider.ExternalProviderSessionDO;
import com.mooddownload.local.mapper.provider.ExternalProviderSessionMapper;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/**
 * 外部 Provider 会话 Repository。
 */
@Repository
public class ExternalProviderSessionRepository {

    private final ExternalProviderSessionMapper externalProviderSessionMapper;

    public ExternalProviderSessionRepository(ExternalProviderSessionMapper externalProviderSessionMapper) {
        this.externalProviderSessionMapper = externalProviderSessionMapper;
    }

    public Optional<ExternalProviderSessionDO> findBySessionKey(String sessionKey) {
        return Optional.ofNullable(externalProviderSessionMapper.selectBySessionKey(sessionKey));
    }

    public List<ExternalProviderSessionDO> findByProviderCode(String providerCode) {
        return externalProviderSessionMapper.selectByProviderCode(providerCode);
    }

    public void saveOrUpdate(ExternalProviderSessionDO sessionDO) {
        if (externalProviderSessionMapper.selectBySessionKey(sessionDO.getSessionKey()) == null) {
            externalProviderSessionMapper.insert(sessionDO);
            return;
        }
        externalProviderSessionMapper.updateBySessionKey(sessionDO);
    }
}
