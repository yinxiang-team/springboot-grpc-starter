package com.yinxiang.microservice.grpc.inject.config;

import com.yinxiang.microservice.grpc.autoconfigure.GrpcServerProperties;
import com.yinxiang.microservice.grpc.inject.spring.AutoConfiguredGrpcClientScannerRegistrar;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Entrance of gRPC inject，import {@link AutoConfiguredGrpcClientScannerRegistrar}
 * @author Huiyuan Fu
 * @since 1.0.0
 */
@Configuration
@EnableConfigurationProperties({GrpcServerProperties.class})
@Import(AutoConfiguredGrpcClientScannerRegistrar.class)
public class GrpcInjectAutoConfiguration {
}
