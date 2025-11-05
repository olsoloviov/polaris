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
package org.apache.polaris.core.metrics.operational;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.polaris.core.PolarisCallContext;
import org.apache.polaris.core.entity.PolarisEntityId;

/**
 * In-memory implementation of {@link OperationalMetricsPersistence}.
 *
 * <p>This implementation stores metrics in memory and is suitable for testing and development
 * purposes. It is not recommended for production use as all metrics will be lost when the service
 * restarts.
 *
 * <p>This implementation is thread-safe.
 */
public class InMemoryOperationalMetricsPersistence implements OperationalMetricsPersistence {

  private final Map<PolarisEntityId, Map<String, MetricValueWithMetadata>> metricsStore =
      new ConcurrentHashMap<>();

  @Override
  public void storeMetrics(
      @Nonnull PolarisCallContext callContext,
      @Nonnull PolarisEntityId tableId,
      @Nonnull Map<String, MetricValueWithMetadata> metrics) {
    metricsStore.compute(
        tableId,
        (k, existingMetrics) -> {
          if (existingMetrics == null) {
            return new HashMap<>(metrics);
          } else {
            existingMetrics.putAll(metrics);
            return existingMetrics;
          }
        });
  }

  @Nonnull
  @Override
  public Map<String, MetricValueWithMetadata> getAllMetrics(
      @Nonnull PolarisCallContext callContext, @Nonnull PolarisEntityId tableId) {
    Map<String, MetricValueWithMetadata> metrics = metricsStore.get(tableId);
    return metrics == null ? Collections.emptyMap() : new HashMap<>(metrics);
  }

  @Nullable
  @Override
  public MetricValueWithMetadata getSingleMetric(
      @Nonnull PolarisCallContext callContext,
      @Nonnull PolarisEntityId tableId,
      @Nonnull String metricName) {
    Map<String, MetricValueWithMetadata> metrics = metricsStore.get(tableId);
    return metrics == null ? null : metrics.get(metricName);
  }

  @Nonnull
  @Override
  public Map<String, MetricValueWithMetadata> deleteAllMetrics(
      @Nonnull PolarisCallContext callContext, @Nonnull PolarisEntityId tableId) {
    Map<String, MetricValueWithMetadata> deletedMetrics = metricsStore.remove(tableId);
    return deletedMetrics == null ? Collections.emptyMap() : deletedMetrics;
  }

  /** Clears all stored metrics. Useful for testing. */
  public void clear() {
    metricsStore.clear();
  }
}
