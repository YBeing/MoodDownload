package com.mooddownload.local.dal.entry;

import com.mooddownload.local.mapper.entry.ExternalEntryLogDO;
import com.mooddownload.local.mapper.entry.ExternalEntryLogMapper;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/**
 * 外部入口审计 Repository。
 */
@Repository
public class ExternalEntryLogRepository {

    private final ExternalEntryLogMapper externalEntryLogMapper;

    public ExternalEntryLogRepository(ExternalEntryLogMapper externalEntryLogMapper) {
        this.externalEntryLogMapper = externalEntryLogMapper;
    }

    public void insert(ExternalEntryLogDO externalEntryLogDO) {
        externalEntryLogMapper.insert(externalEntryLogDO);
    }

    public Optional<ExternalEntryLogDO> findLatestByClientRequestId(String clientRequestId) {
        return Optional.ofNullable(externalEntryLogMapper.selectLatestByClientRequestId(clientRequestId));
    }
}
