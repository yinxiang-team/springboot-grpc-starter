package com.yinxiang.microservice.grpc.context;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.util.SocketUtils;

import java.util.Properties;

import static com.yinxiang.microservice.grpc.autoconfigure.GrpcServerProperties.DEFAULT_GRPC_PORT;

public class GrpcServerEnvironment implements EnvironmentPostProcessor {
  @Override
  public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
    MutablePropertySources sources = environment.getPropertySources();
    Properties properties = new Properties();
    Integer configuredPort = environment.getProperty("grpc.port", Integer.class);
    properties.put("grpc.port", 0);
    if (null == configuredPort) {
      properties.put(LocalRunningGrpcPort.propertyName, DEFAULT_GRPC_PORT);
    } else if (0 == configuredPort) {
      properties.put(LocalRunningGrpcPort.propertyName, SocketUtils.findAvailableTcpPort());
    } else {
      properties.put("grpc.port", configuredPort);
      properties.put(LocalRunningGrpcPort.propertyName, configuredPort);
    }
    sources.addLast(new PropertiesPropertySource("grpc", properties));
  }
}

