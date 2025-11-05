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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.common.annotation.Identifier;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.SecurityContext;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.apache.iceberg.catalog.TableIdentifier;
import org.apache.polaris.core.PolarisCallContext;
import org.apache.polaris.core.PolarisDiagnostics;
import org.apache.polaris.core.auth.PolarisAuthorizer;
import org.apache.polaris.core.catalog.ExternalCatalogFactory;
import org.apache.polaris.core.context.CallContext;
import org.apache.polaris.core.context.RealmContext;
import org.apache.polaris.core.credentials.PolarisCredentialManager;
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
import org.apache.polaris.service.metrics.operational.OperationalMetricsRegistry;
import org.apache.polaris.service.types.MetricDefinitionsResponse;
import org.apache.polaris.service.types.MultipleMetricsResponse;
import org.apache.polaris.service.types.SingleMetricResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
class OperationalMetricsCatalogHandlerTest {

  @Inject PolarisDiagnostics diagnostics;

  @Inject OperationalMetricsRegistry metricsRegistry;

  @InjectMock
  @Identifier("in-memory")
  OperationalMetricsPersistenceFactory mockPersistenceFactory;

  @InjectMock PolarisAuthorizer authorizer;
  @InjectMock PolarisCredentialManager credentialManager;
  @InjectMock ResolutionManifestFactory resolutionManifestFactory;

  private CallContext callContext;
  private PolarisCallContext polarisCallContext;
  private RealmContext realmContext;
  private SecurityContext securityContext;
  private PolarisResolutionManifest resolutionManifest;
  private OperationalMetricsCatalogHandler handler;
  private OperationalMetricsPersistence mockPersistence;

  @BeforeEach
  void setUp() {
    // Setup call context
    realmContext = () -> "test-realm";
    polarisCallContext = mock(PolarisCallContext.class);
    callContext = mock(CallContext.class);
    when(callContext.getRealmContext()).thenReturn(realmContext);
    when(callContext.getPolarisCallContext()).thenReturn(polarisCallContext);

    // Setup security context with a PolarisPrincipal
    securityContext = mock(SecurityContext.class);
    org.apache.polaris.core.auth.PolarisPrincipal userPrincipal =
        org.apache.polaris.core.auth.PolarisPrincipal.of("test-user", Map.of(), Set.of());
    when(securityContext.getUserPrincipal()).thenReturn(userPrincipal);

    // Setup resolution manifest - will be created on demand by the handler
    resolutionManifest = mock(PolarisResolutionManifest.class);
    when(resolutionManifestFactory.createResolutionManifest(
            any(SecurityContext.class), any(String.class)))
        .thenReturn(resolutionManifest);

    // Mock the resolution manifest methods that will be called
    // addPassthroughPath returns void, so we use doNothing()
    org.mockito.Mockito.doNothing().when(resolutionManifest).addPassthroughPath(any(), any());
    when(resolutionManifest.resolveAll())
        .thenReturn(mock(org.apache.polaris.core.persistence.resolver.ResolverStatus.class));

    // Mock external catalog factories
    @SuppressWarnings("unchecked")
    Instance<ExternalCatalogFactory> externalCatalogFactories = mock(Instance.class);
    when(externalCatalogFactories.select(any())).thenReturn(externalCatalogFactories);
    when(externalCatalogFactories.isUnsatisfied()).thenReturn(true);

    // Mock persistence
    mockPersistence = mock(OperationalMetricsPersistence.class);
    when(mockPersistenceFactory.getOrCreatePersistence(any())).thenReturn(mockPersistence);

    // Create handler
    handler =
        new OperationalMetricsCatalogHandler(
            diagnostics,
            callContext,
            resolutionManifestFactory,
            securityContext,
            "test-catalog",
            authorizer,
            credentialManager,
            externalCatalogFactories,
            mockPersistenceFactory,
            metricsRegistry);
  }

  @Test
  void testGetMetricDefinitions() {
    // Call handler
    MetricDefinitionsResponse response = handler.getMetricDefinitions();

    // Verify response contains expected definitions
    assertThat(response.getMetricDefinitions()).isNotEmpty();
    assertThat(response.getMetricDefinitions()).hasSize(10);

    // Verify some known metrics
    assertThat(response.getMetricDefinitions())
        .anyMatch(def -> def.getName().equals("added-data-files"));
    assertThat(response.getMetricDefinitions())
        .anyMatch(def -> def.getName().equals("total-records"));
  }

  @Test
  void testSubmitMetrics() {
    // Setup table resolution
    TableIdentifier tableId = TableIdentifier.of("ns1", "table1");
    setupTableResolution(tableId, 1L, 100L);

    // Create DTO metrics to submit - now easy with proper inheritance!
    Map<String, org.apache.polaris.service.types.MetricValue> dtoMetrics = new HashMap<>();

    // Create a PointMetric
    org.apache.polaris.service.types.PointMetric pointMetric =
        org.apache.polaris.service.types.PointMetric.builder()
            .setType("point")
            .setValue(new java.math.BigDecimal("42"))
            .build();
    Map<String, Object> pointMetadata = new HashMap<>();
    pointMetadata.put("last-updated-timestamp", 1234567890L);
    dtoMetrics.put(
        "added-data-files",
        org.apache.polaris.service.types.MetricValue.builder()
            .setMetadata(pointMetadata)
            .setMetric(pointMetric)
            .build());

    // Create another PointMetric
    org.apache.polaris.service.types.PointMetric pointMetric2 =
        org.apache.polaris.service.types.PointMetric.builder()
            .setType("point")
            .setValue(new java.math.BigDecimal("1000"))
            .build();
    Map<String, Object> pointMetadata2 = new HashMap<>();
    pointMetadata2.put("last-updated-timestamp", 1234567890L);
    dtoMetrics.put(
        "total-records",
        org.apache.polaris.service.types.MetricValue.builder()
            .setMetadata(pointMetadata2)
            .setMetric(pointMetric2)
            .build());

    // Call handler
    handler.submitMetrics(tableId, dtoMetrics);

    // Verify persistence was called with converted core domain objects
    verify(mockPersistence, times(1))
        .storeMetrics(
            any(),
            any(),
            argThat(
                metrics -> {
                  // Verify we have 2 metrics
                  if (metrics.size() != 2) return false;

                  // Verify point metric
                  org.apache.polaris.core.metrics.operational.MetricValueWithMetadata pointCore =
                      metrics.get("added-data-files");
                  if (pointCore == null) return false;
                  if (!(pointCore.getMetric()
                      instanceof org.apache.polaris.core.metrics.operational.PointMetricValue))
                    return false;
                  org.apache.polaris.core.metrics.operational.PointMetricValue pointValue =
                      (org.apache.polaris.core.metrics.operational.PointMetricValue)
                          pointCore.getMetric();
                  // Compare as double to handle different Number types
                  if (pointValue.getValue().doubleValue() != 42.0) return false;

                  // Verify second point metric
                  org.apache.polaris.core.metrics.operational.MetricValueWithMetadata pointCore2 =
                      metrics.get("total-records");
                  if (pointCore2 == null) return false;
                  if (!(pointCore2.getMetric()
                      instanceof org.apache.polaris.core.metrics.operational.PointMetricValue))
                    return false;
                  org.apache.polaris.core.metrics.operational.PointMetricValue pointValue2 =
                      (org.apache.polaris.core.metrics.operational.PointMetricValue)
                          pointCore2.getMetric();
                  // Compare as double to handle different Number types
                  if (pointValue2.getValue().doubleValue() != 1000.0) return false;

                  return true;
                }));
  }

  @Test
  void testGetAllMetrics() {
    // Setup table resolution
    TableIdentifier tableId = TableIdentifier.of("ns1", "table1");
    setupTableResolution(tableId, 1L, 100L);

    // Mock persistence to return metrics
    Map<String, MetricValueWithMetadata> coreMetrics = new HashMap<>();
    MetricMetadata metadata = new MetricMetadata(1234567890L, 1L, null);
    coreMetrics.put(
        "added-data-files", new MetricValueWithMetadata(metadata, new PointMetricValue(5L)));
    coreMetrics.put(
        "total-records", new MetricValueWithMetadata(metadata, new PointMetricValue(1000L)));

    when(mockPersistence.getAllMetrics(any(), any())).thenReturn(coreMetrics);

    // Call handler
    MultipleMetricsResponse response = handler.getAllMetrics(tableId);

    // Verify response
    assertThat(response.getMetrics()).hasSize(2);
    assertThat(response.getMetrics())
        .anyMatch(entry -> entry.getMetadata().getName().equals("added-data-files"));
    assertThat(response.getMetrics())
        .anyMatch(entry -> entry.getMetadata().getName().equals("total-records"));
  }

  @Test
  void testGetSingleMetric() {
    // Setup table resolution
    TableIdentifier tableId = TableIdentifier.of("ns1", "table1");
    setupTableResolution(tableId, 1L, 100L);

    // Mock persistence to return metric
    MetricMetadata metadata = new MetricMetadata(1234567890L, 1L, null);
    MetricValueWithMetadata coreMetric =
        new MetricValueWithMetadata(metadata, new PointMetricValue(5L));

    when(mockPersistence.getSingleMetric(any(), any(), eq("added-data-files")))
        .thenReturn(coreMetric);

    // Call handler
    SingleMetricResponse response = handler.getSingleMetric(tableId, "added-data-files");

    // Verify response
    assertThat(response.getMetrics()).hasSize(1);
    assertThat(response.getMetrics().get(0).getMetadata().getName()).isEqualTo("added-data-files");
  }

  @Test
  void testGetSingleMetricNotFound() {
    // Setup table resolution
    TableIdentifier tableId = TableIdentifier.of("ns1", "table1");
    setupTableResolution(tableId, 1L, 100L);

    // Mock persistence to return null (metric not found)
    when(mockPersistence.getSingleMetric(any(), any(), eq("added-data-files"))).thenReturn(null);

    // Call handler
    SingleMetricResponse response = handler.getSingleMetric(tableId, "added-data-files");

    // Verify response has empty metrics list
    assertThat(response.getMetrics()).hasSize(1);
    assertThat(response.getMetrics().get(0)).isNull();
  }

  /**
   * Helper method to setup table resolution mocks.
   *
   * @param tableIdentifier the table identifier
   * @param catalogId the catalog ID
   * @param entityId the entity ID
   */
  private void setupTableResolution(
      TableIdentifier tableIdentifier, long catalogId, long entityId) {
    // Create a mock entity that implements PolarisEntity interface
    org.apache.polaris.core.entity.PolarisEntity tableEntity =
        mock(org.apache.polaris.core.entity.PolarisEntity.class);
    when(tableEntity.getCatalogId()).thenReturn(catalogId);
    when(tableEntity.getId()).thenReturn(entityId);

    PolarisResolvedPathWrapper tableWrapper = mock(PolarisResolvedPathWrapper.class);
    when(tableWrapper.getRawLeafEntity()).thenReturn(tableEntity);

    // Mock the resolution manifest to return the wrapper for this table
    // The handler calls getResolvedPath with 4 parameters: (identifier, type, subtype, boolean)
    when(resolutionManifest.getResolvedPath(
            eq(tableIdentifier),
            eq(PolarisEntityType.TABLE_LIKE),
            eq(PolarisEntitySubType.ICEBERG_TABLE),
            eq(true)))
        .thenReturn(tableWrapper);

    // Also mock the 3-parameter version used by the handler after authorization
    when(resolutionManifest.getResolvedPath(
            eq(tableIdentifier),
            eq(PolarisEntityType.TABLE_LIKE),
            eq(PolarisEntitySubType.ICEBERG_TABLE)))
        .thenReturn(tableWrapper);
  }
}
