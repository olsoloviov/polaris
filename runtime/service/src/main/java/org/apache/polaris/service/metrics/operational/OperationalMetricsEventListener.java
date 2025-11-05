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

import io.smallrye.common.annotation.Identifier;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.Map;
import org.apache.iceberg.Snapshot;
import org.apache.iceberg.TableMetadata;
import org.apache.iceberg.catalog.TableIdentifier;
import org.apache.polaris.core.catalog.PolarisCatalogHelpers;
import org.apache.polaris.core.context.CallContext;
import org.apache.polaris.core.entity.PolarisEntityId;
import org.apache.polaris.core.entity.PolarisEntitySubType;
import org.apache.polaris.core.entity.PolarisEntityType;
import org.apache.polaris.core.metrics.operational.MetricValueWithMetadata;
import org.apache.polaris.core.metrics.operational.OperationalMetricsPersistence;
import org.apache.polaris.core.metrics.operational.OperationalMetricsPersistenceFactory;
import org.apache.polaris.core.persistence.PolarisResolvedPathWrapper;
import org.apache.polaris.core.persistence.resolver.PolarisResolutionManifest;
import org.apache.polaris.core.persistence.resolver.ResolutionManifestFactory;
import org.apache.polaris.core.persistence.resolver.ResolverPath;
import org.apache.polaris.service.events.IcebergRestCatalogEvents;
import org.apache.polaris.service.events.listeners.PolarisEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Event listener that automatically captures and manages operational metrics from Iceberg table
 * lifecycle events.
 *
 * <p>This listener integrates with the Polaris event system to provide automatic metric collection
 * without requiring explicit client action. It responds to table commit and drop events, extracting
 * relevant metrics and persisting them through the operational metrics SPI.
 *
 * <p>The listener uses {@link SnapshotMetricsReader} to read metrics from Iceberg snapshots and the
 * {@link OperationalMetricsPersistence} SPI to store and manage metric data in a pluggable backend.
 *
 * <p>All operations are designed to be non-blocking and fault-tolerant - failures in metric
 * processing will not affect the underlying table operations.
 */
@ApplicationScoped
@Identifier("operational-metrics")
public class OperationalMetricsEventListener implements PolarisEventListener {
  private static final Logger LOGGER =
      LoggerFactory.getLogger(OperationalMetricsEventListener.class);

  private final CallContext callContext;
  private final ResolutionManifestFactory resolutionManifestFactory;
  private final OperationalMetricsPersistenceFactory persistenceFactory;
  private final SnapshotMetricsReader metricsReader;

  @Inject
  public OperationalMetricsEventListener(
      CallContext callContext,
      ResolutionManifestFactory resolutionManifestFactory,
      OperationalMetricsPersistenceFactory persistenceFactory,
      SnapshotMetricsReader metricsReader) {
    this.callContext = callContext;
    this.resolutionManifestFactory = resolutionManifestFactory;
    this.persistenceFactory = persistenceFactory;
    this.metricsReader = metricsReader;
  }

  /**
   * Handles table commit events by reading and storing operational metrics.
   *
   * <p>This method is invoked after a successful table commit. It reads metrics from the snapshot
   * summary of the committed table metadata using {@link SnapshotMetricsReader} and stores them via
   * the persistence SPI.
   *
   * <p>Any errors during metric processing are logged but do not affect the table commit operation.
   *
   * @param event the after commit table event containing table metadata and snapshot information
   */
  @Override
  public void onAfterCommitTable(IcebergRestCatalogEvents.AfterCommitTableEvent event) {
    try {
      processTableCommit(event);
    } catch (Exception e) {
      // Log error but don't fail the commit
      LOGGER.error(
          "Failed to process operational metrics for table {} in catalog {}",
          event.identifier(),
          event.catalogName(),
          e);
    }
  }

  /**
   * Handles table drop events by cleaning up associated operational metrics.
   *
   * <p>This method is invoked after a successful table drop. It deletes all operational metrics
   * associated with the dropped table from the persistence backend.
   *
   * <p>Any errors during metric deletion are logged but do not affect the table drop operation.
   *
   * @param event the after drop table event containing table identification information
   */
  @Override
  public void onAfterDropTable(IcebergRestCatalogEvents.AfterDropTableEvent event) {
    try {
      processTableDrop(event);
    } catch (Exception e) {
      // Log error but don't fail the drop
      LOGGER.error(
          "Failed to delete operational metrics for table {}.{} in catalog {}",
          event.namespace(),
          event.table(),
          event.catalogName(),
          e);
    }
  }

  private void processTableCommit(IcebergRestCatalogEvents.AfterCommitTableEvent event) {
    TableMetadata metadataAfter = event.metadataAfter();
    if (metadataAfter == null) {
      LOGGER.trace(
          "No metadata after commit for table {} in catalog {}, skipping metrics",
          event.identifier(),
          event.catalogName());
      return;
    }

    Snapshot currentSnapshot = metadataAfter.currentSnapshot();
    if (currentSnapshot == null) {
      LOGGER.trace(
          "No current snapshot for table {} in catalog {}, skipping metrics",
          event.identifier(),
          event.catalogName());
      return;
    }

    Map<String, MetricValueWithMetadata> metrics = metricsReader.readMetrics(currentSnapshot);

    if (metrics.isEmpty()) {
      LOGGER.trace(
          "No supported metrics found in snapshot for table {} in catalog {}",
          event.identifier(),
          event.catalogName());
      return;
    }

    storeMetrics(event, metrics);
  }

  private void processTableDrop(IcebergRestCatalogEvents.AfterDropTableEvent event) {
    TableIdentifier tableIdentifier = TableIdentifier.of(event.namespace(), event.table());
    PolarisEntityId tableId = resolveTableEntity(event.catalogName(), tableIdentifier);
    if (tableId == null) {
      LOGGER.debug(
          "Could not resolve table entity for {}.{} in catalog {}, skipping metrics deletion",
          event.namespace(),
          event.table(),
          event.catalogName());
      return;
    }

    OperationalMetricsPersistence persistence =
        persistenceFactory.getOrCreatePersistence(callContext.getRealmContext());

    Map<String, MetricValueWithMetadata> deletedMetrics =
        persistence.deleteAllMetrics(callContext.getPolarisCallContext(), tableId);

    LOGGER.debug(
        "Deleted {} operational metrics for table {}.{} in catalog {}",
        deletedMetrics.size(),
        event.namespace(),
        event.table(),
        event.catalogName());
  }

  /**
   * Store metrics via persistence SPI.
   *
   * @param event the after commit table event
   * @param metrics the metrics to store
   */
  private void storeMetrics(
      IcebergRestCatalogEvents.AfterCommitTableEvent event,
      Map<String, MetricValueWithMetadata> metrics) {
    PolarisEntityId tableId = resolveTableEntity(event.catalogName(), event.identifier());
    if (tableId == null) {
      LOGGER.warn(
          "Could not resolve table entity for {} in catalog {}, skipping metrics storage",
          event.identifier(),
          event.catalogName());
      return;
    }

    OperationalMetricsPersistence persistence =
        persistenceFactory.getOrCreatePersistence(callContext.getRealmContext());

    persistence.storeMetrics(callContext.getPolarisCallContext(), tableId, metrics);

    LOGGER.debug(
        "Stored {} operational metrics for table {} in catalog {}",
        metrics.size(),
        event.identifier(),
        event.catalogName());
  }

  /**
   * Resolve table entity from catalog name and table identifier.
   *
   * <p>Uses the {@link ResolutionManifestFactory} to create a passthrough resolution manifest that
   * bypasses authorization checks. This is appropriate for event listeners that operate in an
   * elevated-privilege context after the original operation has already been authorized.
   *
   * @param catalogName the catalog name
   * @param tableIdentifier the table identifier
   * @return the table entity ID, or null if resolution fails
   */
  private PolarisEntityId resolveTableEntity(String catalogName, TableIdentifier tableIdentifier) {
    try {
      PolarisResolutionManifest manifest =
          resolutionManifestFactory.createResolutionManifest(null, catalogName);

      manifest.addPassthroughPath(
          new ResolverPath(
              PolarisCatalogHelpers.tableIdentifierToList(tableIdentifier),
              PolarisEntityType.TABLE_LIKE),
          tableIdentifier);
      manifest.resolveAll();

      PolarisResolvedPathWrapper tableWrapper =
          manifest.getResolvedPath(
              tableIdentifier, PolarisEntityType.TABLE_LIKE, PolarisEntitySubType.ICEBERG_TABLE);

      if (tableWrapper == null) {
        return null;
      }

      return new PolarisEntityId(
          tableWrapper.getRawLeafEntity().getCatalogId(), tableWrapper.getRawLeafEntity().getId());

    } catch (Exception e) {
      LOGGER.warn(
          "Error resolving table entity for {} in catalog {}: {}",
          tableIdentifier,
          catalogName,
          e.getMessage());
      LOGGER.debug("Full stack trace for resolution error", e);
      return null;
    }
  }
}
