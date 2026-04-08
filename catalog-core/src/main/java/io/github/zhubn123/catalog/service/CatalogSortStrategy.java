package io.github.zhubn123.catalog.service;

import io.github.zhubn123.catalog.domain.CatalogNode;

import java.util.List;
import java.util.Map;

/**
 * 目录节点排序策略 SPI。
 *
 * <p>当前开放的是“数值型 sort 键”策略扩展点：
 * 默认提供 gap 排序，使用方也可以通过 Spring Bean 注入自定义整数排序策略。</p>
 */
public interface CatalogSortStrategy {

    /**
     * 策略名称，用于日志、文档和配置说明。
     */
    String name();

    /**
     * 归一化单个 sort 值，便于兼容历史脏数据或空值。
     */
    int normalizeSort(Integer sort);

    /**
     * 计算同级末尾追加时的新 sort 值。
     */
    int nextAppendSort(Integer maxSort);

    /**
     * 在同级节点列表中为目标位置分配 sort 值。
     *
     * <p>当当前间隙不足时返回 {@code null}，由调用方决定是否先执行重排再重试。</p>
     */
    Integer resolveTargetSort(List<CatalogNode> siblings, int targetIndex);

    /**
     * 对一组兄弟节点执行重排，返回需要更新的 nodeId -> sort 映射。
     *
     * <p>实现可以同步更新传入节点列表中的 sort，便于调用方在同一批数据上继续后续计算。</p>
     */
    Map<Long, Integer> rebalance(List<CatalogNode> siblings);
}