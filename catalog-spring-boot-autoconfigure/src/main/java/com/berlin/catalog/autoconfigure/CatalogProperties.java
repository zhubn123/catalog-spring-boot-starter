package com.berlin.catalog.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 目录模块配置属性
 * 
 * <p>可通过 application.yml 配置：</p>
 * <pre>
 * catalog:
 *   table-prefix: catalog_    # 表名前缀
 *   enable-rest-api: true     # 是否启用REST API
 * </pre>
 * 
 * @author zhubn
 * @date 2026/4/2
 */
@ConfigurationProperties(prefix = "catalog")
public class CatalogProperties {

    /**
     * 数据库表名前缀
     */
    private String tablePrefix = "catalog_";

    /**
     * 是否启用REST API
     */
    private boolean enableRestApi = true;

    /**
     * 是否在启动时初始化数据库表
     */
    private boolean initSchema = false;

    public String getTablePrefix() {
        return tablePrefix;
    }

    public void setTablePrefix(String tablePrefix) {
        this.tablePrefix = tablePrefix;
    }

    public boolean isEnableRestApi() {
        return enableRestApi;
    }

    public void setEnableRestApi(boolean enableRestApi) {
        this.enableRestApi = enableRestApi;
    }

    public boolean isInitSchema() {
        return initSchema;
    }

    public void setInitSchema(boolean initSchema) {
        this.initSchema = initSchema;
    }
}
