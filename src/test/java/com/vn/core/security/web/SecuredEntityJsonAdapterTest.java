package com.vn.core.security.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vn.core.security.fetch.FetchPlan;
import com.vn.core.security.fetch.FetchPlanBuilder;
import com.vn.core.security.fetch.FetchPlanResolver;
import com.vn.core.security.serialize.SecureEntitySerializer;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link SecuredEntityJsonAdapter} proving that:
 * <ul>
 *   <li>D-08: The fetch plan is resolved once per detail call (not per entity).</li>
 *   <li>D-08: The fetch plan is resolved once per list serialization call (not once per entity in the list).</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class SecuredEntityJsonAdapterTest {

    @Mock
    private FetchPlanResolver fetchPlanResolver;

    @Mock
    private SecureEntitySerializer secureEntitySerializer;

    @Mock
    private SecuredEntityPayloadValidator securedEntityPayloadValidator;

    private SecuredEntityJsonAdapter adapter;

    static class SampleEntity {

        private final Long id;
        private final String name;

        SampleEntity(Long id, String name) {
            this.id = id;
            this.name = name;
        }

        public Long getId() {
            return id;
        }

        public String getName() {
            return name;
        }
    }

    @BeforeEach
    void setUp() {
        adapter = new SecuredEntityJsonAdapter(new ObjectMapper(), fetchPlanResolver, secureEntitySerializer, securedEntityPayloadValidator);
    }

    @Test
    void toJson_resolvesTheFetchPlanExactlyOnce() {
        FetchPlan plan = new FetchPlanBuilder(SampleEntity.class, "sample-detail").add("id").add("name").build();
        when(fetchPlanResolver.resolve(eq(SampleEntity.class), eq("sample-detail"))).thenReturn(plan);
        when(secureEntitySerializer.serialize(any(), eq(plan))).thenReturn(Map.of("id", 1L, "name", "Test"));

        SampleEntity entity = new SampleEntity(1L, "Test");

        adapter.toJson(entity, "sample-detail");

        // D-08: exactly one fetch-plan resolution per detail call
        verify(fetchPlanResolver, times(1)).resolve(SampleEntity.class, "sample-detail");
    }

    @Test
    void toJsonArrayString_resolvesTheFetchPlanExactlyOnceForThreeEntities() {
        FetchPlan plan = new FetchPlanBuilder(SampleEntity.class, "sample-list").add("id").add("name").build();
        when(fetchPlanResolver.resolve(eq(SampleEntity.class), eq("sample-list"))).thenReturn(plan);
        when(secureEntitySerializer.serialize(any(), eq(plan))).thenReturn(Map.of("id", 1L, "name", "Test"));

        List<SampleEntity> entities = List.of(
            new SampleEntity(1L, "Alpha"),
            new SampleEntity(2L, "Beta"),
            new SampleEntity(3L, "Gamma")
        );

        adapter.toJsonArrayString(entities, "sample-list");

        // D-08: exactly one fetch-plan resolution per list call, regardless of entity count
        verify(fetchPlanResolver, times(1)).resolve(SampleEntity.class, "sample-list");
    }

    @Test
    void toJsonArrayString_withEmptyList_doesNotResolveFetchPlan() {
        adapter.toJsonArrayString(List.of(), "sample-list");

        // No entities means no resolution needed
        verify(fetchPlanResolver, times(0)).resolve(any(), any());
    }

    @Test
    void toJson_withNullEntity_returnsNullNodeWithoutResolving() {
        adapter.toJson(null, "sample-detail");

        // Null entity should not trigger fetch-plan resolution
        verify(fetchPlanResolver, times(0)).resolve(any(), any());
    }

    @Test
    void toJsonArrayString_withNullList_returnsEmptyArrayWithoutResolving() {
        String result = adapter.toJsonArrayString(null, "sample-list");

        assertThat(result).isEqualTo("[]");
        verify(fetchPlanResolver, times(0)).resolve(any(), any());
    }

    @Test
    void toJsonString_delegatesToToJson_resolvingOnce() {
        FetchPlan plan = new FetchPlanBuilder(SampleEntity.class, "sample-detail").add("id").add("name").build();
        when(fetchPlanResolver.resolve(eq(SampleEntity.class), eq("sample-detail"))).thenReturn(plan);
        when(secureEntitySerializer.serialize(any(), eq(plan))).thenReturn(Map.of("id", 1L, "name", "Test"));

        SampleEntity entity = new SampleEntity(1L, "Test");

        adapter.toJsonString(entity, "sample-detail");

        // D-08: exactly one fetch-plan resolution per detail string call
        verify(fetchPlanResolver, times(1)).resolve(SampleEntity.class, "sample-detail");
    }
}
