/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.polaris.service.metrics.operational;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.ApplicationScoped;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.polaris.core.metrics.operational.DistributionMetricValue;
import org.apache.polaris.core.metrics.operational.MetricDefinition;
import org.apache.polaris.core.metrics.operational.MetricType;
import org.apache.polaris.core.metrics.operational.MetricValue;
import org.apache.polaris.core.metrics.operational.OperationalMetricsException;
import org.apache.polaris.core.metrics.operational.PointMetricValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Registry of supported operational metrics.
 *
 * <p>This registry contains the list of operational metrics that can be computed, submitted to, and
 * fetched from Polaris by external services and query engines.
 *
 * <p>Metrics are loaded from a JSON configuration file at startup. The registry is immutable and
 * thread-safe. It provides methods to:
 *
 * <ul>
 *   <li>Get all supported metric definitions
 *   <li>Check if a metric name is supported
 *   <li>Get the definition for a specific metric
 *   <li>Validate that a metric value matches its expected type
 * </ul>
 */
@ApplicationScoped
public class OperationalMetricsRegistry {
  private static final Logger LOGGER = LoggerFactory.getLogger(OperationalMetricsRegistry.class);
  private static final String DEFINITIONS_FILE = "operational-metrics-definitions.json";
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private final Map<String, MetricDefinition> definitions;
  private final List<MetricDefinition> allDefinitions;

  /** CDI constructor - called once at application startup. */
  public OperationalMetricsRegistry() {
    this.definitions = loadDefinitions();
    this.allDefinitions = List.copyOf(definitions.values());
  }

  /**
   * Load metric definitions from JSON configuration file.
   *
   * @return unmodifiable map of metric name to definition
   */
  private static Map<String, MetricDefinition> loadDefinitions() {
    try (InputStream inputStream =
        OperationalMetricsRegistry.class.getClassLoader().getResourceAsStream(DEFINITIONS_FILE)) {
      if (inputStream == null) {
        LOGGER.error("Metric definitions file not found: {}", DEFINITIONS_FILE);
        throw new IllegalStateException("Metric definitions file not found: " + DEFINITIONS_FILE);
      }

      MetricDefinitionsConfig config =
          OBJECT_MAPPER.readValue(inputStream, MetricDefinitionsConfig.class);

      Map<String, MetricDefinition> defs = new LinkedHashMap<>();
      for (MetricDefinition def : config.getMetricDefinitions()) {
        MetricDefinition existing = defs.put(def.getName(), def);
        if (existing != null) {
          LOGGER.error(
              "Duplicate metric definition found: '{}' in {}", def.getName(), DEFINITIONS_FILE);
          throw new IllegalStateException(
              "Duplicate metric definition: '" + def.getName() + "'");
        }
      }

      LOGGER.info("Loaded {} metric definitions from {}", defs.size(), DEFINITIONS_FILE);
      return Collections.unmodifiableMap(defs);
    } catch (IOException e) {
      LOGGER.error("Failed to load metric definitions from {}", DEFINITIONS_FILE, e);
      throw new IllegalStateException("Failed to load metric definitions", e);
    }
  }

  /**
   * Get all supported metric definitions.
   *
   * @return unmodifiable list of all metric definitions
   */
  @Nonnull
  public List<MetricDefinition> getAllDefinitions() {
    return allDefinitions;
  }

  /**
   * Get the definition for a specific metric.
   *
   * @param metricName the metric name
   * @return the metric definition, or null if not found
   */
  @Nullable
  public MetricDefinition getDefinition(@Nonnull String metricName) {
    return definitions.get(metricName);
  }

  /**
   * Validate that a metric is supported and its value matches the expected type.
   *
   * @param metricName the metric name
   * @param metricValue the metric value to validate
   * @throws NullPointerException if metricName or metricValue is null
   * @throws OperationalMetricsException if the metric is not supported or the type doesn't match
   */
  public void validateMetric(@Nonnull String metricName, @Nonnull MetricValue metricValue) {
    if (metricName == null) {
      throw new NullPointerException("metricName cannot be null");
    }
    if (metricValue == null) {
      throw new NullPointerException("metricValue cannot be null");
    }

    MetricDefinition definition = definitions.get(metricName);
    if (definition == null) {
      throw new OperationalMetricsException("Metric '" + metricName + "' is not registered");
    }

    MetricType expectedType = definition.getType();
    MetricType actualType = getMetricValueType(metricValue);

    if (expectedType != actualType) {
      throw new OperationalMetricsException(
          String.format(
              "Invalid metric type for '%s': expected '%s', got '%s'",
              metricName, expectedType.getValue(), actualType.getValue()));
    }
  }

  /**
   * Get the type of a metric value.
   *
   * @param metricValue the metric value
   * @return the metric type
   */
  private MetricType getMetricValueType(MetricValue metricValue) {
    if (metricValue instanceof PointMetricValue) {
      return MetricType.POINT;
    } else if (metricValue instanceof DistributionMetricValue) {
      return MetricType.DISTRIBUTION;
    } else {
      throw new IllegalArgumentException("Unknown metric value type: " + metricValue.getClass());
    }
  }

  /**
   * Internal class for deserializing the JSON configuration file.
   *
   * <p>This matches the structure of the operational-metrics-definitions.json file.
   */
  private static class MetricDefinitionsConfig {
    private final List<MetricDefinition> metricDefinitions;

    @JsonCreator
    public MetricDefinitionsConfig(
        @JsonProperty("metric-definitions") List<MetricDefinition> metricDefinitions) {
      this.metricDefinitions = metricDefinitions;
    }

    public List<MetricDefinition> getMetricDefinitions() {
      return metricDefinitions;
    }
  }
}
