package io.github.zhubn123.catalog.autoconfigure;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 目录模块配置属性。
 *
 * <p>当前仅保留已经真实生效的公开配置项，避免暴露“看起来能配、实际上无效”的参数。</p>
 *
 * @author zhubn
 * @date 2026/4/2
 */
@Data
@ConfigurationProperties(prefix = "catalog")
public class CatalogProperties {

    /**
     * 是否启用 REST API。
     */
    private boolean enableRestApi = true;
}
