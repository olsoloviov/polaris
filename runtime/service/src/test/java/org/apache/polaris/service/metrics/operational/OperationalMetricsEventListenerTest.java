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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.common.annotation.Identifier;
import jakarta.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import org.apache.iceberg.PartitionSpec;
import org.apache.iceberg.Schema;
import org.apache.iceberg.Snapshot;
import org.apache.iceberg.TableMetadata;
import org.apache.iceberg.catalog.Namespace;
import org.apache.iceberg.catalog.TableIdentifier;
import org.apache.iceberg.types.Types;
import org.apache.polaris.core.PolarisCallContext;
import org.apache.polaris.core.context.CallContext;
import org.apache.polaris.core.context.RealmContext;
import org.apache.polaris.core.entity.PolarisEntity;
import org.apache.polaris.core.entity.PolarisEntityId;
import org.apache.polaris.core.entity.PolarisEntitySubType;
import org.apache.polaris.core.entity.PolarisEntityType;
import org.apache.polaris.core.metrics.operational.MetricMetadata;
import org.apache.polaris.core.metrics.operational.MetricValueWithMetadata;
import org.apache.polaris.core.metrics.operational.OperationalMetricsPersistence;
import org.apache.polaris.core.metrics.operational.OperationalMetricsPersistenceFactory;
import org.apache.polaris.core.metrics.operational.PointMetricValue;
import org.apache.polaris.core.persistence.PolarisResolvedPathWrapper;
import org.apache.polaris.core.persistence.resolver.PolarisResolutionManifest;
import org.apache.polaris.core.persistence.resolver.ResolutionManifestFactory;
import org.apache.polaris.service.events.IcebergRestCatalogEvents;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@QuarkusTest
class OperationalMetricsEventListenerTest {

  @Inject
  @Identifier("operational-metrics")
  OperationalMetricsEventListener listener;

  @Inject OperationalMetricsRegistry registry;

  @InjectMock CallContext callContext;

  @InjectMock ResolutionManifestFactory resolutionManifestFactory;

  @InjectMock OperationalMetricsPersistenceFactory persistenceFactory;

  private OperationalMetricsPersistence mockPersistence;
  private PolarisCallContext polarisCallContext;
  private RealmContext realmContext;

  @BeforeEach
  void setUp() {
    mockPersistence = mock(OperationalMetricsPersistence.class);
    polarisCallContext = mock(PolarisCallContext.class);
    realmContext = mock(RealmContext.class);

    when(callContext.getPolarisCallContext()).thenReturn(polarisCallContext);
    when(callContext.getRealmContext()).thenReturn(realmContext);
    when(persistenceFactory.getOrCreatePersistence(realmContext)).thenReturn(mockPersistence);
  }

  @Test
  void testOnAfterCommitTableExtractsAndStoresMetrics() {
    // Setup entity resolution mocks
    setupEntityResolutionMocks("test_catalog", Namespace.of("ns1"), "test_table");

    // Create table metadata with snapshot summary containing metrics
    TableMetadata metadata = createTableMetadataWithMetrics();

    // Create event
    IcebergRestCatalogEvents.AfterCommitTableEvent event =
        new IcebergRestCatalogEvents.AfterCommitTableEvent(
            "test_catalog",
            TableIdentifier.of("ns1", "test_table"),
            null, // metadataBefore
            metadata);

    // Trigger event
    listener.onAfterCommitTable(event);

    // Verify metrics were stored
    ArgumentCaptor<Map<String, MetricValueWithMetadata>> metricsCaptor =
        ArgumentCaptor.forClass(Map.class);
    verify(mockPersistence)
        .storeMetrics(eq(polarisCallContext), any(PolarisEntityId.class), metricsCaptor.capture());

    Map<String, MetricValueWithMetadata> storedMetrics = metricsCaptor.getValue();
    assertThat(storedMetrics).isNotEmpty();
    assertThat(storedMetrics).containsKeys("added-data-files", "added-records", "total-records");
  }

  @Test
  void testOnAfterCommitTableWithNoSnapshot() {
    // Setup entity resolution mocks
    setupEntityResolutionMocks("test_catalog", Namespace.of("ns1"), "test_table");

    // Create table metadata without snapshot
    Schema schema = new Schema(Types.NestedField.required(1, "id", Types.LongType.get()));
    TableMetadata metadata =
        TableMetadata.newTableMetadata(
            schema, PartitionSpec.unpartitioned(), "s3://bucket/table", Map.of());

    IcebergRestCatalogEvents.AfterCommitTableEvent event =
        new IcebergRestCatalogEvents.AfterCommitTableEvent(
            "test_catalog", TableIdentifier.of("ns1", "test_table"), null, metadata);

    // Trigger event
    listener.onAfterCommitTable(event);

    // Verify no metrics were stored
    verify(mockPersistence, never()).storeMetrics(any(), any(PolarisEntityId.class), any());
  }

  @Test
  void testOnAfterCommitTableWithEmptySnapshotSummary() {
    // Setup entity resolution mocks
    setupEntityResolutionMocks("test_catalog", Namespace.of("ns1"), "test_table");

    // Create table metadata with empty snapshot summary
    Schema schema = new Schema(Types.NestedField.required(1, "id", Types.LongType.get()));
    TableMetadata metadata =
        TableMetadata.newTableMetadata(
            schema, PartitionSpec.unpartitioned(), "s3://bucket/table", Map.of());

    // Create a snapshot with empty summary
    Snapshot snapshot = mock(Snapshot.class);
    when(snapshot.summary()).thenReturn(Map.of());
    when(snapshot.sequenceNumber()).thenReturn(1L);

    TableMetadata metadataWithSnapshot = mock(TableMetadata.class);
    when(metadataWithSnapshot.currentSnapshot()).thenReturn(snapshot);

    IcebergRestCatalogEvents.AfterCommitTableEvent event =
        new IcebergRestCatalogEvents.AfterCommitTableEvent(
            "test_catalog", TableIdentifier.of("ns1", "test_table"), null, metadataWithSnapshot);

    // Trigger event
    listener.onAfterCommitTable(event);

    // Verify no metrics were stored (empty summary)
    verify(mockPersistence, never()).storeMetrics(any(), any(PolarisEntityId.class), any());
  }

  @Test
  void testOnAfterCommitTableFiltersUnregisteredMetrics() {
    // Setup entity resolution mocks
    setupEntityResolutionMocks("test_catalog", Namespace.of("ns1"), "test_table");

    // Create snapshot with both registered and unregistered metrics
    Snapshot snapshot = mock(Snapshot.class);
    when(snapshot.summary())
        .thenReturn(
            Map.of(
                "added-data-files", "5", // registered
                "total-records", "1000", // registered
                "custom-metric", "999", // not registered
                "another-unknown", "123")); // not registered
    when(snapshot.sequenceNumber()).thenReturn(1L);

    TableMetadata metadata = mock(TableMetadata.class);
    when(metadata.currentSnapshot()).thenReturn(snapshot);

    IcebergRestCatalogEvents.AfterCommitTableEvent event =
        new IcebergRestCatalogEvents.AfterCommitTableEvent(
            "test_catalog", TableIdentifier.of("ns1", "test_table"), null, metadata);

    // Trigger event
    listener.onAfterCommitTable(event);

    // Verify only registered metrics were stored
    ArgumentCaptor<Map<String, MetricValueWithMetadata>> metricsCaptor =
        ArgumentCaptor.forClass(Map.class);
    verify(mockPersistence)
        .storeMetrics(eq(polarisCallContext), any(PolarisEntityId.class), metricsCaptor.capture());

    Map<String, MetricValueWithMetadata> storedMetrics = metricsCaptor.getValue();
    assertThat(storedMetrics).hasSize(2);
    assertThat(storedMetrics).containsOnlyKeys("added-data-files", "total-records");
    assertThat(storedMetrics).doesNotContainKeys("custom-metric", "another-unknown");
  }

  @Test
  void testOnAfterCommitTableWithNestedNamespace() {
    // Setup entity resolution mocks for nested namespace
    setupEntityResolutionMocks("test_catalog", Namespace.of("level1", "level2"), "test_table");

    TableMetadata metadata = createTableMetadataWithMetrics();

    IcebergRestCatalogEvents.AfterCommitTableEvent event =
        new IcebergRestCatalogEvents.AfterCommitTableEvent(
            "test_catalog",
            TableIdentifier.of(Namespace.of("level1", "level2"), "test_table"),
            null,
            metadata);

    // Trigger event
    listener.onAfterCommitTable(event);

    // Verify metrics were stored
    verify(mockPersistence).storeMetrics(eq(polarisCallContext), any(PolarisEntityId.class), any());
  }

  private void setupEntityResolutionMocks(
      String catalogName, Namespace namespace, String tableName) {
    // Create mock table entity
    PolarisEntity tableEntity = mock(PolarisEntity.class);
    when(tableEntity.getCatalogId()).thenReturn(1L);
    when(tableEntity.getId()).thenReturn(100L);

    // Create mock resolved path wrapper
    PolarisResolvedPathWrapper resolvedPathWrapper = mock(PolarisResolvedPathWrapper.class);
    when(resolvedPathWrapper.getRawLeafEntity()).thenReturn(tableEntity);

    // Create mock resolution manifest
    PolarisResolutionManifest mockManifest = mock(PolarisResolutionManifest.class);
    when(mockManifest.getResolvedPath(
            any(TableIdentifier.class),
            eq(PolarisEntityType.TABLE_LIKE),
            eq(PolarisEntitySubType.ICEBERG_TABLE)))
        .thenReturn(resolvedPathWrapper);

    // Mock the factory to return our mock manifest
    when(resolutionManifestFactory.createResolutionManifest(eq(null), eq(catalogName)))
        .thenReturn(mockManifest);
  }

  private TableMetadata createTableMetadataWithMetrics() {
    Snapshot snapshot = mock(Snapshot.class);
    when(snapshot.summary())
        .thenReturn(
            Map.of(
                "added-data-files", "5",
                "added-records", "1000",
                "total-records", "5000",
                "total-data-files", "10"));
    when(snapshot.sequenceNumber()).thenReturn(1L);

    TableMetadata metadata = mock(TableMetadata.class);
    when(metadata.currentSnapshot()).thenReturn(snapshot);

    return metadata;
  }

  @Test
  void testOnAfterDropTable() {
    // Setup entity resolution mocks
    setupEntityResolutionMocks("test_catalog", Namespace.of("ns1"), "test_table");

    // Mock deleteAllMetrics to return some deleted metrics
    Map<String, MetricValueWithMetadata> deletedMetrics = new HashMap<>();
    MetricMetadata metadata = new MetricMetadata(System.currentTimeMillis(), 1L, null);
    deletedMetrics.put(
        "added-data-files", new MetricValueWithMetadata(metadata, new PointMetricValue(5L)));
    deletedMetrics.put(
        "total-records", new MetricValueWithMetadata(metadata, new PointMetricValue(1000L)));

    when(mockPersistence.deleteAllMetrics(eq(polarisCallContext), any(PolarisEntityId.class)))
        .thenReturn(deletedMetrics);

    IcebergRestCatalogEvents.AfterDropTableEvent event =
        new IcebergRestCatalogEvents.AfterDropTableEvent(
            "test_catalog", Namespace.of("ns1"), "test_table", false);

    // Trigger event
    listener.onAfterDropTable(event);

    // Verify deleteAllMetrics was called
    verify(mockPersistence).deleteAllMetrics(eq(polarisCallContext), any(PolarisEntityId.class));
  }

  @Test
  void testOnAfterDropTableWithNoMetrics() {
    // Setup entity resolution mocks
    setupEntityResolutionMocks("test_catalog", Namespace.of("ns1"), "test_table");

    // Mock deleteAllMetrics to return empty map (no metrics existed)
    when(mockPersistence.deleteAllMetrics(eq(polarisCallContext), any(PolarisEntityId.class)))
        .thenReturn(Map.of());

    IcebergRestCatalogEvents.AfterDropTableEvent event =
        new IcebergRestCatalogEvents.AfterDropTableEvent(
            "test_catalog", Namespace.of("ns1"), "test_table", false);

    // Trigger event
    listener.onAfterDropTable(event);

    // Verify deleteAllMetrics was called
    verify(mockPersistence).deleteAllMetrics(eq(polarisCallContext), any(PolarisEntityId.class));
  }
}
