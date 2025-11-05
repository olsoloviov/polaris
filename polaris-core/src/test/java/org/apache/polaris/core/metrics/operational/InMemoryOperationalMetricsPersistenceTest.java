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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import org.apache.polaris.core.PolarisCallContext;
import org.apache.polaris.core.context.RealmContext;
import org.apache.polaris.core.entity.PolarisEntityId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class InMemoryOperationalMetricsPersistenceTest {

  private InMemoryOperationalMetricsPersistence persistence;
  private PolarisCallContext callContext;
  private PolarisEntityId tableId;

  @BeforeEach
  void setUp() {
    persistence = new InMemoryOperationalMetricsPersistence();
    callContext = mock(PolarisCallContext.class);
    RealmContext realmContext = mock(RealmContext.class);
    when(callContext.getRealmContext()).thenReturn(realmContext);
    when(realmContext.getRealmIdentifier()).thenReturn("test-realm");
    tableId = new PolarisEntityId(1L, 100L);
  }

  @Test
  void testStoreAndRetrieveMetrics() {
    // Create test metrics
    Map<String, MetricValueWithMetadata> metrics = new HashMap<>();
    MetricMetadata metadata = new MetricMetadata(System.currentTimeMillis(), 1L, null);
    metrics.put(
        "total-records", new MetricValueWithMetadata(metadata, new PointMetricValue(1000L)));
    metrics.put(
        "total-data-files", new MetricValueWithMetadata(metadata, new PointMetricValue(10L)));

    // Store metrics
    persistence.storeMetrics(callContext, tableId, metrics);

    // Retrieve all metrics
    Map<String, MetricValueWithMetadata> retrieved =
        persistence.getAllMetrics(callContext, tableId);

    assertThat(retrieved).hasSize(2);
    assertThat(retrieved.get("total-records").getMetric())
        .isInstanceOf(PointMetricValue.class)
        .extracting(m -> ((PointMetricValue) m).getValue())
        .isEqualTo(1000L);
    assertThat(retrieved.get("total-data-files").getMetric())
        .isInstanceOf(PointMetricValue.class)
        .extracting(m -> ((PointMetricValue) m).getValue())
        .isEqualTo(10L);
  }

  @Test
  void testGetSingleMetric() {
    // Store metrics
    Map<String, MetricValueWithMetadata> metrics = new HashMap<>();
    MetricMetadata metadata = new MetricMetadata(System.currentTimeMillis(), 1L, null);
    metrics.put(
        "total-records", new MetricValueWithMetadata(metadata, new PointMetricValue(1000L)));

    persistence.storeMetrics(callContext, tableId, metrics);

    // Retrieve single metric
    MetricValueWithMetadata retrieved =
        persistence.getSingleMetric(callContext, tableId, "total-records");

    assertThat(retrieved).isNotNull();
    assertThat(retrieved.getMetric())
        .isInstanceOf(PointMetricValue.class)
        .extracting(m -> ((PointMetricValue) m).getValue())
        .isEqualTo(1000L);
  }

  @Test
  void testGetSingleMetricNotFound() {
    MetricValueWithMetadata retrieved =
        persistence.getSingleMetric(callContext, tableId, "non-existent");

    assertThat(retrieved).isNull();
  }

  @Test
  void testGetAllMetricsForNonExistentTable() {
    Map<String, MetricValueWithMetadata> retrieved =
        persistence.getAllMetrics(callContext, tableId);

    assertThat(retrieved).isEmpty();
  }

  @Test
  void testUpdateExistingMetric() {
    // Store initial metrics
    Map<String, MetricValueWithMetadata> metrics = new HashMap<>();
    MetricMetadata metadata1 = new MetricMetadata(1000L, 1L, null);
    metrics.put(
        "total-records", new MetricValueWithMetadata(metadata1, new PointMetricValue(1000L)));

    persistence.storeMetrics(callContext, tableId, metrics);

    // Update with new value
    Map<String, MetricValueWithMetadata> updatedMetrics = new HashMap<>();
    MetricMetadata metadata2 = new MetricMetadata(2000L, 2L, null);
    updatedMetrics.put(
        "total-records", new MetricValueWithMetadata(metadata2, new PointMetricValue(2000L)));

    persistence.storeMetrics(callContext, tableId, updatedMetrics);

    // Verify updated value
    MetricValueWithMetadata retrieved =
        persistence.getSingleMetric(callContext, tableId, "total-records");

    assertThat(retrieved.getMetric())
        .isInstanceOf(PointMetricValue.class)
        .extracting(m -> ((PointMetricValue) m).getValue())
        .isEqualTo(2000L);
    assertThat(retrieved.getMetadata().getTimestamp()).isEqualTo(2000L);
    assertThat(retrieved.getMetadata().getIcebergSequenceNumber()).isEqualTo(2L);
  }

  @Test
  void testStoreMultipleTablesMetrics() {
    PolarisEntityId table1 = new PolarisEntityId(1L, 100L);
    PolarisEntityId table2 = new PolarisEntityId(1L, 200L);

    // Store metrics for table 1
    Map<String, MetricValueWithMetadata> metrics1 = new HashMap<>();
    MetricMetadata metadata1 = new MetricMetadata(System.currentTimeMillis(), 1L, null);
    metrics1.put(
        "total-records", new MetricValueWithMetadata(metadata1, new PointMetricValue(1000L)));
    persistence.storeMetrics(callContext, table1, metrics1);

    // Store metrics for table 2
    Map<String, MetricValueWithMetadata> metrics2 = new HashMap<>();
    MetricMetadata metadata2 = new MetricMetadata(System.currentTimeMillis(), 1L, null);
    metrics2.put(
        "total-records", new MetricValueWithMetadata(metadata2, new PointMetricValue(2000L)));
    persistence.storeMetrics(callContext, table2, metrics2);

    // Verify both tables have their own metrics
    MetricValueWithMetadata table1Metric =
        persistence.getSingleMetric(callContext, table1, "total-records");
    MetricValueWithMetadata table2Metric =
        persistence.getSingleMetric(callContext, table2, "total-records");

    assertThat(table1Metric.getMetric())
        .isInstanceOf(PointMetricValue.class)
        .extracting(m -> ((PointMetricValue) m).getValue())
        .isEqualTo(1000L);
    assertThat(table2Metric.getMetric())
        .isInstanceOf(PointMetricValue.class)
        .extracting(m -> ((PointMetricValue) m).getValue())
        .isEqualTo(2000L);
  }

  @Test
  void testStoreNullMetrics() {
    assertThatThrownBy(() -> persistence.storeMetrics(callContext, tableId, null))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void testStoreWithNullTableId() {
    Map<String, MetricValueWithMetadata> metrics = new HashMap<>();
    MetricMetadata metadata = new MetricMetadata(System.currentTimeMillis(), 1L, null);
    metrics.put(
        "total-records", new MetricValueWithMetadata(metadata, new PointMetricValue(1000L)));

    assertThatThrownBy(() -> persistence.storeMetrics(callContext, null, metrics))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void testDistributionMetrics() {
    // Create distribution metric
    Map<String, MetricValueWithMetadata> metrics = new HashMap<>();
    MetricMetadata metadata = new MetricMetadata(System.currentTimeMillis(), 1L, null);
    DistributionValue distribution =
        new DistributionValue(
            100L, // min
            null, // p10
            null, // p25
            500L, // p50
            null, // p75
            900L, // p90
            null, // p99
            null, // p999
            null, // p9999
            1000L, // max
            550L, // average
            null, // standardDeviation
            5500L); // total
    metrics.put(
        "file-size-distribution",
        new MetricValueWithMetadata(metadata, new DistributionMetricValue(distribution)));

    persistence.storeMetrics(callContext, tableId, metrics);

    // Retrieve and verify
    MetricValueWithMetadata retrieved =
        persistence.getSingleMetric(callContext, tableId, "file-size-distribution");

    assertThat(retrieved.getMetric()).isInstanceOf(DistributionMetricValue.class);
    DistributionMetricValue distMetric = (DistributionMetricValue) retrieved.getMetric();
    assertThat(distMetric.getValue().getMin()).isEqualTo(100L);
    assertThat(distMetric.getValue().getMax()).isEqualTo(1000L);
    assertThat(distMetric.getValue().getP50()).isEqualTo(500L);
    assertThat(distMetric.getValue().getP90()).isEqualTo(900L);
  }

  @Test
  void testMetadataPreservation() {
    long timestamp = 1234567890L;
    long sequenceNumber = 42L;

    Map<String, MetricValueWithMetadata> metrics = new HashMap<>();
    MetricMetadata metadata = new MetricMetadata(timestamp, sequenceNumber, null);
    metrics.put(
        "total-records", new MetricValueWithMetadata(metadata, new PointMetricValue(1000L)));

    persistence.storeMetrics(callContext, tableId, metrics);

    MetricValueWithMetadata retrieved =
        persistence.getSingleMetric(callContext, tableId, "total-records");

    assertThat(retrieved.getMetadata().getTimestamp()).isEqualTo(timestamp);
    assertThat(retrieved.getMetadata().getIcebergSequenceNumber()).isEqualTo(sequenceNumber);
  }

  @Test
  void testDeleteAllMetrics() {
    // Store some metrics
    Map<String, MetricValueWithMetadata> metrics = new HashMap<>();
    MetricMetadata metadata = new MetricMetadata(System.currentTimeMillis(), 1L, null);
    metrics.put(
        "added-data-files", new MetricValueWithMetadata(metadata, new PointMetricValue(5L)));
    metrics.put(
        "total-records", new MetricValueWithMetadata(metadata, new PointMetricValue(1000L)));

    persistence.storeMetrics(callContext, tableId, metrics);

    // Verify metrics exist
    assertThat(persistence.getAllMetrics(callContext, tableId)).hasSize(2);

    // Delete all metrics
    Map<String, MetricValueWithMetadata> deletedMetrics =
        persistence.deleteAllMetrics(callContext, tableId);

    // Verify deleted metrics are returned
    assertThat(deletedMetrics).hasSize(2);
    assertThat(deletedMetrics).containsKeys("added-data-files", "total-records");

    // Verify metrics are gone
    assertThat(persistence.getAllMetrics(callContext, tableId)).isEmpty();
    assertThat(persistence.getSingleMetric(callContext, tableId, "added-data-files")).isNull();
  }

  @Test
  void testDeleteAllMetricsWhenEmpty() {
    // Delete metrics for a table that has no metrics
    Map<String, MetricValueWithMetadata> deletedMetrics =
        persistence.deleteAllMetrics(callContext, tableId);

    // Should return empty map
    assertThat(deletedMetrics).isEmpty();
  }

  @Test
  void testMetadataWithAdditionalProperties() {
    long timestamp = 1234567890L;
    long sequenceNumber = 42L;
    long snapshotId = 999L;

    Map<String, MetricValueWithMetadata> metrics = new HashMap<>();
    MetricMetadata metadata = new MetricMetadata(timestamp, sequenceNumber, snapshotId);
    metadata.setAdditionalProperty("custom-key", "custom-value");
    metadata.setAdditionalProperty("another-key", 123);
    metrics.put(
        "total-records", new MetricValueWithMetadata(metadata, new PointMetricValue(1000L)));

    persistence.storeMetrics(callContext, tableId, metrics);

    MetricValueWithMetadata retrieved =
        persistence.getSingleMetric(callContext, tableId, "total-records");

    assertThat(retrieved.getMetadata().getTimestamp()).isEqualTo(timestamp);
    assertThat(retrieved.getMetadata().getIcebergSequenceNumber()).isEqualTo(sequenceNumber);
    assertThat(retrieved.getMetadata().getIcebergSnapshotId()).isEqualTo(snapshotId);
    assertThat(retrieved.getMetadata().getAdditionalProperties())
        .containsEntry("custom-key", "custom-value")
        .containsEntry("another-key", 123);
  }
}
