package io.github.zhubn123.catalog.domain;

import java.util.List;

/**
 * 通用分页结果。
 *
 * <p>用于目录查询这类只读接口，统一返回分页参数、总量和当前页数据，
 * 避免每个分页接口都重复定义响应结构。</p>
 *
 * @param <T> 当前页数据元素类型
 */
public class CatalogPage<T> {

    private final int page;
    private final int size;
    private final long total;
    private final boolean hasNext;
    private final List<T> items;

    public CatalogPage(int page, int size, long total, boolean hasNext, List<T> items) {
        this.page = page;
        this.size = size;
        this.total = total;
        this.hasNext = hasNext;
        this.items = List.copyOf(items);
    }

    public int getPage() {
        return page;
    }

    public int getSize() {
        return size;
    }

    public long getTotal() {
        return total;
    }

    public boolean isHasNext() {
        return hasNext;
    }

    public List<T> getItems() {
        return items;
    }
}
