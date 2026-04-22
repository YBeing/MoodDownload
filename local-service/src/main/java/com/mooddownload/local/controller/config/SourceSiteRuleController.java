package com.mooddownload.local.controller.config;

import com.mooddownload.local.common.response.ApiResponse;
import com.mooddownload.local.controller.config.convert.ConfigControllerConverter;
import com.mooddownload.local.controller.config.vo.SourceSiteRuleResponse;
import com.mooddownload.local.controller.config.vo.UpdateSourceSiteRuleRequest;
import com.mooddownload.local.service.config.SourceSiteRuleService;
import com.mooddownload.local.service.config.model.SourceSiteRuleModel;
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
 * 站点规则接口控制器。
 */
@RestController
@RequestMapping("/api/config/site-rules")
public class SourceSiteRuleController {

    private final SourceSiteRuleService sourceSiteRuleService;

    private final ConfigControllerConverter configControllerConverter;

    public SourceSiteRuleController(
        SourceSiteRuleService sourceSiteRuleService,
        ConfigControllerConverter configControllerConverter
    ) {
        this.sourceSiteRuleService = sourceSiteRuleService;
        this.configControllerConverter = configControllerConverter;
    }

    /**
     * 查询全部站点规则。
     *
     * @return 统一响应
     */
    @GetMapping
    public ApiResponse<java.util.List<SourceSiteRuleResponse>> listSourceSiteRules() {
        List<SourceSiteRuleResponse> responseList = new ArrayList<SourceSiteRuleResponse>();
        for (SourceSiteRuleModel ruleModel : sourceSiteRuleService.listAll()) {
            responseList.add(configControllerConverter.toSourceSiteRuleResponse(ruleModel));
        }
        return ApiResponse.success(responseList);
    }

    /**
     * 更新站点规则。
     *
     * @param ruleId 规则 ID
     * @param request 更新请求
     * @return 统一响应
     */
    @PutMapping("/{id}")
    public ApiResponse<SourceSiteRuleResponse> updateSourceSiteRule(
        @PathVariable("id") Long ruleId,
        @Valid @RequestBody UpdateSourceSiteRuleRequest request
    ) {
        return ApiResponse.success(configControllerConverter.toSourceSiteRuleResponse(
            sourceSiteRuleService.update(configControllerConverter.toUpdateSourceSiteRuleCommand(ruleId, request))
        ));
    }
}
