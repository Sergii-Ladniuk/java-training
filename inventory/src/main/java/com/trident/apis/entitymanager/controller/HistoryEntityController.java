package com.trident.apis.entitymanager.controller;

import com.trident.shared.immigration.history.HistoryEntity;
import com.trident.shared.immigration.history.HistoryService;
import com.trident.shared.immigration.network.SimpleResponse;
import com.trident.shared.immigration.repository.criteria.*;
import com.trident.shared.immigration.util.RestUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.LinkedList;
import java.util.List;

import static com.trident.shared.immigration.constants.ControllerConstants.*;

@RestController
@RequestMapping(path = "/api/v1/history", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
@Api(value = "/api/v1/history", description = "History of entities modifications")
public class HistoryEntityController {

    @Autowired
    private HistoryService historyService;

    @GetMapping
    @ApiOperation(value = "Returns entities history by filters")
    public Page<?> getHistory(
            Pageable pageable,
            @ApiParam(required = false, name = USER_ID, value = FILTER_BY_USER_ID) @RequestParam(required = false) String userId,
            @ApiParam(required = false, name = HISTORY_TYPE, value = FILTER_BY_HISTORY_TYPE) @RequestParam(required = false) String historyType,
            @ApiParam(required = false, name = PARAM_DATE_FROM, value = FILTER_BY_DATE) @RequestParam(required = false) Long dateFrom,
            @ApiParam(required = false, name = PARAM_DATE_TO, value = FILTER_BY_DATE) @RequestParam(required = false) Long dateTo,
            @ApiParam(required = false, name = PARAM_FULL_MODEL, value = DESCRIPTION_IS_FULL_MODEL) @RequestParam(name = PARAM_FULL_MODEL, required = false, defaultValue = "false") Boolean isFullModel) {
        SearchCriteriaList criterias = prepareSearchCriterias(userId, historyType, dateFrom, dateTo);
        return historyService.filterHistory(pageable, criterias, isFullModel);
    }


    @DeleteMapping
    @ApiOperation(value = "Clear history")
    public SimpleResponse clearHistory(HttpServletRequest request) {
        historyService.clearHistory(RestUtils.getUserId(request));
        return new SimpleResponse("OK");
    }

    private SearchCriteriaList prepareSearchCriterias(String userId, String historyType, Long dateFrom, Long dateTo) {
        SearchCriteriaList criterias = new SearchCriteriaList();
        if (!StringUtils.isEmpty(userId)) {
            SearchLikeCriteria c = new SearchLikeCriteria(USER_ID, userId);
            criterias.add(c);
        }
        if (!StringUtils.isEmpty(historyType)) { criterias.add(new SearchEqualCriteria<>(HISTORY_TYPE, historyType)); }
        Long fixedDateTo = dateTo;
        if (dateFrom != null && fixedDateTo == null) {
            fixedDateTo = System.currentTimeMillis();
        }
        if (dateFrom != null && fixedDateTo != null) { criterias.add(new SearchBetweenCriteria("updateDate", dateFrom, fixedDateTo)); }
        return criterias;
    }
}
