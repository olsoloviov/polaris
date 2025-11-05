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

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import org.apache.polaris.service.config.RealmOverridable;

/**
 * Configuration for operational metrics persistence.
 *
 * <p>This configuration determines which {@link
 * org.apache.polaris.core.operational.OperationalMetricsPersistenceFactory} implementation to use
 * for storing and retrieving operational metrics.
 *
 * <p>Available types:
 *
 * <ul>
 *   <li>{@code noop} - Default. Discards all metrics (metrics disabled)
 *   <li>{@code in-memory} - Stores metrics in memory (for testing/development)
 *   <li>Custom implementations can be added by creating a factory annotated with
 *       {@code @Identifier("custom-type")}
 * </ul>
 */
@ConfigMapping(prefix = "polaris.operational-metrics")
public interface OperationalMetricsPersistenceConfiguration extends RealmOverridable {

  /**
   * The type of operational metrics persistence to use.
   *
   * @return the persistence type identifier
   */
  @WithDefault("noop")
  String type();
}
