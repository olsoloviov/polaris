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
import java.util.Map;
import org.apache.polaris.core.PolarisCallContext;
import org.apache.polaris.core.entity.PolarisEntityId;

/**
 * Service Provider Interface for operational metrics storage and retrieval.
 *
 * <p>This interface allows service administrators to plug in their preferred database for metrics
 * storage and retrieval. Implementations can use time-series databases, custom retention policies,
 * or any other storage mechanism suitable for their needs.
 *
 * <p>Metrics are identified by a {@link PolarisEntityId} which contains {@code (catalogId, id)}.
 * This follows the standard Polaris entity identification pattern where the combination uniquely
 * identifies a table entity within a realm. The realm context is provided via {@link
 * PolarisCallContext}.
 *
 * <p>The interface is designed to be simple and flexible, allowing for various storage backends
 * while maintaining a consistent API for the Polaris operational metrics system.
 *
 * <p>Note: The metrics database is assumed to be secured and trusted. It is the administrators'
 * responsibility to ensure that metrics are only accessible to the Polaris service and to implement
 * proper retention policies and data cleanup logic.
 */
public interface OperationalMetricsPersistence {

  /**
   * Store new values for given metrics.
   *
   * <p>This method stores metric values along with their metadata for a specific table. The
   * implementation should handle both point metrics (single values) and distribution metrics
   * (statistical data with percentiles).
   *
   * <p>If a metric already exists for the table, the implementation should update it with the new
   * value. The behavior for historical values (whether to keep them or replace them) is
   * implementation-specific.
   *
   * @param callContext the Polaris call context containing realm and authentication information
   * @param tableId the entity ID of the table, containing catalogId and entity id
   * @param metrics a map of metric names to their values and metadata. Each entry contains a {@link
   *     MetricValueWithMetadata} object with the metric value and associated metadata
   * @throws OperationalMetricsException if there is an error storing the metrics
   */
  void storeMetrics(
      @Nonnull PolarisCallContext callContext,
      @Nonnull PolarisEntityId tableId,
      @Nonnull Map<String, MetricValueWithMetadata> metrics);

  /**
   * Fetch the latest values of all metrics for a given table.
   *
   * <p>This method retrieves all available metrics for the specified table. If no metrics exist for
   * the table, an empty map should be returned.
   *
   * @param callContext the Polaris call context containing realm and authentication information
   * @param tableId the entity ID of the table, containing catalogId and entity id
   * @return a map of metric names to their values and metadata. Returns an empty map if no metrics
   *     exist for the table
   * @throws OperationalMetricsException if there is an error retrieving the metrics
   */
  @Nonnull
  Map<String, MetricValueWithMetadata> getAllMetrics(
      @Nonnull PolarisCallContext callContext, @Nonnull PolarisEntityId tableId);

  /**
   * Fetch the latest value of a single metric for a given table.
   *
   * <p>This method retrieves a specific metric for the specified table. If the metric does not
   * exist, null should be returned.
   *
   * @param callContext the Polaris call context containing realm and authentication information
   * @param tableId the entity ID of the table, containing catalogId and entity id
   * @param metricName the name of the metric to retrieve
   * @return the metric value and metadata, or null if the metric does not exist for the table
   * @throws OperationalMetricsException if there is an error retrieving the metric
   */
  @Nullable
  MetricValueWithMetadata getSingleMetric(
      @Nonnull PolarisCallContext callContext,
      @Nonnull PolarisEntityId tableId,
      @Nonnull String metricName);

  /**
   * Delete all metrics for a given table.
   *
   * <p>This method removes all stored metrics for the specified table and returns the deleted
   * metrics. This is typically called when a table is dropped to clean up associated metric data.
   *
   * <p>If no metrics exist for the table, an empty map should be returned.
   *
   * @param callContext the Polaris call context containing realm and authentication information
   * @param tableId the entity ID of the table, containing catalogId and entity id
   * @return a map of metric names to their values and metadata that were deleted. Returns an empty
   *     map if no metrics existed for the table
   * @throws OperationalMetricsException if there is an error deleting the metrics
   */
  @Nonnull
  Map<String, MetricValueWithMetadata> deleteAllMetrics(
      @Nonnull PolarisCallContext callContext, @Nonnull PolarisEntityId tableId);
}
