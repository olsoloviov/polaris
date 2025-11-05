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

import jakarta.annotation.Nonnull;
import org.apache.polaris.core.context.RealmContext;

/**
 * Factory interface for creating {@link OperationalMetricsPersistence} instances.
 *
 * <p>This factory allows service administrators to plug in their preferred database for metrics
 * storage and retrieval. Implementations should be annotated with {@code @ApplicationScoped} and
 * {@code @Identifier} to enable CDI-based selection.
 *
 * <p>Example implementation:
 *
 * <pre>{@code
 * @ApplicationScoped
 * @Identifier("my-metrics-db")
 * public class MyMetricsPersistenceFactory implements OperationalMetricsPersistenceFactory {
 *   @Override
 *   public OperationalMetricsPersistence getOrCreatePersistence(RealmContext realmContext) {
 *     return new MyMetricsPersistence(realmContext);
 *   }
 * }
 * }</pre>
 */
public interface OperationalMetricsPersistenceFactory {

  /**
   * Get or create an {@link OperationalMetricsPersistence} instance for the given realm.
   *
   * <p>Implementations may cache instances per realm or create new instances on each call,
   * depending on their requirements.
   *
   * @param realmContext the realm context for which to get or create the persistence instance
   * @return an OperationalMetricsPersistence instance for the realm
   */
  @Nonnull
  OperationalMetricsPersistence getOrCreatePersistence(@Nonnull RealmContext realmContext);
}
