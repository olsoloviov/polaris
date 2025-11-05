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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.annotation.Nonnull;
import java.util.Objects;

/**
 * Represents a metric value along with its associated metadata.
 *
 * <p>This class encapsulates both the metric value (which can be either a point metric or a
 * distribution metric) and the metadata that provides context about when and how the metric was
 * computed.
 */
public class MetricValueWithMetadata {
  private final MetricMetadata metadata;
  private final MetricValue metric;

  @JsonCreator
  public MetricValueWithMetadata(
      @JsonProperty("metadata") @Nonnull MetricMetadata metadata,
      @JsonProperty("metric") @Nonnull MetricValue metric) {
    this.metadata = Objects.requireNonNull(metadata, "metadata cannot be null");
    this.metric = Objects.requireNonNull(metric, "metric cannot be null");
  }

  @JsonProperty("metadata")
  @Nonnull
  public MetricMetadata getMetadata() {
    return metadata;
  }

  @JsonProperty("metric")
  @Nonnull
  public MetricValue getMetric() {
    return metric;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof MetricValueWithMetadata)) return false;
    MetricValueWithMetadata that = (MetricValueWithMetadata) o;
    return Objects.equals(metadata, that.metadata) && Objects.equals(metric, that.metric);
  }

  @Override
  public int hashCode() {
    return Objects.hash(metadata, metric);
  }

  @Override
  public String toString() {
    return "MetricValueWithMetadata{" + "metadata=" + metadata + ", metric=" + metric + '}';
  }
}
