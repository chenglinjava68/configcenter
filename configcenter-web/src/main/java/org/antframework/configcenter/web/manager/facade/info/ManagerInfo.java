/* 
 * 作者：钟勋 (e-mail:zhongxunking@163.com)
 */

/*
 * 修订记录:
 * @author 钟勋 2017-09-19 21:35 创建
 */
package org.antframework.configcenter.web.manager.facade.info;


import org.antframework.common.util.tostring.ToString;
import org.antframework.configcenter.web.manager.facade.enums.ManagerType;

import java.io.Serializable;

/**
 * 管理员信息
 */
public class ManagerInfo implements Serializable {
    // 管理员编码
    private String managerCode;
    // 名称
    private String name;
    // 类型
    private ManagerType type;

    public String getManagerCode() {
        return managerCode;
    }

    public void setManagerCode(String managerCode) {
        this.managerCode = managerCode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ManagerType getType() {
        return type;
    }

    public void setType(ManagerType type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return ToString.toString(this);
    }
}
