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
package org.apache.polaris.service.catalog.metrics;

import jakarta.enterprise.inject.Instance;
import jakarta.ws.rs.core.SecurityContext;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.iceberg.catalog.TableIdentifier;
import org.apache.polaris.core.PolarisDiagnostics;
import org.apache.polaris.core.auth.PolarisAuthorizableOperation;
import org.apache.polaris.core.auth.PolarisAuthorizer;
import org.apache.polaris.core.catalog.ExternalCatalogFactory;
import org.apache.polaris.core.context.CallContext;
import org.apache.polaris.core.credentials.PolarisCredentialManager;
import org.apache.polaris.core.entity.PolarisEntityId;
import org.apache.polaris.core.entity.PolarisEntitySubType;
import org.apache.polaris.core.entity.PolarisEntityType;
import org.apache.polaris.core.metrics.operational.MetricDefinition;
import org.apache.polaris.core.metrics.operational.MetricValueWithMetadata;
import org.apache.polaris.core.metrics.operational.OperationalMetricsPersistence;
import org.apache.polaris.core.metrics.operational.OperationalMetricsPersistenceFactory;
import org.apache.polaris.core.persistence.PolarisResolvedPathWrapper;
import org.apache.polaris.core.persistence.resolver.ResolutionManifestFactory;
import org.apache.polaris.service.catalog.common.CatalogHandler;
import org.apache.polaris.service.metrics.operational.OperationalMetricsRegistry;
import org.apache.polaris.service.types.MetricDefinitionsResponse;
import org.apache.polaris.service.types.MetricEntry;
import org.apache.polaris.service.types.MetricValue;
import org.apache.polaris.service.types.MultipleMetricsResponse;
import org.apache.polaris.service.types.SingleMetricResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handler for operational metrics operations.
 *
 * <p>This handler extends {@link CatalogHandler} to provide authorization and business logic for
 * operational metrics operations on Iceberg tables. It delegates to {@link
 * OperationalMetricsPersistence} for storage and retrieval.
 */
public class OperationalMetricsCatalogHandler extends CatalogHandler {
  private static final Logger LOGGER =
      LoggerFactory.getLogger(OperationalMetricsCatalogHandler.class);

  private final OperationalMetricsPersistenceFactory persistenceFactory;
  private final OperationalMetricsRegistry metricsRegistry;
  private final OperationalMetricsConverter metricsConverter;

  public OperationalMetricsCatalogHandler(
      PolarisDiagnostics diagnostics,
      CallContext callContext,
      ResolutionManifestFactory resolutionManifestFactory,
      SecurityContext securityContext,
      String catalogName,
      PolarisAuthorizer authorizer,
      PolarisCredentialManager credentialManager,
      Instance<ExternalCatalogFactory> externalCatalogFactories,
      OperationalMetricsPersistenceFactory persistenceFactory,
      OperationalMetricsRegistry metricsRegistry) {
    super(
        diagnostics,
        callContext,
        resolutionManifestFactory,
        securityContext,
        catalogName,
        authorizer,
        credentialManager,
        externalCatalogFactories);
    this.persistenceFactory = persistenceFactory;
    this.metricsRegistry = metricsRegistry;
    this.metricsConverter = new OperationalMetricsConverter();
  }

  @Override
  protected void initializeCatalog() {
    // No catalog initialization needed for metrics operations
  }

  /**
   * Submit metrics for a table.
   *
   * @param tableIdentifier the table identifier
   * @param metrics map of metric name to metric value
   */
  public void submitMetrics(TableIdentifier tableIdentifier, Map<String, MetricValue> metrics) {
    authorizeBasicTableLikeOperationOrThrow(
        PolarisAuthorizableOperation.SUBMIT_METRICS,
        PolarisEntitySubType.ICEBERG_TABLE,
        tableIdentifier);

    PolarisResolvedPathWrapper tableWrapper =
        resolutionManifest.getResolvedPath(
            tableIdentifier, PolarisEntityType.TABLE_LIKE, PolarisEntitySubType.ICEBERG_TABLE);

    if (tableWrapper == null) {
      throw new IllegalStateException("Table not found after authorization: " + tableIdentifier);
    }

    PolarisEntityId tableId =
        new PolarisEntityId(
            tableWrapper.getRawLeafEntity().getCatalogId(),
            tableWrapper.getRawLeafEntity().getId());

    Map<String, MetricValueWithMetadata> coreMetrics =
        metrics.entrySet().stream()
            .collect(
                Collectors.toMap(
                    Map.Entry::getKey,
                    entry -> {
                      String metricName = entry.getKey();
                      MetricValueWithMetadata coreMetric =
                          metricsConverter.convertDtoToCore(metricName, entry.getValue());
                      metricsRegistry.validateMetric(metricName, coreMetric.getMetric());
                      return coreMetric;
                    }));

    OperationalMetricsPersistence persistence =
        persistenceFactory.getOrCreatePersistence(callContext.getRealmContext());
    persistence.storeMetrics(callContext.getPolarisCallContext(), tableId, coreMetrics);

    LOGGER.debug(
        "Stored {} metrics for table {} (catalogId={}, id={})",
        metrics.size(),
        tableIdentifier,
        tableId.getCatalogId(),
        tableId.getId());
  }

  /**
   * Get all metrics for a table.
   *
   * @param tableIdentifier the table identifier
   * @return response containing all metrics
   */
  public MultipleMetricsResponse getAllMetrics(TableIdentifier tableIdentifier) {
    authorizeBasicTableLikeOperationOrThrow(
        PolarisAuthorizableOperation.GET_ALL_METRICS,
        PolarisEntitySubType.ICEBERG_TABLE,
        tableIdentifier);

    PolarisResolvedPathWrapper tableWrapper =
        resolutionManifest.getResolvedPath(
            tableIdentifier, PolarisEntityType.TABLE_LIKE, PolarisEntitySubType.ICEBERG_TABLE);

    if (tableWrapper == null) {
      throw new IllegalStateException("Table not found after authorization: " + tableIdentifier);
    }

    PolarisEntityId tableId =
        new PolarisEntityId(
            tableWrapper.getRawLeafEntity().getCatalogId(),
            tableWrapper.getRawLeafEntity().getId());

    OperationalMetricsPersistence persistence =
        persistenceFactory.getOrCreatePersistence(callContext.getRealmContext());
    Map<String, MetricValueWithMetadata> coreMetrics =
        persistence.getAllMetrics(callContext.getPolarisCallContext(), tableId);

    return convertCoreToMultipleMetricsResponse(coreMetrics);
  }

  /**
   * Get a single metric for a table.
   *
   * @param tableIdentifier the table identifier
   * @param metricName the metric name
   * @return response containing the metric
   */
  public SingleMetricResponse getSingleMetric(TableIdentifier tableIdentifier, String metricName) {
    authorizeBasicTableLikeOperationOrThrow(
        PolarisAuthorizableOperation.GET_SINGLE_METRIC,
        PolarisEntitySubType.ICEBERG_TABLE,
        tableIdentifier);

    PolarisResolvedPathWrapper tableWrapper =
        resolutionManifest.getResolvedPath(
            tableIdentifier, PolarisEntityType.TABLE_LIKE, PolarisEntitySubType.ICEBERG_TABLE);

    if (tableWrapper == null) {
      throw new IllegalStateException("Table not found after authorization: " + tableIdentifier);
    }

    PolarisEntityId tableId =
        new PolarisEntityId(
            tableWrapper.getRawLeafEntity().getCatalogId(),
            tableWrapper.getRawLeafEntity().getId());

    OperationalMetricsPersistence persistence =
        persistenceFactory.getOrCreatePersistence(callContext.getRealmContext());
    MetricValueWithMetadata coreMetric =
        persistence.getSingleMetric(callContext.getPolarisCallContext(), tableId, metricName);

    return convertCoreToSingleMetricResponse(metricName, coreMetric);
  }

  /**
   * Get metric definitions (list of supported metrics).
   *
   * @return response containing metric definitions
   */
  public MetricDefinitionsResponse getMetricDefinitions() {
    List<MetricDefinition> definitions = metricsRegistry.getAllDefinitions();

    List<org.apache.polaris.service.types.MetricDefinition> dtoDefinitions =
        definitions.stream()
            .map(
                def ->
                    org.apache.polaris.service.types.MetricDefinition.builder()
                        .setName(def.getName())
                        .setType(convertMetricTypeToDto(def.getType()))
                        .setDescription(def.getDescription())
                        .build())
            .collect(Collectors.toList());

    return MetricDefinitionsResponse.builder().setMetricDefinitions(dtoDefinitions).build();
  }

  /**
   * Convert core MetricType to DTO TypeEnum.
   *
   * @param type the core metric type
   * @return the DTO type enum
   */
  private org.apache.polaris.service.types.MetricDefinition.TypeEnum convertMetricTypeToDto(
      org.apache.polaris.core.metrics.operational.MetricType type) {
    switch (type) {
      case POINT:
        return org.apache.polaris.service.types.MetricDefinition.TypeEnum.POINT;
      case DISTRIBUTION:
        return org.apache.polaris.service.types.MetricDefinition.TypeEnum.DISTRIBUTION;
      default:
        throw new IllegalArgumentException("Unknown metric type: " + type);
    }
  }

  /**
   * Convert core domain objects to DTO for multiple metrics response.
   *
   * @param coreMetrics map of metric name to core domain object
   * @return DTO response object
   */
  private MultipleMetricsResponse convertCoreToMultipleMetricsResponse(
      Map<String, MetricValueWithMetadata> coreMetrics) {
    List<MetricEntry> entries =
        coreMetrics.entrySet().stream()
            .map(entry -> metricsConverter.convertCoreToMetricEntry(entry.getKey(), entry.getValue()))
            .collect(Collectors.toList());

    return MultipleMetricsResponse.builder().setMetrics(entries).build();
  }

  /**
   * Convert core domain object to DTO for single metric response.
   *
   * @param metricName the name of the metric
   * @param coreMetric the core domain object (can be null if metric not found)
   * @return DTO response object
   */
  private SingleMetricResponse convertCoreToSingleMetricResponse(
      String metricName, MetricValueWithMetadata coreMetric) {
    if (coreMetric == null) {
      // Metric not found - return response with null entry
      // Use Collections.singletonList instead of List.of since List.of doesn't allow nulls
      return SingleMetricResponse.builder()
          .setMetrics(java.util.Collections.singletonList(null))
          .build();
    }
    MetricEntry entry = metricsConverter.convertCoreToMetricEntry(metricName, coreMetric);
    return SingleMetricResponse.builder().setMetrics(List.of(entry)).build();
  }
}
