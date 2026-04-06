package io.github.zhubn123.catalog.autoconfigure;

import io.github.zhubn123.catalog.domain.CatalogNode;
import io.github.zhubn123.catalog.exception.CatalogException;
import io.github.zhubn123.catalog.service.CatalogService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * 目录管理 REST API 控制器。
 *
 * <p>提供目录树常用的增删改查与业务绑定接口。写接口优先接收 JSON body，
 * 同时兼容旧的 query/form 参数，便于渐进迁移现有调用方。</p>
 *
 * @author zhubn
 * @date 2026/4/2
 */
@RestController
@RequestMapping("/catalog")
public class CatalogController {

    private final CatalogService catalogService;

    public CatalogController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    /**
     * 新增单个目录节点。
     */
    @PostMapping("/node")
    public Long addNode(@RequestBody(required = false) AddNodeRequest request, Long parentId, String name) {
        Long effectiveParentId = request != null && request.parentId() != null ? request.parentId() : parentId;
        String effectiveName = request != null && request.name() != null ? request.name() : name;
        return catalogService.addNode(effectiveParentId, requireText(effectiveName, "name"));
    }

    /**
     * 批量新增同级节点。
     *
     * <p>推荐使用 JSON body 中的 {@code names} 数组；旧的逗号分隔字符串参数仍保留兼容。</p>
     */
    @PostMapping("/node/batch")
    public List<Long> batchAddNode(@RequestBody(required = false) BatchAddNodeRequest request, Long parentId, String names) {
        Long effectiveParentId = request != null && request.parentId() != null ? request.parentId() : parentId;
        List<String> effectiveNames = request != null && request.names() != null
                ? normalizeStringList(request.names())
                : splitCsv(names);
        List<String> validatedNames = requireNonEmptyStrings(effectiveNames, "names");
        return catalogService.batchAddNode(effectiveParentId, validatedNames.toArray(String[]::new));
    }

    /**
     * 移动节点到新的父节点或同级位置。
     */
    @PostMapping("/move")
    public void move(@RequestBody(required = false) MoveNodeRequest request, Long nodeId, Long parentId, Integer targetIndex) {
        Long effectiveNodeId = request != null && request.nodeId() != null ? request.nodeId() : nodeId;
        Long effectiveParentId = request != null && request.parentId() != null ? request.parentId() : parentId;
        Integer effectiveTargetIndex = request != null && request.targetIndex() != null ? request.targetIndex() : targetIndex;
        catalogService.moveNode(
                requirePositiveId(effectiveNodeId, "nodeId"),
                effectiveParentId,
                requireNonNegative(effectiveTargetIndex, "targetIndex")
        );
    }

    /**
     * 更新节点基础信息。
     *
     * <p>当传入 {@code sort} 时，会复用移动逻辑调整同级顺序。</p>
     */
    @PostMapping("/node/update")
    public void updateNode(
            @RequestBody(required = false) UpdateNodeRequest request,
            Long nodeId,
            String name,
            String code,
            Integer sort
    ) {
        Long effectiveNodeId = request != null && request.nodeId() != null ? request.nodeId() : nodeId;
        String effectiveName = trimToNull(request != null && request.name() != null ? request.name() : name);
        String effectiveCode = trimToNull(request != null && request.code() != null ? request.code() : code);
        Integer effectiveSort = requireNonNegative(
                request != null && request.sort() != null ? request.sort() : sort,
                "sort"
        );
        if (effectiveName == null && effectiveCode == null && effectiveSort == null) {
            throw CatalogException.invalidArgument("至少提供一个可更新字段");
        }
        catalogService.updateNode(requirePositiveId(effectiveNodeId, "nodeId"), effectiveName, effectiveCode, effectiveSort);
    }

    /**
     * 删除节点。
     *
     * <p>默认按非递归语义校验；当 {@code recursive=true} 时，会连同子树一并删除。</p>
     */
    @PostMapping("/node/delete")
    public void deleteNode(@RequestBody(required = false) DeleteNodeRequest request, Long nodeId, Boolean recursive) {
        Long effectiveNodeId = request != null && request.nodeId() != null ? request.nodeId() : nodeId;
        Boolean effectiveRecursive = request != null && request.recursive() != null ? request.recursive() : recursive;
        catalogService.deleteNode(requirePositiveId(effectiveNodeId, "nodeId"), Boolean.TRUE.equals(effectiveRecursive));
    }

    /**
     * 绑定单个业务对象到单个目录节点。
     */
    @PostMapping("/bind")
    public void bind(@RequestBody(required = false) BindRequest request, Long nodeId, String bizId, String bizType) {
        catalogService.bind(
                requirePositiveId(resolveNodeId(request, nodeId), "nodeId"),
                requireText(resolveBizId(request, bizId), "bizId"),
                requireText(resolveBizType(request, bizType), "bizType")
        );
    }

    /**
     * 兼容保留的旧批量绑定入口。
     *
     * <p>当前单个业务对象只允许绑定一个节点，因此这里只接受一个有效节点；
     * 多组一对一绑定请使用 {@link #batchBindPairs(BatchBindPairsRequest, String, String, String)}。</p>
     */
    @PostMapping("/bind/batch")
    @Deprecated
    public void batchBind(@RequestBody(required = false) BatchBindRequest request, String nodeIds, String bizId, String bizType) {
        List<Long> nodeIdList = request != null && request.nodeIds() != null
                ? normalizeLongList(request.nodeIds())
                : parseCsvLongList(nodeIds);
        String effectiveBizId = request != null && request.bizId() != null ? request.bizId() : bizId;
        String effectiveBizType = request != null && request.bizType() != null ? request.bizType() : bizType;
        catalogService.batchBind(
                requirePositiveIds(nodeIdList, "nodeIds"),
                requireText(effectiveBizId, "bizId"),
                requireText(effectiveBizType, "bizType")
        );
    }

    /**
     * 按顺序执行多组一对一业务绑定。
     *
     * <p>{@code nodeIds[i]} 会与 {@code bizIds[i]} 配对绑定，推荐使用 JSON body 数组调用。</p>
     */
    @PostMapping("/bind/pairs")
    public void batchBindPairs(
            @RequestBody(required = false) BatchBindPairsRequest request,
            String nodeIds,
            String bizIds,
            String bizType
    ) {
        List<Long> nodeIdList = request != null && request.nodeIds() != null
                ? normalizeLongList(request.nodeIds())
                : parseCsvLongList(nodeIds);
        List<String> bizIdList = request != null && request.bizIds() != null
                ? normalizeStringList(request.bizIds())
                : splitCsv(bizIds);
        String effectiveBizType = request != null && request.bizType() != null ? request.bizType() : bizType;
        List<Long> validatedNodeIds = requirePositiveIds(nodeIdList, "nodeIds");
        List<String> validatedBizIds = requireNonEmptyStrings(bizIdList, "bizIds");
        requireSameSize(validatedNodeIds, "nodeIds", validatedBizIds, "bizIds");
        catalogService.batchBindByBizIds(validatedNodeIds, validatedBizIds, requireText(effectiveBizType, "bizType"));
    }

    /**
     * 解绑单个节点与业务对象之间的关系。
     */
    @PostMapping("/unbind")
    public void unbind(@RequestBody(required = false) BindRequest request, Long nodeId, String bizId, String bizType) {
        catalogService.unbind(
                requirePositiveId(resolveNodeId(request, nodeId), "nodeId"),
                requireText(resolveBizId(request, bizId), "bizId"),
                requireText(resolveBizType(request, bizType), "bizType")
        );
    }

    /**
     * 返回完整目录的扁平节点列表，顺序与树前序遍历一致。
     *
     * <p>前端如需真正的嵌套树结构，应基于返回列表自行组装。</p>
     */
    @GetMapping("/nodes")
    public List<CatalogNode> nodes() {
        return catalogService.listNodesInTreeOrder();
    }

    /**
     * 兼容旧命名，返回值仍是扁平节点列表，而不是已经组装好的嵌套树。
     */
    @GetMapping("/tree")
    @Deprecated
    public List<CatalogNode> tree() {
        return catalogService.listNodesInTreeOrder();
    }

    /**
     * 查询业务对象绑定节点的唯一路径。
     */
    @GetMapping("/bizPath")
    public List<CatalogNode> bizPath(String bizId, String bizType) {
        return catalogService.getBizPath(requireText(bizId, "bizId"), requireText(bizType, "bizType"));
    }

    /**
     * 查询业务对象当前绑定的节点 ID。
     */
    @GetMapping("/bizNodes")
    public List<Long> bizNodes(String bizId, String bizType) {
        return catalogService.getNodeIds(requireText(bizId, "bizId"), requireText(bizType, "bizType"));
    }

    /**
     * 查询指定节点子树下绑定的业务 ID。
     */
    @GetMapping("/nodeBiz")
    public List<String> nodeBiz(Long nodeId, String bizType) {
        return catalogService.getBizIdsByNodeTree(requirePositiveId(nodeId, "nodeId"), requireText(bizType, "bizType"));
    }

    /**
     * 返回用于还原业务局部树的扁平节点列表。
     */
    @GetMapping("/bizTreeNodes")
    public List<CatalogNode> bizTreeNodes(String bizId, String bizType) {
        return catalogService.listBizRelatedNodes(requireText(bizId, "bizId"), requireText(bizType, "bizType"));
    }

    /**
     * 兼容旧命名，返回值仍是扁平节点列表。
     */
    @GetMapping("/bizTree")
    @Deprecated
    public List<CatalogNode> bizTree(String bizId, String bizType) {
        return bizTreeNodes(bizId, bizType);
    }

    /**
     * 返回指定节点子树的扁平节点列表。
     */
    @GetMapping("/subtreeNodes")
    public List<CatalogNode> subtreeNodes(Long nodeId) {
        return catalogService.listSubtreeNodes(requirePositiveId(nodeId, "nodeId"));
    }

    /**
     * 兼容旧命名，返回值仍是扁平节点列表。
     */
    @GetMapping("/subtree")
    @Deprecated
    public List<CatalogNode> subtree(Long nodeId) {
        return subtreeNodes(nodeId);
    }

    /**
     * 统一从 request body 或兼容参数中解析节点 ID。
     */
    private Long resolveNodeId(BindRequest request, Long nodeId) {
        return request != null && request.nodeId() != null ? request.nodeId() : nodeId;
    }

    /**
     * 统一从 request body 或兼容参数中解析业务 ID。
     */
    private String resolveBizId(BindRequest request, String bizId) {
        return request != null && request.bizId() != null ? request.bizId() : bizId;
    }

    /**
     * 统一从 request body 或兼容参数中解析业务类型。
     */
    private String resolveBizType(BindRequest request, String bizType) {
        return request != null && request.bizType() != null ? request.bizType() : bizType;
    }

    /**
     * 将旧式逗号分隔字符串解析为长整型列表。
     */
    private List<Long> parseCsvLongList(String value) {
        return splitCsv(value).stream()
                .map(item -> {
                    try {
                        return Long.valueOf(item);
                    } catch (NumberFormatException exception) {
                        throw CatalogException.invalidArgument("nodeIds 中包含无效数字: " + item);
                    }
                })
                .toList();
    }

    /**
     * 过滤 body 数组中的空值，避免兼容旧前端时把空元素继续传给 service 层。
     */
    private List<Long> normalizeLongList(List<Long> values) {
        return values.stream()
                .filter(Objects::nonNull)
                .toList();
    }

    /**
     * 对兼容 query/form 的写接口统一做必填校验，避免出现“请求进来了但什么都没做”的静默行为。
     */
    private Long requirePositiveId(Long value, String fieldName) {
        if (value == null || value <= 0) {
            throw CatalogException.invalidArgument(fieldName + " 不能为空且必须大于 0");
        }
        return value;
    }

    /**
     * 统一收口文本参数校验，让 body 与旧 query/form 参数返回一致的错误提示。
     */
    private String requireText(String value, String fieldName) {
        String normalizedValue = trimToNull(value);
        if (normalizedValue == null) {
            throw CatalogException.invalidArgument(fieldName + " 不能为空");
        }
        return normalizedValue;
    }

    /**
     * 批量接口至少要有一条有效数据，否则前端很难区分“空操作成功”还是“参数漏传”。
     */
    private List<String> requireNonEmptyStrings(List<String> values, String fieldName) {
        if (values == null || values.isEmpty()) {
            throw CatalogException.invalidArgument(fieldName + " 不能为空");
        }
        return values;
    }

    /**
     * 节点 ID 列表既要非空，也要保证每一项都是合法正整数。
     */
    private List<Long> requirePositiveIds(List<Long> values, String fieldName) {
        if (values == null || values.isEmpty()) {
            throw CatalogException.invalidArgument(fieldName + " 不能为空");
        }
        boolean hasInvalidValue = values.stream().anyMatch(value -> value == null || value <= 0);
        if (hasInvalidValue) {
            throw CatalogException.invalidArgument(fieldName + " 中存在无效节点 ID");
        }
        return values;
    }

    /**
     * 成对批量绑定要求两个数组严格一一对应，避免 service 层再猜测调用方意图。
     */
    private void requireSameSize(List<?> left, String leftName, List<?> right, String rightName) {
        if (left.size() != right.size()) {
            throw CatalogException.invalidArgument(leftName + " 和 " + rightName + " 的长度必须一致");
        }
    }

    /**
     * 排序位置类参数允许缺省，但不接受负数，避免把无效索引继续传入核心逻辑。
     */
    private Integer requireNonNegative(Integer value, String fieldName) {
        if (value != null && value < 0) {
            throw CatalogException.invalidArgument(fieldName + " 不能小于 0");
        }
        return value;
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    /**
     * 兼容旧参数风格，将逗号分隔字符串拆成列表后交给统一清洗逻辑。
     */
    private List<String> splitCsv(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return normalizeStringList(Arrays.asList(value.split(",")));
    }

    /**
     * 统一去掉空白和空串，保证 body 数组与 CSV 参数的清洗规则一致。
     */
    private List<String> normalizeStringList(List<String> values) {
        return values.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(item -> !item.isEmpty())
                .toList();
    }

    /**
     * 单节点新增请求。
     */
    public record AddNodeRequest(Long parentId, String name) {
    }

    /**
     * 批量新增请求。
     */
    public record BatchAddNodeRequest(Long parentId, List<String> names) {
    }

    /**
     * 节点移动请求。
     */
    public record MoveNodeRequest(Long nodeId, Long parentId, Integer targetIndex) {
    }

    /**
     * 节点更新请求。
     */
    public record UpdateNodeRequest(Long nodeId, String name, String code, Integer sort) {
    }

    /**
     * 节点删除请求。
     */
    public record DeleteNodeRequest(Long nodeId, Boolean recursive) {
    }

    /**
     * 单业务对象绑定或解绑请求。
     */
    public record BindRequest(Long nodeId, String bizId, String bizType) {
    }

    /**
     * 兼容旧批量绑定请求。
     */
    public record BatchBindRequest(List<Long> nodeIds, String bizId, String bizType) {
    }

    /**
     * 一对一批量绑定请求。
     */
    public record BatchBindPairsRequest(List<Long> nodeIds, List<String> bizIds, String bizType) {
    }
}
