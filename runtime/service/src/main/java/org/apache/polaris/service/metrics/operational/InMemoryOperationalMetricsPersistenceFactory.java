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

import io.smallrye.common.annotation.Identifier;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;
import java.util.HashMap;
import java.util.Map;
import org.apache.polaris.core.context.RealmContext;
import org.apache.polaris.core.metrics.operational.InMemoryOperationalMetricsPersistence;
import org.apache.polaris.core.metrics.operational.OperationalMetricsPersistence;
import org.apache.polaris.core.metrics.operational.OperationalMetricsPersistenceFactory;

/**
 * In-memory factory for operational metrics persistence.
 *
 * <p>This factory creates {@link InMemoryOperationalMetricsPersistence} instances that store
 * metrics in memory. It maintains one persistence instance per realm.
 *
 * <p>This implementation is suitable for testing and development purposes. It is not recommended
 * for production use as all metrics will be lost when the service restarts.
 */
@ApplicationScoped
@Identifier("in-memory")
@Named("inMemoryOperationalMetricsPersistenceFactory")
public class InMemoryOperationalMetricsPersistenceFactory
    implements OperationalMetricsPersistenceFactory {

  private final Map<String, InMemoryOperationalMetricsPersistence> persistenceMap = new HashMap<>();

  @Override
  public synchronized OperationalMetricsPersistence getOrCreatePersistence(
      RealmContext realmContext) {
    return persistenceMap.computeIfAbsent(
        realmContext.getRealmIdentifier(), k -> new InMemoryOperationalMetricsPersistence());
  }

  /** Clears all stored metrics across all realms. Useful for testing. */
  public synchronized void clearAll() {
    persistenceMap.values().forEach(InMemoryOperationalMetricsPersistence::clear);
    persistenceMap.clear();
  }
}
