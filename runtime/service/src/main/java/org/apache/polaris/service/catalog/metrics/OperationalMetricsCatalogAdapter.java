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

import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import java.util.Map;
import org.apache.iceberg.catalog.Namespace;
import org.apache.iceberg.catalog.TableIdentifier;
import org.apache.iceberg.rest.RESTUtil;
import org.apache.polaris.core.PolarisDiagnostics;
import org.apache.polaris.core.auth.PolarisAuthorizer;
import org.apache.polaris.core.catalog.ExternalCatalogFactory;
import org.apache.polaris.core.context.CallContext;
import org.apache.polaris.core.context.RealmContext;
import org.apache.polaris.core.credentials.PolarisCredentialManager;
import org.apache.polaris.core.metrics.operational.OperationalMetricsPersistenceFactory;
import org.apache.polaris.core.persistence.resolver.ResolutionManifestFactory;
import org.apache.polaris.service.catalog.CatalogPrefixParser;
import org.apache.polaris.service.catalog.api.PolarisCatalogOperationalMetricsApiService;
import org.apache.polaris.service.catalog.common.CatalogAdapter;
import org.apache.polaris.service.metrics.operational.OperationalMetricsRegistry;
import org.apache.polaris.service.types.MetricDefinitionsResponse;
import org.apache.polaris.service.types.MetricValue;
import org.apache.polaris.service.types.MultipleMetricsResponse;
import org.apache.polaris.service.types.SingleMetricResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Adapter for operational metrics API endpoints.
 *
 * <p>This adapter implements the REST API for submitting and retrieving operational metrics for
 * Iceberg tables. It delegates to {@link OperationalMetricsCatalogHandler} for business logic.
 */
@RequestScoped
public class OperationalMetricsCatalogAdapter
    implements PolarisCatalogOperationalMetricsApiService, CatalogAdapter {
  private static final Logger LOGGER =
      LoggerFactory.getLogger(OperationalMetricsCatalogAdapter.class);

  private final PolarisDiagnostics diagnostics;
  private final RealmContext realmContext;
  private final CallContext callContext;
  private final ResolutionManifestFactory resolutionManifestFactory;
  private final PolarisAuthorizer polarisAuthorizer;
  private final CatalogPrefixParser prefixParser;
  private final PolarisCredentialManager polarisCredentialManager;
  private final Instance<ExternalCatalogFactory> externalCatalogFactories;
  private final OperationalMetricsPersistenceFactory persistenceFactory;
  private final OperationalMetricsRegistry metricsRegistry;

  @Inject
  public OperationalMetricsCatalogAdapter(
      PolarisDiagnostics diagnostics,
      RealmContext realmContext,
      CallContext callContext,
      ResolutionManifestFactory resolutionManifestFactory,
      PolarisAuthorizer polarisAuthorizer,
      CatalogPrefixParser prefixParser,
      PolarisCredentialManager polarisCredentialManager,
      @Any Instance<ExternalCatalogFactory> externalCatalogFactories,
      OperationalMetricsPersistenceFactory persistenceFactory,
      OperationalMetricsRegistry metricsRegistry) {
    this.diagnostics = diagnostics;
    this.realmContext = realmContext;
    this.callContext = callContext;
    this.resolutionManifestFactory = resolutionManifestFactory;
    this.polarisAuthorizer = polarisAuthorizer;
    this.prefixParser = prefixParser;
    this.polarisCredentialManager = polarisCredentialManager;
    this.externalCatalogFactories = externalCatalogFactories;
    this.persistenceFactory = persistenceFactory;
    this.metricsRegistry = metricsRegistry;
  }

  private OperationalMetricsCatalogHandler newHandlerWrapper(
      SecurityContext securityContext, String prefix) {
    validatePrincipal(securityContext);

    return new OperationalMetricsCatalogHandler(
        diagnostics,
        callContext,
        resolutionManifestFactory,
        securityContext,
        prefixParser.prefixToCatalogName(realmContext, prefix),
        polarisAuthorizer,
        polarisCredentialManager,
        externalCatalogFactories,
        persistenceFactory,
        metricsRegistry);
  }

  @Override
  public Response submitMetrics(
      String prefix,
      String namespace,
      String table,
      Map<String, MetricValue> requestBody,
      RealmContext realmContext,
      SecurityContext securityContext) {
    Namespace ns = decodeNamespace(namespace);
    TableIdentifier tableIdentifier = TableIdentifier.of(ns, RESTUtil.decodeString(table));
    OperationalMetricsCatalogHandler handler = newHandlerWrapper(securityContext, prefix);
    handler.submitMetrics(tableIdentifier, requestBody);
    return Response.noContent().build();
  }

  @Override
  public Response getAllMetrics(
      String prefix,
      String namespace,
      String table,
      RealmContext realmContext,
      SecurityContext securityContext) {
    Namespace ns = decodeNamespace(namespace);
    TableIdentifier tableIdentifier = TableIdentifier.of(ns, RESTUtil.decodeString(table));
    OperationalMetricsCatalogHandler handler = newHandlerWrapper(securityContext, prefix);
    MultipleMetricsResponse response = handler.getAllMetrics(tableIdentifier);
    return Response.ok(response).build();
  }

  @Override
  public Response getSingleMetric(
      String prefix,
      String namespace,
      String table,
      String metric,
      RealmContext realmContext,
      SecurityContext securityContext) {
    Namespace ns = decodeNamespace(namespace);
    TableIdentifier tableIdentifier = TableIdentifier.of(ns, RESTUtil.decodeString(table));
    String metricName = RESTUtil.decodeString(metric);
    OperationalMetricsCatalogHandler handler = newHandlerWrapper(securityContext, prefix);
    SingleMetricResponse response = handler.getSingleMetric(tableIdentifier, metricName);
    return Response.ok(response).build();
  }

  @Override
  public Response getMetricDefinitions(
      String prefix, RealmContext realmContext, SecurityContext securityContext) {
    OperationalMetricsCatalogHandler handler = newHandlerWrapper(securityContext, prefix);
    MetricDefinitionsResponse response = handler.getMetricDefinitions();
    return Response.ok(response).build();
  }
}
