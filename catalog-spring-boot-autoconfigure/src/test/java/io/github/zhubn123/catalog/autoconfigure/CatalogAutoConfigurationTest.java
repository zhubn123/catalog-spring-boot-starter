package io.github.zhubn123.catalog.autoconfigure;

import io.github.zhubn123.catalog.mapper.CatalogNodeMapper;
import io.github.zhubn123.catalog.mapper.CatalogRelMapper;
import io.github.zhubn123.catalog.service.CatalogSortStrategy;
import io.github.zhubn123.catalog.service.GapCatalogSortStrategy;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CatalogAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(CatalogAutoConfiguration.class))
            .withPropertyValues("catalog.enable-rest-api=false")
            .withUserConfiguration(MinimalMapperConfiguration.class);

    @Test
    void usesGapSortStrategyByDefault() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(CatalogSortStrategy.class);
            assertThat(context.getBean(CatalogSortStrategy.class)).isInstanceOf(GapCatalogSortStrategy.class);
            assertThat(((GapCatalogSortStrategy) context.getBean(CatalogSortStrategy.class)).getStep())
                    .isEqualTo(GapCatalogSortStrategy.DEFAULT_STEP);
        });
    }

    @Test
    void honorsConfiguredGapStep() {
        contextRunner
                .withPropertyValues("catalog.sort.gap-step=128")
                .run(context -> {
                    assertThat(context.getBean(CatalogSortStrategy.class)).isInstanceOf(GapCatalogSortStrategy.class);
                    assertThat(((GapCatalogSortStrategy) context.getBean(CatalogSortStrategy.class)).getStep())
                            .isEqualTo(128);
                });
    }

    @Test
    void backsOffWhenUserProvidesCustomSortStrategyBean() {
        contextRunner
                .withUserConfiguration(CustomSortStrategyConfiguration.class)
                .run(context -> {
                    assertThat(context.getBean(CatalogSortStrategy.class))
                            .isSameAs(context.getBean("customSortStrategy"));
                });
    }

    @Configuration(proxyBeanMethods = false)
    static class MinimalMapperConfiguration {

        @Bean
        CatalogNodeMapper catalogNodeMapper() {
            return Mockito.mock(CatalogNodeMapper.class);
        }

        @Bean
        CatalogRelMapper catalogRelMapper() {
            return Mockito.mock(CatalogRelMapper.class);
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class CustomSortStrategyConfiguration {

        @Bean
        CatalogSortStrategy customSortStrategy() {
            return new CatalogSortStrategy() {
                @Override
                public String name() {
                    return "custom";
                }

                @Override
                public int normalizeSort(Integer sort) {
                    return sort == null ? 100 : sort;
                }

                @Override
                public int nextAppendSort(Integer maxSort) {
                    return maxSort == null ? 100 : maxSort + 100;
                }

                @Override
                public Integer resolveTargetSort(List<io.github.zhubn123.catalog.domain.CatalogNode> siblings, int targetIndex) {
                    return 100;
                }

                @Override
                public Map<Long, Integer> rebalance(List<io.github.zhubn123.catalog.domain.CatalogNode> siblings) {
                    return Collections.emptyMap();
                }
            };
        }
    }
}
