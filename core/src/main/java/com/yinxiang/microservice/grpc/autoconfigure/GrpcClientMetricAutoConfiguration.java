/*
 * Copyright (c) 2016-2019 Michael Zhang <yidongnan@gmail.com>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the
 * Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.yinxiang.microservice.grpc.autoconfigure;

import io.grpc.ClientInterceptor;
import io.micrometer.core.instrument.MeterRegistry;
import com.yinxiang.microservice.grpc.metric.MetricCollectingClientInterceptor;
import org.springframework.boot.actuate.autoconfigure.metrics.CompositeMeterRegistryAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Auto configuration class for Spring-Boot. This allows zero config client metrics for gRPC services.
 *
 * @author  * @author https://github.com/yidongnan/grpc-spring-boot-starter
 */
@Configuration
@AutoConfigureAfter(CompositeMeterRegistryAutoConfiguration.class)
@ConditionalOnBean(MeterRegistry.class)
public class GrpcClientMetricAutoConfiguration {

    /**
     * Creates a {@link ClientInterceptor} that collects metrics about incoming and outgoing requests and responses.
     *
     * @param registry The registry used to create the metrics.
     * @return The newly created MetricCollectingClientInterceptor bean.
     */
    @Bean
    @ConditionalOnMissingBean
    public MetricCollectingClientInterceptor metricCollectingClientInterceptor(final MeterRegistry registry) {
        return new MetricCollectingClientInterceptor(registry);
    }

}
