package com.yinxiang.microservice.grpc.autoconfigure;


import com.yinxiang.microservice.grpc.GrpcServerBuilderConfigurer;
import com.yinxiang.microservice.grpc.GrpcServerRunner;
import com.yinxiang.microservice.grpc.GrpcService;
import com.yinxiang.microservice.grpc.context.LocalRunningGrpcPort;
import io.grpc.ServerBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.services.HealthStatusManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@AutoConfigureOrder
@ConditionalOnBean(annotation = GrpcService.class)
@EnableConfigurationProperties(GrpcServerProperties.class)
public class GrpcAutoConfiguration {

  @LocalRunningGrpcPort
  private int port;

  @Autowired
  private GrpcServerProperties grpcServerProperties;



  @Bean
  @ConditionalOnProperty(value = "grpc.enabled", havingValue = "true", matchIfMissing = true)
  public GrpcServerRunner grpcServerRunner(GrpcServerBuilderConfigurer configurer) {
    return new GrpcServerRunner(configurer, ServerBuilder.forPort(port));
  }

  @Bean
  @ConditionalOnExpression("#{environment.getProperty('grpc.inProcessServerName','')!=''}")
  public GrpcServerRunner grpcInprocessServerRunner(GrpcServerBuilderConfigurer configurer){
    return new GrpcServerRunner(configurer, InProcessServerBuilder.forName(grpcServerProperties.getInProcessServerName()));
  }

  @Bean
  public HealthStatusManager healthStatusManager() {
    return new HealthStatusManager();
  }

  @Bean
  @ConditionalOnMissingBean(  GrpcServerBuilderConfigurer.class)
  public GrpcServerBuilderConfigurer serverBuilderConfigurer(){
    return new GrpcServerBuilderConfigurer();
  }
}
