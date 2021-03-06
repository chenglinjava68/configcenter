/* 
 * 作者：钟勋 (e-mail:zhongxunking@163.com)
 */

/*
 * 修订记录:
 * @author 钟勋 2017-08-20 15:27 创建
 */
package org.antframework.configcenter.biz.service;

import org.antframework.common.util.facade.FacadeUtils;
import org.antframework.common.util.facade.FacadeUtils.SpringDataPageExtractor;
import org.antframework.configcenter.dal.dao.AppDao;
import org.antframework.configcenter.dal.entity.App;
import org.antframework.configcenter.facade.order.manage.QueryAppOrder;
import org.antframework.configcenter.facade.result.manage.QueryAppResult;
import org.bekit.service.annotation.service.Service;
import org.bekit.service.annotation.service.ServiceExecute;
import org.bekit.service.engine.ServiceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.HashMap;
import java.util.Map;

/**
 * 查询应用服务
 */
@Service
public class QueryAppService {
    @Autowired
    private AppDao appDao;

    @ServiceExecute
    public void execute(ServiceContext<QueryAppOrder, QueryAppResult> context) {
        QueryAppOrder order = context.getOrder();

        Page<App> page = appDao.query(buildSearchParams(order), buildPageable(order));
        FacadeUtils.setQueryResult(context.getResult(), new SpringDataPageExtractor<>(page));
    }

    // 构建查询条件
    private Map<String, Object> buildSearchParams(QueryAppOrder queryAppOrder) {
        Map<String, Object> searchParams = new HashMap<>();
        if (queryAppOrder.getAppCode() != null) {
            searchParams.put("LIKE_appCode", "%" + queryAppOrder.getAppCode() + "%");
        }
        return searchParams;
    }

    // 构建分页
    private Pageable buildPageable(QueryAppOrder queryAppOrder) {
        Sort sort = new Sort(Sort.Direction.DESC, "id");
        return new PageRequest(queryAppOrder.getPageNo() - 1, queryAppOrder.getPageSize(), sort);
    }
}
