package com.project.cybershop.pcbuild.internal.service.impl;

import com.project.cybershop.catalog.api.CatalogPublicApi;
import com.project.cybershop.pcbuild.internal.service.CandidateRepositoryFacade;
import com.project.cybershop.pcbuild.internal.service.LlmClientService;
import com.project.cybershop.pcbuild.internal.support.CandidatePoolBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.data.repository.Repository;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Nguyên tắc 7 (DD 3.1, D24, K19): các thành phần chạy trong vòng lặp tính toán chỉ nhận dữ liệu qua snapshot,
 * không phụ thuộc thứ gì đọc DB, gọi HTTP hay LLM. Một phụ thuộc như vậy biến mỗi request thành hàng nghìn câu SQL.
 */
@DisplayName("Thành phần thuần bộ nhớ (nguyên tắc 7): không phụ thuộc repository, CatalogPublicApi, LLM, facade")
class InMemoryComponentsTest {

    private static final List<Class<?>> FORBIDDEN =
            List.of(CandidateRepositoryFacade.class, CatalogPublicApi.class, LlmClientService.class, Repository.class);

    @ParameterizedTest(name = "{0}")
    @ValueSource(classes = {GreedyBuildSolver.class, RuleBasedCompatibilityEngine.class, PartScorerImpl.class,
            PsuSizerImpl.class, CandidatePoolBuilder.class})
    void hasNoDataAccessDependency(Class<?> component) {
        List<String> dependencies = new ArrayList<>();
        for (Constructor<?> constructor : component.getDeclaredConstructors()) {
            for (Class<?> parameter : constructor.getParameterTypes()) {
                dependencies.add(parameter.getName());
                assertThat(FORBIDDEN).as("%s nhận %s qua constructor", component.getSimpleName(), parameter.getSimpleName())
                        .noneMatch(forbidden -> forbidden.isAssignableFrom(parameter));
            }
        }
        for (Field field : component.getDeclaredFields()) {
            assertThat(FORBIDDEN).as("%s có field %s", component.getSimpleName(), field.getName())
                    .noneMatch(forbidden -> forbidden.isAssignableFrom(field.getType()));
        }
        System.out.printf("[pure] %s phụ thuộc: %s%n", component.getSimpleName(), dependencies);
    }
}
