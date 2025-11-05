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
import java.util.Map;
import org.apache.polaris.core.PolarisCallContext;
import org.apache.polaris.core.entity.PolarisEntityId;

/**
 * No-op implementation of {@link OperationalMetricsPersistence}.
 *
 * <p>This implementation is used by default when no operational metrics database is explicitly
 * configured by an administrator. All operations are no-ops, effectively disabling metrics support.
 */
public class NoOpOperationalMetricsPersistence implements OperationalMetricsPersistence {

  @Override
  public void storeMetrics(
      @Nonnull PolarisCallContext callContext,
      @Nonnull PolarisEntityId tableId,
      @Nonnull Map<String, MetricValueWithMetadata> metrics) {
    // No-op: metrics are not stored
  }

  @Nonnull
  @Override
  public Map<String, MetricValueWithMetadata> getAllMetrics(
      @Nonnull PolarisCallContext callContext, @Nonnull PolarisEntityId tableId) {
    // No-op: return empty map
    return Collections.emptyMap();
  }

  @Nullable
  @Override
  public MetricValueWithMetadata getSingleMetric(
      @Nonnull PolarisCallContext callContext,
      @Nonnull PolarisEntityId tableId,
      @Nonnull String metricName) {
    // No-op: return null
    return null;
  }

  @Nonnull
  @Override
  public Map<String, MetricValueWithMetadata> deleteAllMetrics(
      @Nonnull PolarisCallContext callContext, @Nonnull PolarisEntityId tableId) {
    // No-op: return empty map
    return Collections.emptyMap();
  }
}
