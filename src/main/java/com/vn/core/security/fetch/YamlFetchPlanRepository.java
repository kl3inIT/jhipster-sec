package com.vn.core.security.fetch;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.vn.core.config.ApplicationProperties;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

/**
 * YAML-backed implementation of {@link FetchPlanRepository}.
 * Loads fetch plan definitions from the file specified in
 * {@code application.fetch-plans.config} (default: {@code classpath:fetch-plans.yml}).
 *
 * <p>Plans are keyed as {@code entityClassName.toLowerCase()#planName} for lookup.
 * Supports {@code extends} to inherit properties from another named plan.
 */
@Component
public class YamlFetchPlanRepository implements FetchPlanRepository {

    private static final Logger LOG = LoggerFactory.getLogger(YamlFetchPlanRepository.class);

    private final ApplicationProperties applicationProperties;
    private final ResourceLoader resourceLoader;

    private final Map<String, FetchPlan> plans = new HashMap<>();

    public YamlFetchPlanRepository(ApplicationProperties applicationProperties, ResourceLoader resourceLoader) {
        this.applicationProperties = applicationProperties;
        this.resourceLoader = resourceLoader;
    }

    @PostConstruct
    void init() {
        String configPath = applicationProperties.getFetchPlans().getConfig();
        LOG.debug("Loading fetch plans from: {}", configPath);
        try (InputStream is = resourceLoader.getResource(configPath).getInputStream()) {
            ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            JsonNode root = mapper.readTree(is);
            JsonNode fetchPlansNode = root.get("fetch-plans");
            if (fetchPlansNode == null || !fetchPlansNode.isArray() || fetchPlansNode.isEmpty()) {
                LOG.debug("No fetch plans defined in {}", configPath);
                return;
            }

            // Two-pass: first pass creates all plans without extends resolution
            // Second pass is skipped as Phase 4 will add real plans - for now handle basic cases
            Map<String, JsonNode> rawPlans = new HashMap<>();
            List<String> planOrder = new ArrayList<>();

            for (JsonNode planNode : fetchPlansNode) {
                String entityName = planNode.get("entity").asText();
                String planName = planNode.get("name").asText();
                String key = buildKey(entityName, planName);
                rawPlans.put(key, planNode);
                planOrder.add(key);
            }

            // Build plans in order, resolving extends references
            for (String key : planOrder) {
                if (!plans.containsKey(key)) {
                    buildPlan(key, rawPlans, new ArrayList<>());
                }
            }

            LOG.debug("Loaded {} fetch plan(s)", plans.size());
        } catch (IOException e) {
            LOG.warn("Could not load fetch plans from {}: {}", configPath, e.getMessage());
        }
    }

    private FetchPlan buildPlan(String key, Map<String, JsonNode> rawPlans, List<String> buildStack) {
        if (plans.containsKey(key)) {
            return plans.get(key);
        }

        JsonNode planNode = rawPlans.get(key);
        if (planNode == null) {
            throw new IllegalArgumentException("Fetch plan not found for key: " + key);
        }

        // Cycle detection
        if (buildStack.contains(key)) {
            throw new IllegalStateException("Circular fetch plan extends detected: " + buildStack + " -> " + key);
        }
        buildStack.add(key);

        String entityName = planNode.get("entity").asText();
        String planName = planNode.get("name").asText();

        List<FetchPlanProperty> properties = new ArrayList<>();

        // Resolve extends first — inherit parent properties
        if (planNode.has("extends")) {
            String parentPlanName = planNode.get("extends").asText();
            String parentKey = buildKey(entityName, parentPlanName);
            FetchPlan parentPlan = buildPlan(parentKey, rawPlans, new ArrayList<>(buildStack));
            properties.addAll(parentPlan.getProperties());
        }

        // Add own properties
        JsonNode propsNode = planNode.get("properties");
        if (propsNode != null && propsNode.isArray()) {
            for (JsonNode propNode : propsNode) {
                if (propNode.isTextual()) {
                    properties.add(new FetchPlanProperty(propNode.asText()));
                } else if (propNode.isObject()) {
                    String propName = propNode.get("name").asText();
                    if (propNode.has("properties")) {
                        FetchPlan nestedPlan = new FetchPlan(planName + ":" + propName, null, parseProperties(propNode.get("properties"), entityName, rawPlans, buildStack));
                        properties.add(new FetchPlanProperty(propName, nestedPlan, FetchMode.AUTO));
                    } else if (propNode.has("fetchPlan")) {
                        String nestedPlanName = propNode.get("fetchPlan").asText();
                        String nestedKey = buildKey(entityName, nestedPlanName);
                        FetchPlan nestedPlan;
                        if (rawPlans.containsKey(nestedKey)) {
                            nestedPlan = buildPlan(nestedKey, rawPlans, new ArrayList<>(buildStack));
                        } else {
                            // Placeholder for external plan reference
                            nestedPlan = new FetchPlan(nestedPlanName, null, List.of());
                        }
                        properties.add(new FetchPlanProperty(propName, nestedPlan, FetchMode.AUTO));
                    } else {
                        properties.add(new FetchPlanProperty(propName));
                    }
                }
            }
        }

        // Resolve entity class from name (best-effort — Phase 4 provides real entities)
        Class<?> entityClass = resolveEntityClass(entityName);
        FetchPlan plan = new FetchPlan(planName, entityClass, List.copyOf(properties));
        plans.put(key, plan);
        buildStack.remove(key);
        return plan;
    }

    private List<FetchPlanProperty> parseProperties(
        JsonNode propsNode,
        String entityName,
        Map<String, JsonNode> rawPlans,
        List<String> buildStack
    ) {
        List<FetchPlanProperty> properties = new ArrayList<>();
        if (propsNode == null || !propsNode.isArray()) {
            return properties;
        }

        for (JsonNode propNode : propsNode) {
            if (propNode.isTextual()) {
                properties.add(new FetchPlanProperty(propNode.asText()));
            } else if (propNode.isObject()) {
                String propName = propNode.get("name").asText();
                if (propNode.has("properties")) {
                    FetchPlan nestedPlan = new FetchPlan(
                        buildStack.get(buildStack.size() - 1).substring(buildStack.get(buildStack.size() - 1).lastIndexOf('#') + 1) + ":" + propName,
                        null,
                        parseProperties(propNode.get("properties"), entityName, rawPlans, buildStack)
                    );
                    properties.add(new FetchPlanProperty(propName, nestedPlan, FetchMode.AUTO));
                } else if (propNode.has("fetchPlan")) {
                    String nestedPlanName = propNode.get("fetchPlan").asText();
                    String nestedKey = buildKey(entityName, nestedPlanName);
                    FetchPlan nestedPlan;
                    if (rawPlans.containsKey(nestedKey)) {
                        nestedPlan = buildPlan(nestedKey, rawPlans, new ArrayList<>(buildStack));
                    } else {
                        nestedPlan = new FetchPlan(nestedPlanName, null, List.of());
                    }
                    properties.add(new FetchPlanProperty(propName, nestedPlan, FetchMode.AUTO));
                } else {
                    properties.add(new FetchPlanProperty(propName));
                }
            }
        }

        return List.copyOf(properties);
    }

    private Class<?> resolveEntityClass(String entityName) {
        try {
            return Class.forName(entityName);
        } catch (ClassNotFoundException e) {
            LOG.debug("Entity class not found on classpath: {} — storing plan without entity class", entityName);
            return null;
        }
    }

    private String buildKey(String entityName, String planName) {
        return entityName.toLowerCase(Locale.ROOT) + "#" + planName;
    }

    @Override
    public Optional<FetchPlan> findByEntityAndName(Class<?> entityClass, String name) {
        String key = entityClass.getName().toLowerCase(Locale.ROOT) + "#" + name;
        return Optional.ofNullable(plans.get(key));
    }

    @Override
    public List<FetchPlan> findAllByEntity(Class<?> entityClass) {
        String prefix = entityClass.getName().toLowerCase(Locale.ROOT) + "#";
        return plans.entrySet().stream().filter(e -> e.getKey().startsWith(prefix)).map(Map.Entry::getValue).toList();
    }
}
