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

package com.yinxiang.microservice.grpc.metric;

import com.yinxiang.microservice.grpc.GrpcGlobalInterceptor;
import com.yinxiang.microservice.grpc.util.InterceptorOrder;
import io.grpc.*;
import io.grpc.Status.Code;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;

import java.util.function.Function;
import java.util.function.UnaryOperator;

import static com.yinxiang.microservice.grpc.metric.MetricConstants.*;
import static com.yinxiang.microservice.grpc.metric.MetricUtils.prepareCounterFor;
import static com.yinxiang.microservice.grpc.metric.MetricUtils.prepareTimerFor;

/**
 * A gRPC client interceptor that will collect metrics for micrometer.
 *
 * @author https://github.com/yidongnan/grpc-spring-boot-starter
 */
@GrpcGlobalInterceptor
@Order(InterceptorOrder.ORDER_TRACING_METRICS)
public class MetricCollectingClientInterceptor extends AbstractMetricCollectingInterceptor
        implements ClientInterceptor {

    /**
     * Creates a new gRPC client interceptor that will collect metrics into the given {@link MeterRegistry}.
     *
     * @param registry The registry to use.
     */
    @Autowired
    public MetricCollectingClientInterceptor(final MeterRegistry registry) {
        super(registry);
    }

    /**
     * Creates a new gRPC client interceptor that will collect metrics into the given {@link MeterRegistry} and uses the
     * given customizer to configure the {@link Counter}s and {@link Timer}s.
     *
     * @param registry The registry to use.
     * @param counterCustomizer The unary function that can be used to customize the created counters.
     * @param timerCustomizer The unary function that can be used to customize the created timers.
     * @param eagerInitializedCodes The status codes that should be eager initialized.
     */
    public MetricCollectingClientInterceptor(final MeterRegistry registry,
                                             final UnaryOperator<Counter.Builder> counterCustomizer,
                                             final UnaryOperator<Timer.Builder> timerCustomizer, final Code... eagerInitializedCodes) {
        super(registry, counterCustomizer, timerCustomizer, eagerInitializedCodes);
    }

    @Override
    protected Counter newRequestCounterFor(final MethodDescriptor<?, ?> method) {
        return this.counterCustomizer.apply(
                prepareCounterFor(method,
                        METRIC_NAME_CLIENT_REQUESTS_SENT,
                        "The total number of requests sent"))
                .register(this.registry);
    }

    @Override
    protected Counter newResponseCounterFor(final MethodDescriptor<?, ?> method) {
        return this.counterCustomizer.apply(
                prepareCounterFor(method,
                        METRIC_NAME_CLIENT_RESPONSES_RECEIVED,
                        "The total number of responses received"))
                .register(this.registry);
    }

    @Override
    protected Function<Code, Timer> newTimerFunction(final MethodDescriptor<?, ?> method) {
        return asTimerFunction(() -> this.timerCustomizer.apply(
                prepareTimerFor(method,
                        METRIC_NAME_CLIENT_PROCESSING_DURATION,
                        "The total time taken for the client to complete the call, including network delay")));
    }

    @Override
    public <Q, A> ClientCall<Q, A> interceptCall(final MethodDescriptor<Q, A> methodDescriptor,
                                                 final CallOptions callOptions, final Channel channel) {
        final MetricSet metrics = metricsFor(methodDescriptor);
        return new MetricCollectingClientCall<>(
                channel.newCall(methodDescriptor, callOptions),
                this.registry,
                metrics.getRequestCounter(),
                metrics.getResponseCounter(),
                metrics.getTimerFunction());
    }

}
