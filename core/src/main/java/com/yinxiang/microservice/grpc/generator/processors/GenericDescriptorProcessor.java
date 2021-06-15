package com.yinxiang.microservice.grpc.generator.processors;

import com.google.protobuf.Descriptors.GenericDescriptor;
import com.yinxiang.microservice.grpc.generator.infos.BaseInfo;

import java.util.function.Function;
import java.util.function.Predicate;

/**
 * A generic descriptor processor interface.
 * Use to cover a object instance of type T which extends GenericDescriptor.
 * Use check an attribute if need hide.
 * @param <T> the type which extends GenericDescriptor
 * @param <R> the type which extends BaseInfo which correspond to the type which extends GenericDescriptor
 * @author Huiyuan Fu
 * @see GenericDescriptor
 * @see BaseInfo
 */
public interface GenericDescriptorProcessor<T extends GenericDescriptor, R extends BaseInfo>
        extends Function<T, R>, Predicate<T> {}
