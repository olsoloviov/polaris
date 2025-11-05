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
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.Objects;

/**
 * Statistical distribution data with percentiles and summary statistics.
 *
 * <p>Contains percentile values (min, p10, p25, p50, p75, p90, p99, p99.9, p99.99, max) as well as
 * summary statistics (average, standard deviation, total).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DistributionValue {
  private final Number min;
  private final Number p10;
  private final Number p25;
  private final Number p50;
  private final Number p75;
  private final Number p90;
  private final Number p99;
  private final Number p999;
  private final Number p9999;
  private final Number max;
  private final Number average;
  private final Number standardDeviation;
  private final Number total;

  @JsonCreator
  public DistributionValue(
      @JsonProperty("min") @Nonnull Number min,
      @JsonProperty("p10") @Nullable Number p10,
      @JsonProperty("p25") @Nullable Number p25,
      @JsonProperty("p50") @Nonnull Number p50,
      @JsonProperty("p75") @Nullable Number p75,
      @JsonProperty("p90") @Nullable Number p90,
      @JsonProperty("p99") @Nullable Number p99,
      @JsonProperty("p999") @Nullable Number p999,
      @JsonProperty("p9999") @Nullable Number p9999,
      @JsonProperty("max") @Nonnull Number max,
      @JsonProperty("average") @Nonnull Number average,
      @JsonProperty("standard-deviation") @Nullable Number standardDeviation,
      @JsonProperty("total") @Nullable Number total) {
    this.min = Objects.requireNonNull(min, "min cannot be null");
    this.p10 = p10;
    this.p25 = p25;
    this.p50 = Objects.requireNonNull(p50, "p50 cannot be null");
    this.p75 = p75;
    this.p90 = p90;
    this.p99 = p99;
    this.p999 = p999;
    this.p9999 = p9999;
    this.max = Objects.requireNonNull(max, "max cannot be null");
    this.average = Objects.requireNonNull(average, "average cannot be null");
    this.standardDeviation = standardDeviation;
    this.total = total;
  }

  @JsonProperty("min")
  @Nonnull
  public Number getMin() {
    return min;
  }

  @JsonProperty("p10")
  @Nullable
  public Number getP10() {
    return p10;
  }

  @JsonProperty("p25")
  @Nullable
  public Number getP25() {
    return p25;
  }

  @JsonProperty("p50")
  @Nonnull
  public Number getP50() {
    return p50;
  }

  @JsonProperty("p75")
  @Nullable
  public Number getP75() {
    return p75;
  }

  @JsonProperty("p90")
  @Nullable
  public Number getP90() {
    return p90;
  }

  @JsonProperty("p99")
  @Nullable
  public Number getP99() {
    return p99;
  }

  @JsonProperty("p999")
  @Nullable
  public Number getP999() {
    return p999;
  }

  @JsonProperty("p9999")
  @Nullable
  public Number getP9999() {
    return p9999;
  }

  @JsonProperty("max")
  @Nonnull
  public Number getMax() {
    return max;
  }

  @JsonProperty("average")
  @Nonnull
  public Number getAverage() {
    return average;
  }

  @JsonProperty("standard-deviation")
  @Nullable
  public Number getStandardDeviation() {
    return standardDeviation;
  }

  @JsonProperty("total")
  @Nullable
  public Number getTotal() {
    return total;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof DistributionValue)) return false;
    DistributionValue that = (DistributionValue) o;
    return Objects.equals(min, that.min)
        && Objects.equals(p10, that.p10)
        && Objects.equals(p25, that.p25)
        && Objects.equals(p50, that.p50)
        && Objects.equals(p75, that.p75)
        && Objects.equals(p90, that.p90)
        && Objects.equals(p99, that.p99)
        && Objects.equals(p999, that.p999)
        && Objects.equals(p9999, that.p9999)
        && Objects.equals(max, that.max)
        && Objects.equals(average, that.average)
        && Objects.equals(standardDeviation, that.standardDeviation)
        && Objects.equals(total, that.total);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        min, p10, p25, p50, p75, p90, p99, p999, p9999, max, average, standardDeviation, total);
  }

  @Override
  public String toString() {
    return "DistributionValue{"
        + "min="
        + min
        + ", p10="
        + p10
        + ", p25="
        + p25
        + ", p50="
        + p50
        + ", p75="
        + p75
        + ", p90="
        + p90
        + ", p99="
        + p99
        + ", p999="
        + p999
        + ", p9999="
        + p9999
        + ", max="
        + max
        + ", average="
        + average
        + ", standardDeviation="
        + standardDeviation
        + ", total="
        + total
        + '}';
  }
}
