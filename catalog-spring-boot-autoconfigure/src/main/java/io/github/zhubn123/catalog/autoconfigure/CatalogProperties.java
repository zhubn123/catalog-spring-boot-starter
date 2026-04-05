package io.github.zhubn123.catalog.autoconfigure;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 目录模块配置属性。
 *
 * <p>可通过 application.yml 中的 catalog 前缀进行配置。</p>
 *
 * @author zhubn
 * @date 2026/4/2
 */
@Data
@ConfigurationProperties(prefix = "catalog")
public class CatalogProperties {

    /**
     * 数据库表名前缀
     */
    private String tablePrefix = "catalog_";

    /**
     * 是否启用 REST API
     */
    private boolean enableRestApi = true;

    /**
     * 是否在启动时初始化数据库
     */
    private boolean initSchema = false;

}
