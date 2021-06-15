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

package com.yinxiang.microservice.grpc.util;

import com.google.protobuf.Descriptors;
import com.google.protobuf.GeneratedMessageV3.ExtendableMessage;
import io.grpc.MethodDescriptor;

import java.util.Map;
import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Utility class that contains methods to extract some information from grpc classes.
 *
 * @author https://github.com/yidongnan/grpc-spring-boot-starter
 */
public final class GrpcUtils {

    private static final BiPredicate<Map.Entry<Descriptors.FieldDescriptor, Object>, String> NAME_PREDICATE =
            (entry, fullName) -> fullName.equals(entry.getKey().getFullName());

    /**
     * The cloud discovery metadata key used to identify the grpc port.
     */
    public static final String CLOUD_DISCOVERY_METADATA_PORT = "gRPC.port";

    /**
     * Extracts the service name from the given method.
     *
     * @param method The method to get the service name from.
     * @return The extracted service name.
     * @see MethodDescriptor#extractFullServiceName(String)
     * @see #extractMethodName(MethodDescriptor)
     */
    public static String extractServiceName(final MethodDescriptor<?, ?> method) {
        return MethodDescriptor.extractFullServiceName(method.getFullMethodName());
    }

    /**
     * Extracts the method name from the given method.
     *
     * @param method The method to get the method name from.
     * @return The extracted method name.
     * @see #extractServiceName(MethodDescriptor)
     */
    public static String extractMethodName(final MethodDescriptor<?, ?> method) {
        // This method is the equivalent of MethodDescriptor.extractFullServiceName
        final String fullMethodName = method.getFullMethodName();
        final int index = fullMethodName.lastIndexOf('/');
        if (index == -1) {
            return fullMethodName;
        }
        return fullMethodName.substring(index + 1);
    }

    /**
     * Try to find the first element of options and execute with this value.
     * @param message           {@link M}
     * @param optionFullName    stream filter by the name of option
     * @param consumer          consumer for the found value
     */
    public static <T, M extends ExtendableMessage<M>> void executeFirstOptionValue(
            M message,
            String optionFullName,
            Consumer<T> consumer
    ) {
        GrpcUtils.<T, M>findFirstOptionValue(message, optionFullName).ifPresent(consumer);
    }

    /**
     * Try to find the first element of options, else null.
     * @param message           {@link M}
     * @param optionFullName    stream filter by the name of option
     * @param mapper            converter of {@link T} to {@link R}
     * @return  the first element of options
     */
    public static <T, M extends ExtendableMessage<M>, R> R findFirstOptionValue(
            M message,
            String optionFullName,
            Function<T, R> mapper
    ) {
        return GrpcUtils.<T, M>findFirstOptionValue(message, optionFullName).map(mapper).orElse(null);
    }

    /**
     * Try to find the first element of options with {@link Optional}.
     * @param message           {@link M}
     * @param optionFullName    stream filter by the name of option
     * @return  the first element of options
     */
    @SuppressWarnings("unchecked")
    public static <T, M extends ExtendableMessage<M>> Optional<T> findFirstOptionValue(
            M message,
            String optionFullName
    ) {
        Optional<Map.Entry<Descriptors.FieldDescriptor, Object>> optional =
                findFirstOption(getStream(message), entry -> NAME_PREDICATE.test(entry, optionFullName));
        return optional.map(entry -> (T) entry.getValue());
    }

    /**
     * Check has any match option.
     * @param message   {@link M}
     * @param predicate stream filter
     * @return  has any match option
     */
    public static <M extends ExtendableMessage<M>> boolean anyMatchOption(
            M message,
            Predicate<Map.Entry<Descriptors.FieldDescriptor, Object>> predicate
    ) {
        return getStream(message).anyMatch(predicate);
    }

    /**
     * Try to find the first element of options with {@link Optional}.
     * @param stream    {@link Stream}
     * @param predicate stream filter
     * @return  the first element of options
     */
    private static Optional<Map.Entry<Descriptors.FieldDescriptor, Object>> findFirstOption(
            Stream<Map.Entry<Descriptors.FieldDescriptor, Object>> stream,
            Predicate<Map.Entry<Descriptors.FieldDescriptor, Object>> predicate
    ) {
        return stream.filter(predicate).findFirst();
    }

    /**
     * Get a stream from a {@link M}.
     * @param message   {@link M}
     * @param <M>   {@link ExtendableMessage}
     * @return  a stream
     */
    public static <M extends ExtendableMessage<M>>
    Stream<Map.Entry<Descriptors.FieldDescriptor, Object>> getStream(M message) {
        return message.getAllFields().entrySet().stream();
    }

    private GrpcUtils() {}

}
