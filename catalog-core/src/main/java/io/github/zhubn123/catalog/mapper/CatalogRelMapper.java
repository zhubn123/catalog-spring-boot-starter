package io.github.zhubn123.catalog.mapper;

import io.github.zhubn123.catalog.domain.CatalogRel;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 业务绑定关系数据访问接口。
 *
 * @author zhubn
 * @date 2026/4/2
 */
@Mapper
public interface CatalogRelMapper {

    void insert(CatalogRel rel);

    void batchInsert(List<CatalogRel> rels);

    void delete(Long nodeId, String bizId, String bizType);

    void deleteByNodeIds(List<Long> nodeIds);

    List<CatalogRel> selectByBiz(String bizId, String bizType);

    List<CatalogRel> selectByNode(Long nodeId, String bizType);

    List<String> selectBizIdsByNodeIds(List<Long> nodeIds, String bizType);

    Integer countByNodeIds(List<Long> nodeIds);
}
