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

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import org.apache.iceberg.Snapshot;
import org.apache.polaris.core.metrics.operational.MetricDefinition;
import org.apache.polaris.core.metrics.operational.MetricMetadata;
import org.apache.polaris.core.metrics.operational.MetricType;
import org.apache.polaris.core.metrics.operational.MetricValueWithMetadata;
import org.apache.polaris.core.metrics.operational.PointMetricValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Reads operational metrics from Iceberg snapshot summaries.
 *
 * <p>This class is responsible for parsing Iceberg snapshot summary properties and converting them
 * into operational metrics that can be stored via the metrics persistence SPI. It validates that
 * metrics are registered in the {@link OperationalMetricsRegistry} and handles type conversions and
 * parsing errors gracefully.
 *
 * <p>Currently supports reading point metrics (numeric long values) from snapshot summaries.
 * Distribution metrics and other metric types are not yet supported for automatic reading from
 * snapshots.
 */
@ApplicationScoped
public class SnapshotMetricsReader {
  private static final Logger LOGGER = LoggerFactory.getLogger(SnapshotMetricsReader.class);

  private final OperationalMetricsRegistry metricsRegistry;

  @Inject
  public SnapshotMetricsReader(OperationalMetricsRegistry metricsRegistry) {
    this.metricsRegistry = metricsRegistry;
  }

  /**
   * Reads operational metrics from an Iceberg snapshot.
   *
   * <p>This method examines the snapshot's summary properties and reads any metrics that are:
   *
   * <ul>
   *   <li>Present in the snapshot summary
   *   <li>Registered in the operational metrics registry
   *   <li>Of type POINT (numeric long values)
   *   <li>Successfully parseable as long values
   * </ul>
   *
   * <p>Metrics that don't meet these criteria are skipped with appropriate logging.
   *
   * @param snapshot the Iceberg snapshot to read metrics from
   * @return map of metric name to metric value with metadata, empty if no valid metrics found
   */
  public Map<String, MetricValueWithMetadata> readMetrics(Snapshot snapshot) {
    if (snapshot == null) {
      LOGGER.debug("Snapshot is null, no metrics to read");
      return Map.of();
    }

    Map<String, String> snapshotSummary = snapshot.summary();
    if (snapshotSummary == null || snapshotSummary.isEmpty()) {
      LOGGER.debug("Snapshot summary is empty, no metrics to read");
      return Map.of();
    }

    return readMetrics(snapshotSummary, snapshot.sequenceNumber());
  }

  /**
   * Reads operational metrics from snapshot summary properties.
   *
   * <p>This is the core reading logic that processes each property in the snapshot summary,
   * validates it against the metrics registry, and converts it to the appropriate metric value
   * type.
   *
   * @param snapshotSummary the snapshot summary properties
   * @param sequenceNumber the Iceberg sequence number for metadata
   * @return map of metric name to metric value with metadata, empty if no valid metrics found
   */
  public Map<String, MetricValueWithMetadata> readMetrics(
      Map<String, String> snapshotSummary, long sequenceNumber) {
    Map<String, MetricValueWithMetadata> metrics = new HashMap<>();
    long timestamp = System.currentTimeMillis();

    for (Map.Entry<String, String> entry : snapshotSummary.entrySet()) {
      String metricName = entry.getKey();
      String metricValueStr = entry.getValue();

      MetricDefinition definition = metricsRegistry.getDefinition(metricName);
      if (definition == null) {
        LOGGER.trace("Metric '{}' not registered, skipping", metricName);
        continue;
      }

      // Only support point metrics (long values) from snapshot summary
      if (definition.getType() != MetricType.POINT) {
        LOGGER.debug(
            "Metric '{}' is not a point metric (type: {}), skipping",
            metricName,
            definition.getType());
        continue;
      }

      try {
        long value = Long.parseLong(metricValueStr);
        MetricMetadata metadata = new MetricMetadata(timestamp, sequenceNumber, null);
        PointMetricValue metricValue = new PointMetricValue(value);
        metrics.put(metricName, new MetricValueWithMetadata(metadata, metricValue));
        LOGGER.trace("Read metric '{}' with value {}", metricName, value);
      } catch (NumberFormatException e) {
        LOGGER.debug(
            "Failed to parse metric '{}' value '{}' as long, skipping", metricName, metricValueStr);
      }
    }

    return metrics;
  }
}
