package io.github.zhubn123.catalog.mapper;

import io.github.zhubn123.catalog.domain.CatalogNode;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 目录节点数据访问接口。
 *
 * @author zhubn
 * @date 2026/4/2
 */
@Mapper
public interface CatalogNodeMapper {

    void insert(CatalogNode node);

    void batchInsert(List<CatalogNode> nodes);

    CatalogNode selectById(Long id);

    List<CatalogNode> selectByIds(List<Long> ids);

    List<CatalogNode> selectAll();

    List<CatalogNode> selectByParentId(Long parentId);

    List<CatalogNode> selectByPathPrefix(String path);

    Integer selectMaxSortByParent(Long parentId);

    Integer countChildren(Long nodeId);

    List<Long> selectIdsHavingChildren(List<Long> nodeIds);

    void updatePath(Long id, String path);

    void batchUpdatePath(List<CatalogNode> nodes);

    void updateBasic(Long id, String name, String code);

    void updateSort(Long id, Integer sort);

    void updateParentLevelPathSort(Long id, Long parentId, Integer level, String path, Integer sort);

    void moveSubtree(String oldPath, String newPath, int levelDelta);

    void decrementSortAfter(Long parentId, int afterSort);

    void incrementSortFrom(Long parentId, int fromSort);

    void decrementSortRange(Long parentId, int fromSort, int toSort, Long excludeId);

    void incrementSortRange(Long parentId, int fromSort, int toSort, Long excludeId);

    void deleteByIds(List<Long> ids);
}
