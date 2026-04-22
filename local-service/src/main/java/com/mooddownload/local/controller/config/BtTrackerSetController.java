package com.mooddownload.local.controller.config;

import com.mooddownload.local.common.response.ApiResponse;
import com.mooddownload.local.controller.config.convert.ConfigControllerConverter;
import com.mooddownload.local.controller.config.vo.BtTrackerSetResponse;
import com.mooddownload.local.controller.config.vo.UpdateBtTrackerSetRequest;
import com.mooddownload.local.service.config.BtTrackerSetService;
import com.mooddownload.local.service.config.model.BtTrackerSetModel;
import java.util.ArrayList;
import java.util.List;
import javax.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Tracker 配置集接口控制器。
 */
@RestController
@RequestMapping("/api/config/tracker-sets")
public class BtTrackerSetController {

    private final BtTrackerSetService btTrackerSetService;

    private final ConfigControllerConverter configControllerConverter;

    public BtTrackerSetController(
        BtTrackerSetService btTrackerSetService,
        ConfigControllerConverter configControllerConverter
    ) {
        this.btTrackerSetService = btTrackerSetService;
        this.configControllerConverter = configControllerConverter;
    }

    /**
     * 查询全部 Tracker 集。
     *
     * @return 统一响应
     */
    @GetMapping
    public ApiResponse<java.util.List<BtTrackerSetResponse>> listTrackerSets() {
        List<BtTrackerSetResponse> responseList = new ArrayList<BtTrackerSetResponse>();
        for (BtTrackerSetModel trackerSetModel : btTrackerSetService.listAll()) {
            responseList.add(configControllerConverter.toBtTrackerSetResponse(trackerSetModel));
        }
        return ApiResponse.success(responseList);
    }

    /**
     * 更新 Tracker 集。
     *
     * @param trackerSetCode 编码
     * @param request 更新请求
     * @return 统一响应
     */
    @PutMapping("/{code}")
    public ApiResponse<BtTrackerSetResponse> updateTrackerSet(
        @PathVariable("code") String trackerSetCode,
        @Valid @RequestBody UpdateBtTrackerSetRequest request
    ) {
        return ApiResponse.success(configControllerConverter.toBtTrackerSetResponse(
            btTrackerSetService.update(configControllerConverter.toUpdateBtTrackerSetCommand(trackerSetCode, request))
        ));
    }
}
