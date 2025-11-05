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
import com.fasterxml.jackson.annotation.JsonValue;

/** Type of metric value. */
public enum MetricType {
  /** A simple point metric with a single numeric value. */
  POINT("point"),

  /** A distribution metric containing percentile-based statistical data. */
  DISTRIBUTION("distribution");

  private final String value;

  MetricType(String value) {
    this.value = value;
  }

  @JsonValue
  public String getValue() {
    return value;
  }

  /**
   * Parse a metric type from its string value.
   *
   * @param value the string value ("point" or "distribution")
   * @return the corresponding MetricType
   * @throws IllegalArgumentException if the value is not recognized
   */
  @JsonCreator
  public static MetricType fromValue(String value) {
    for (MetricType type : values()) {
      if (type.value.equals(value)) {
        return type;
      }
    }
    throw new IllegalArgumentException("Unknown metric type: " + value);
  }
}
