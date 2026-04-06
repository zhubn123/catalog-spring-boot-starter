package io.github.zhubn123.catalog.mapper;

import io.github.zhubn123.catalog.domain.CatalogNode;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration;
import org.springframework.boot.autoconfigure.sql.init.SqlInitializationAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.TestPropertySource;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@JdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@ImportAutoConfiguration({
        DataSourceAutoConfiguration.class,
        DataSourceTransactionManagerAutoConfiguration.class,
        JdbcTemplateAutoConfiguration.class,
        SqlInitializationAutoConfiguration.class,
        org.mybatis.spring.boot.autoconfigure.MybatisAutoConfiguration.class
})
@TestPropertySource(properties = {
        "mybatis.mapper-locations=classpath*:mapper/*.xml",
        "spring.sql.init.mode=always",
        "spring.sql.init.schema-locations=classpath:schema-h2.sql"
})
class CatalogNodeMapperTest {

    @Autowired
    private CatalogNodeMapper nodeMapper;

    @Test
    void batchUpdatePathUpdatesMultipleRowsWithoutMultiStatementSql() {
        CatalogNode root = insertNode(0L, "Root", 1, 1);
        CatalogNode child = insertNode(root.getId(), "Child", 2, 1);

        root.setPath("/ROOT");
        child.setPath("/ROOT/" + child.getId());

        nodeMapper.batchUpdatePath(List.of(root, child));

        Map<Long, String> pathById = nodeMapper.selectByIds(List.of(root.getId(), child.getId())).stream()
                .collect(Collectors.toMap(CatalogNode::getId, CatalogNode::getPath));

        assertThat(pathById)
                .containsEntry(root.getId(), "/ROOT")
                .containsEntry(child.getId(), "/ROOT/" + child.getId());
    }

    private CatalogNode insertNode(Long parentId, String name, Integer level, Integer sort) {
        CatalogNode node = new CatalogNode();
        node.setParentId(parentId);
        node.setName(name);
        node.setLevel(level);
        node.setSort(sort);
        nodeMapper.insert(node);
        nodeMapper.updatePath(node.getId(), "/" + node.getId());
        return node;
    }

    @Configuration(proxyBeanMethods = false)
    @MapperScan("io.github.zhubn123.catalog.mapper")
    static class TestConfig {
    }
}
