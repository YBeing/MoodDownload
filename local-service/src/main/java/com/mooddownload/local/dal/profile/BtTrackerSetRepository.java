package com.mooddownload.local.dal.profile;

import com.mooddownload.local.mapper.profile.BtTrackerSetDO;
import com.mooddownload.local.mapper.profile.BtTrackerSetMapper;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/**
 * Tracker 配置集 Repository。
 */
@Repository
public class BtTrackerSetRepository {

    private final BtTrackerSetMapper btTrackerSetMapper;

    public BtTrackerSetRepository(BtTrackerSetMapper btTrackerSetMapper) {
        this.btTrackerSetMapper = btTrackerSetMapper;
    }

    public Optional<BtTrackerSetDO> findByCode(String trackerSetCode) {
        return Optional.ofNullable(btTrackerSetMapper.selectByCode(trackerSetCode));
    }

    public List<BtTrackerSetDO> findAll() {
        return btTrackerSetMapper.selectAll();
    }

    public void saveOrUpdate(BtTrackerSetDO trackerSetDO) {
        if (btTrackerSetMapper.selectByCode(trackerSetDO.getTrackerSetCode()) == null) {
            btTrackerSetMapper.insert(trackerSetDO);
            return;
        }
        btTrackerSetMapper.updateByCode(trackerSetDO);
    }
}
