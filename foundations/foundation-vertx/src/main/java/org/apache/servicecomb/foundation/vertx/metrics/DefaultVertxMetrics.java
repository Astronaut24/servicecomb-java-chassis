/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.servicecomb.foundation.vertx.metrics;

import java.util.Map;

import org.apache.servicecomb.foundation.common.concurrent.ConcurrentHashMapEx;
import org.apache.servicecomb.foundation.vertx.metrics.metric.DefaultClientEndpointMetricManager;
import org.apache.servicecomb.foundation.vertx.metrics.metric.DefaultEndpointMetric;

import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.metrics.impl.DummyVertxMetrics;
import io.vertx.core.net.NetClientOptions;
import io.vertx.core.net.NetServerOptions;
import io.vertx.core.net.SocketAddress;
import io.vertx.core.spi.metrics.HttpClientMetrics;
import io.vertx.core.spi.metrics.HttpServerMetrics;
import io.vertx.core.spi.metrics.TCPMetrics;

public class DefaultVertxMetrics extends DummyVertxMetrics {
  private final Vertx vertx;

  private VertxOptions vertxOptions;

  // to support listen multiple addresses, must use a map to manage the metric
  private Map<SocketAddress, DefaultEndpointMetric> serverEndpointMetricMap = new ConcurrentHashMapEx<>();

  private volatile DefaultClientEndpointMetricManager clientEndpointMetricManager;

  public DefaultVertxMetrics(Vertx vertx, VertxOptions vertxOptions) {
    this.vertx = vertx;
    this.vertxOptions = vertxOptions;
    // can not create DefaultClientEndpointMetricManager in this time
    // because vertx is not inited
  }

  public Vertx getVertx() {
    return vertx;
  }

  public DefaultClientEndpointMetricManager getClientEndpointMetricManager() {
    return clientEndpointMetricManager;
  }

  public Map<SocketAddress, DefaultEndpointMetric> getServerEndpointMetricMap() {
    return serverEndpointMetricMap;
  }

  private void initClientEndpointMetricManager() {
    if (clientEndpointMetricManager == null) {
      synchronized (vertx) {
        if (clientEndpointMetricManager == null) {
          clientEndpointMetricManager = new DefaultClientEndpointMetricManager(vertx,
              (MetricsOptionsEx) vertxOptions.getMetricsOptions());
        }
      }
    }
  }

  @Override
  public HttpServerMetrics<?, ?, ?> createMetrics(HttpServer server, SocketAddress localAddress,
      HttpServerOptions options) {
    DefaultEndpointMetric endpointMetric = serverEndpointMetricMap
        .computeIfAbsent(localAddress, DefaultEndpointMetric::new);
    return new DefaultHttpServerMetrics(endpointMetric);
  }

  @Override
  public HttpClientMetrics<?, ?, ?, ?, ?> createMetrics(HttpClient client, HttpClientOptions options) {
    initClientEndpointMetricManager();
    return new DefaultHttpClientMetrics(clientEndpointMetricManager);
  }

  @Override
  public TCPMetrics<?> createMetrics(SocketAddress localAddress, NetServerOptions options) {
    DefaultEndpointMetric endpointMetric = serverEndpointMetricMap
        .computeIfAbsent(localAddress, DefaultEndpointMetric::new);
    return new DefaultTcpServerMetrics(endpointMetric);
  }

  @Override
  public TCPMetrics<?> createMetrics(NetClientOptions options) {
    initClientEndpointMetricManager();
    return new DefaultTcpClientMetrics(clientEndpointMetricManager);
  }

  @Override
  public boolean isMetricsEnabled() {
    return true;
  }

  @Deprecated
  @Override
  public boolean isEnabled() {
    return true;
  }
}
