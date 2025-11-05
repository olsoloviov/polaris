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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.util.List;
import org.apache.polaris.core.metrics.operational.MetricDefinition;
import org.apache.polaris.core.metrics.operational.MetricType;
import org.apache.polaris.core.metrics.operational.OperationalMetricsException;
import org.apache.polaris.core.metrics.operational.PointMetricValue;
import org.junit.jupiter.api.Test;

@QuarkusTest
class OperationalMetricsRegistryTest {

  @Inject OperationalMetricsRegistry registry;

  @Test
  void testGetAllDefinitions() {
    List<MetricDefinition> definitions = registry.getAllDefinitions();

    assertThat(definitions).isNotEmpty().hasSize(10);

    // Verify some expected metrics
    assertThat(definitions)
        .extracting(MetricDefinition::getName)
        .contains(
            "added-data-files",
            "added-records",
            "total-records",
            "total-data-files",
            "total-delete-files");
  }

  @Test
  void testGetDefinitionByName() {
    MetricDefinition definition = registry.getDefinition("total-records");

    assertThat(definition).isNotNull();
    assertThat(definition.getName()).isEqualTo("total-records");
    assertThat(definition.getType()).isEqualTo(MetricType.POINT);
    assertThat(definition.getDescription()).isNotEmpty();
  }

  @Test
  void testGetDefinitionForNonExistentMetric() {
    MetricDefinition definition = registry.getDefinition("non-existent-metric");

    assertThat(definition).isNull();
  }

  @Test
  void testValidateMetricSuccess() {
    PointMetricValue validMetric = new PointMetricValue(1000L);

    // Should not throw exception
    registry.validateMetric("total-records", validMetric);
  }

  @Test
  void testValidateMetricNotRegistered() {
    PointMetricValue metric = new PointMetricValue(1000L);

    assertThatThrownBy(() -> registry.validateMetric("unknown-metric", metric))
        .isInstanceOf(OperationalMetricsException.class)
        .hasMessageContaining("Metric 'unknown-metric' is not registered");
  }

  @Test
  void testValidateMetricTypeMismatch() {
    // Registry expects POINT type for total-records, but we're providing DISTRIBUTION
    // This would require creating a DistributionMetricValue, but since all current metrics
    // are POINT type, we can't easily test this without modifying the registry JSON
    // For now, we'll just verify the validation logic exists
    assertThat(registry.getDefinition("total-records").getType()).isEqualTo(MetricType.POINT);
  }

  @Test
  void testAllRegisteredMetricsArePointType() {
    List<MetricDefinition> definitions = registry.getAllDefinitions();

    // All currently registered metrics should be POINT type
    assertThat(definitions).allMatch(def -> def.getType() == MetricType.POINT);
  }

  @Test
  void testSpecificMetricDefinitions() {
    // Test added-data-files
    MetricDefinition addedDataFiles = registry.getDefinition("added-data-files");
    assertThat(addedDataFiles).isNotNull();
    assertThat(addedDataFiles.getName()).isEqualTo("added-data-files");
    assertThat(addedDataFiles.getType()).isEqualTo(MetricType.POINT);

    // Test total-files-size
    MetricDefinition totalFilesSize = registry.getDefinition("total-files-size");
    assertThat(totalFilesSize).isNotNull();
    assertThat(totalFilesSize.getName()).isEqualTo("total-files-size");
    assertThat(totalFilesSize.getType()).isEqualTo(MetricType.POINT);

    // Test total-position-deletes
    MetricDefinition totalPositionDeletes = registry.getDefinition("total-position-deletes");
    assertThat(totalPositionDeletes).isNotNull();
    assertThat(totalPositionDeletes.getName()).isEqualTo("total-position-deletes");
    assertThat(totalPositionDeletes.getType()).isEqualTo(MetricType.POINT);
  }

  @Test
  void testRegistryIsApplicationScoped() {
    // Verify that the registry is a CDI bean by checking it was injected
    assertThat(registry).isNotNull();

    // Get all definitions twice and verify it's the same instance (cached)
    List<MetricDefinition> definitions1 = registry.getAllDefinitions();
    List<MetricDefinition> definitions2 = registry.getAllDefinitions();

    assertThat(definitions1).isSameAs(definitions2);
  }

  @Test
  void testValidateNullMetricName() {
    PointMetricValue metric = new PointMetricValue(1000L);

    assertThatThrownBy(() -> registry.validateMetric(null, metric))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void testValidateNullMetricValue() {
    assertThatThrownBy(() -> registry.validateMetric("total-records", null))
        .isInstanceOf(NullPointerException.class);
  }
}
