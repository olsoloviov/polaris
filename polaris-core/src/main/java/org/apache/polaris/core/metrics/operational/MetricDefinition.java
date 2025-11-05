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
 * Definition of a supported operational metric.
 *
 * <p>Defines the name, type, and description of a metric that can be computed, submitted, and
 * retrieved from Polaris.
 */
public class MetricDefinition {
  private final String name;
  private final MetricType type;
  private final String description;

  @JsonCreator
  public MetricDefinition(
      @JsonProperty("name") @Nonnull String name,
      @JsonProperty("type") @Nonnull MetricType type,
      @JsonProperty("description") @Nonnull String description) {
    this.name = Objects.requireNonNull(name, "name cannot be null");
    this.type = Objects.requireNonNull(type, "type cannot be null");
    this.description = Objects.requireNonNull(description, "description cannot be null");
  }

  @JsonProperty("name")
  @Nonnull
  public String getName() {
    return name;
  }

  @JsonProperty("type")
  @Nonnull
  public MetricType getType() {
    return type;
  }

  @JsonProperty("description")
  @Nonnull
  public String getDescription() {
    return description;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof MetricDefinition)) return false;
    MetricDefinition that = (MetricDefinition) o;
    return Objects.equals(name, that.name)
        && type == that.type
        && Objects.equals(description, that.description);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, type, description);
  }

  @Override
  public String toString() {
    return "MetricDefinition{"
        + "name='"
        + name
        + '\''
        + ", type="
        + type
        + ", description='"
        + description
        + '\''
        + '}';
  }
}
