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
import org.apache.polaris.core.context.RealmContext;
import org.apache.polaris.core.metrics.operational.NoOpOperationalMetricsPersistence;
import org.apache.polaris.core.metrics.operational.OperationalMetricsPersistence;
import org.apache.polaris.core.metrics.operational.OperationalMetricsPersistenceFactory;

/**
 * No-op factory for operational metrics persistence.
 *
 * <p>This is the default factory used when no operational metrics database is explicitly configured
 * by an administrator. It returns a {@link NoOpOperationalMetricsPersistence} instance that
 * discards all metrics, effectively disabling metrics support.
 */
@ApplicationScoped
@Identifier("noop")
@Named("noopOperationalMetricsPersistenceFactory")
public class NoOpOperationalMetricsPersistenceFactory
    implements OperationalMetricsPersistenceFactory {

  private final OperationalMetricsPersistence noOpPersistence =
      new NoOpOperationalMetricsPersistence();

  @Override
  public OperationalMetricsPersistence getOrCreatePersistence(RealmContext realmContext) {
    return noOpPersistence;
  }
}
