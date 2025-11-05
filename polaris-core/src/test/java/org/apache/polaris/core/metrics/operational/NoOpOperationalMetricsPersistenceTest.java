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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.util.HashMap;
import java.util.Map;
import org.apache.polaris.core.PolarisCallContext;
import org.apache.polaris.core.entity.PolarisEntityId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class NoOpOperationalMetricsPersistenceTest {

  private NoOpOperationalMetricsPersistence persistence;
  private PolarisCallContext callContext;
  private PolarisEntityId tableId;

  @BeforeEach
  void setUp() {
    persistence = new NoOpOperationalMetricsPersistence();
    callContext = mock(PolarisCallContext.class);
    tableId = new PolarisEntityId(1L, 100L);
  }

  @Test
  void testStoreMetricsDoesNothing() {
    Map<String, MetricValueWithMetadata> metrics = new HashMap<>();
    MetricMetadata metadata = new MetricMetadata(System.currentTimeMillis(), 1L, null);
    metrics.put(
        "total-records", new MetricValueWithMetadata(metadata, new PointMetricValue(1000L)));

    // Should not throw any exception
    persistence.storeMetrics(callContext, tableId, metrics);
  }

  @Test
  void testGetAllMetricsReturnsEmpty() {
    Map<String, MetricValueWithMetadata> result = persistence.getAllMetrics(callContext, tableId);

    assertThat(result).isNotNull().isEmpty();
  }

  @Test
  void testGetSingleMetricReturnsNull() {
    MetricValueWithMetadata result =
        persistence.getSingleMetric(callContext, tableId, "total-records");

    assertThat(result).isNull();
  }

  @Test
  void testStoreAndRetrieveReturnsEmpty() {
    // Store some metrics
    Map<String, MetricValueWithMetadata> metrics = new HashMap<>();
    MetricMetadata metadata = new MetricMetadata(System.currentTimeMillis(), 1L, null);
    metrics.put(
        "total-records", new MetricValueWithMetadata(metadata, new PointMetricValue(1000L)));
    persistence.storeMetrics(callContext, tableId, metrics);

    // Verify nothing is stored
    Map<String, MetricValueWithMetadata> result = persistence.getAllMetrics(callContext, tableId);
    assertThat(result).isEmpty();

    MetricValueWithMetadata singleResult =
        persistence.getSingleMetric(callContext, tableId, "total-records");
    assertThat(singleResult).isNull();
  }

  @Test
  void testDeleteAllMetricsReturnsEmpty() {
    // Delete metrics (no-op should return empty)
    Map<String, MetricValueWithMetadata> deletedMetrics =
        persistence.deleteAllMetrics(callContext, tableId);

    // Should return empty map
    assertThat(deletedMetrics).isEmpty();
  }
}
