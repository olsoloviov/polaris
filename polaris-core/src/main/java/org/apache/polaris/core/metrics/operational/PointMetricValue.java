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

/** A simple point metric with a single numeric value. */
public class PointMetricValue implements MetricValue {
  private final Number value;

  @JsonCreator
  public PointMetricValue(@JsonProperty("value") @Nonnull Number value) {
    this.value = Objects.requireNonNull(value, "value cannot be null");
  }

  @Override
  @JsonProperty("type")
  public MetricType getType() {
    return MetricType.POINT;
  }

  @JsonProperty("value")
  @Nonnull
  public Number getValue() {
    return value;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof PointMetricValue)) return false;
    PointMetricValue that = (PointMetricValue) o;
    return Objects.equals(value, that.value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(value);
  }

  @Override
  public String toString() {
    return "PointMetricValue{" + "value=" + value + '}';
  }
}
