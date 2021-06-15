package com.yinxiang.microservice.grpc;

import io.grpc.ServerInterceptor;
import org.springframework.stereotype.Service;

import java.lang.annotation.*;

/**
 * Marks class as a gRPC service bean
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Service
public @interface GrpcService {
  Class<? extends ServerInterceptor>[] interceptors() default {};
  boolean applyGlobalInterceptors() default true;
}

