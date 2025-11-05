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

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.apache.polaris.core.metrics.operational.MetricMetadata;
import org.apache.polaris.core.metrics.operational.MetricValueWithMetadata;
import org.apache.polaris.service.types.Metric;
import org.apache.polaris.service.types.MetricEntry;
import org.apache.polaris.service.types.MetricEntryMetadata;
import org.apache.polaris.service.types.MetricValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Converter for transforming operational metrics between REST API DTOs and core domain objects.
 *
 * <p>Handles bidirectional conversion:
 *
 * <ul>
 *   <li>DTO → Core: Converts REST API request objects to internal domain model
 *   <li>Core → DTO: Converts internal domain model to REST API response objects
 * </ul>
 *
 * <p>The converter maintains separation between external API contracts (using OpenAPI-specified
 * field names like "last-updated-timestamp") and internal persistence format (using simplified
 * field names like "timestamp").
 */
class OperationalMetricsConverter {
  private static final Logger LOGGER = LoggerFactory.getLogger(OperationalMetricsConverter.class);
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  /**
   * Convert DTO metric (from REST API) to core domain object.
   *
   * @param metricName the name of the metric
   * @param dtoMetric the DTO metric value from REST API
   * @return core domain object with metadata
   * @throws IllegalArgumentException if the metric format is invalid
   */
  MetricValueWithMetadata convertDtoToCore(String metricName, MetricValue dtoMetric) {
    try {
      Map<String, Object> dtoMetadata = dtoMetric.getMetadata();
      Long timestamp =
          dtoMetadata.containsKey("last-updated-timestamp")
              ? ((Number) dtoMetadata.get("last-updated-timestamp")).longValue()
              : null;
      Long sequenceNumber =
          dtoMetadata.containsKey("last-sequence-number")
              ? ((Number) dtoMetadata.get("last-sequence-number")).longValue()
              : null;
      Long snapshotId =
          dtoMetadata.containsKey("last-snapshot-id")
              ? ((Number) dtoMetadata.get("last-snapshot-id")).longValue()
              : null;

      MetricMetadata metadata = new MetricMetadata(timestamp, sequenceNumber, snapshotId);

      org.apache.polaris.core.metrics.operational.MetricValue coreMetricValue =
          OBJECT_MAPPER.convertValue(
              dtoMetric.getMetric(), org.apache.polaris.core.metrics.operational.MetricValue.class);

      return new MetricValueWithMetadata(metadata, coreMetricValue);
    } catch (Exception e) {
      LOGGER.error("Failed to convert DTO metric '{}' to core type", metricName, e);
      throw new IllegalArgumentException(
          "Invalid metric format for '" + metricName + "': " + e.getMessage(), e);
    }
  }

  /**
   * Convert core domain object to DTO MetricEntry.
   *
   * @param metricName the name of the metric
   * @param coreMetric the core domain object
   * @return DTO MetricEntry for REST API response
   * @throws IllegalStateException if conversion fails
   */
  MetricEntry convertCoreToMetricEntry(String metricName, MetricValueWithMetadata coreMetric) {
    try {
      MetricEntryMetadata dtoMetadata = MetricEntryMetadata.builder(metricName).build();
      if (coreMetric.getMetadata().getTimestamp() != null) {
        dtoMetadata.put("last-updated-timestamp", coreMetric.getMetadata().getTimestamp());
      }
      if (coreMetric.getMetadata().getIcebergSequenceNumber() != null) {
        dtoMetadata.put(
            "last-sequence-number", coreMetric.getMetadata().getIcebergSequenceNumber());
      }
      if (coreMetric.getMetadata().getIcebergSnapshotId() != null) {
        dtoMetadata.put("last-snapshot-id", coreMetric.getMetadata().getIcebergSnapshotId());
      }
      coreMetric.getMetadata().getAdditionalProperties().forEach(dtoMetadata::put);

      Metric dtoMetric = convertMetricValueToDto(coreMetric.getMetric());

      return MetricEntry.builder().setMetadata(dtoMetadata).setMetric(dtoMetric).build();
    } catch (Exception e) {
      LOGGER.error("Failed to convert core metric '{}' to DTO", metricName, e);
      throw new IllegalStateException(
          "Failed to convert metric '" + metricName + "' to response format: " + e.getMessage(), e);
    }
  }

  /**
   * Convert core MetricValue to DTO Metric.
   *
   * <p>Constructs the appropriate DTO subtype (PointMetric or DistributionMetric) based on the
   * core type. Both subtypes properly extend Metric using allOf in the OpenAPI spec.
   *
   * @param coreMetric the core metric value
   * @return the DTO metric (either PointMetric or DistributionMetric)
   * @throws IllegalArgumentException if the metric type is unknown
   */
  private Metric convertMetricValueToDto(
      org.apache.polaris.core.metrics.operational.MetricValue coreMetric) {
    if (coreMetric
        instanceof org.apache.polaris.core.metrics.operational.PointMetricValue pointValue) {
      return org.apache.polaris.service.types.PointMetric.builder()
          .setType("point")
          .setValue(toBigDecimal(pointValue.getValue()))
          .build();
    } else if (coreMetric
        instanceof org.apache.polaris.core.metrics.operational.DistributionMetricValue distValue) {
      org.apache.polaris.core.metrics.operational.DistributionValue coreDistValue =
          distValue.getValue();

      org.apache.polaris.service.types.DistributionValue dtoDistValue =
          org.apache.polaris.service.types.DistributionValue.builder()
              .setMin(toBigDecimal(coreDistValue.getMin()))
              .setP10(toBigDecimal(coreDistValue.getP10()))
              .setP25(toBigDecimal(coreDistValue.getP25()))
              .setP50(toBigDecimal(coreDistValue.getP50()))
              .setP75(toBigDecimal(coreDistValue.getP75()))
              .setP90(toBigDecimal(coreDistValue.getP90()))
              .setP99(toBigDecimal(coreDistValue.getP99()))
              .setP999(toBigDecimal(coreDistValue.getP999()))
              .setP9999(toBigDecimal(coreDistValue.getP9999()))
              .setMax(toBigDecimal(coreDistValue.getMax()))
              .setAverage(toBigDecimal(coreDistValue.getAverage()))
              .setStandardDeviation(toBigDecimal(coreDistValue.getStandardDeviation()))
              .setTotal(toBigDecimal(coreDistValue.getTotal()))
              .build();

      return org.apache.polaris.service.types.DistributionMetric.builder()
          .setType("distribution")
          .setValue(dtoDistValue)
          .build();
    } else {
      throw new IllegalArgumentException("Unknown metric value type: " + coreMetric.getClass());
    }
  }

  /**
   * Convert a Number to BigDecimal, handling null values.
   *
   * @param number the number to convert (may be null)
   * @return BigDecimal representation, or null if input is null
   */
  private java.math.BigDecimal toBigDecimal(Number number) {
    if (number == null) {
      return null;
    }
    if (number instanceof java.math.BigDecimal bigDecimal) {
      return bigDecimal;
    }
    return new java.math.BigDecimal(number.toString());
  }
}

