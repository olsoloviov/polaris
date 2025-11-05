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

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.annotation.Nullable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Metadata associated with a metric value.
 *
 * <p>Contains optional timestamp, Iceberg sequence number, and snapshot ID for tracking metric
 * computation context. Additional custom properties can be stored via {@link
 * #additionalProperties}.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MetricMetadata {
  private final Long timestamp;
  private final Long icebergSequenceNumber;
  private final Long icebergSnapshotId;
  private final Map<String, Object> additionalProperties;

  @JsonCreator
  public MetricMetadata(
      @JsonProperty("timestamp") @Nullable Long timestamp,
      @JsonProperty("iceberg-sequence-number") @Nullable Long icebergSequenceNumber,
      @JsonProperty("iceberg-snapshot-id") @Nullable Long icebergSnapshotId) {
    this.timestamp = timestamp;
    this.icebergSequenceNumber = icebergSequenceNumber;
    this.icebergSnapshotId = icebergSnapshotId;
    this.additionalProperties = new HashMap<>();
  }

  /** Creates an empty metadata object. */
  public static MetricMetadata empty() {
    return new MetricMetadata(null, null, null);
  }

  @JsonProperty("timestamp")
  @Nullable
  public Long getTimestamp() {
    return timestamp;
  }

  @JsonProperty("iceberg-sequence-number")
  @Nullable
  public Long getIcebergSequenceNumber() {
    return icebergSequenceNumber;
  }

  @JsonProperty("iceberg-snapshot-id")
  @Nullable
  public Long getIcebergSnapshotId() {
    return icebergSnapshotId;
  }

  /**
   * Get additional custom properties.
   *
   * @return unmodifiable map of additional properties
   */
  @JsonIgnore
  public Map<String, Object> getAdditionalProperties() {
    return Collections.unmodifiableMap(additionalProperties);
  }

  /**
   * Set an additional custom property.
   *
   * @param key property key
   * @param value property value
   */
  @JsonAnySetter
  public void setAdditionalProperty(String key, Object value) {
    additionalProperties.put(key, value);
  }

  /**
   * Used by Jackson for serialization of additional properties.
   *
   * @return map of additional properties
   */
  @JsonAnyGetter
  Map<String, Object> getAdditionalPropertiesInternal() {
    return additionalProperties;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof MetricMetadata)) return false;
    MetricMetadata that = (MetricMetadata) o;
    return Objects.equals(timestamp, that.timestamp)
        && Objects.equals(icebergSequenceNumber, that.icebergSequenceNumber)
        && Objects.equals(icebergSnapshotId, that.icebergSnapshotId)
        && Objects.equals(additionalProperties, that.additionalProperties);
  }

  @Override
  public int hashCode() {
    return Objects.hash(timestamp, icebergSequenceNumber, icebergSnapshotId, additionalProperties);
  }

  @Override
  public String toString() {
    return "MetricMetadata{"
        + "timestamp="
        + timestamp
        + ", icebergSequenceNumber="
        + icebergSequenceNumber
        + ", icebergSnapshotId="
        + icebergSnapshotId
        + ", additionalProperties="
        + additionalProperties
        + '}';
  }
}
