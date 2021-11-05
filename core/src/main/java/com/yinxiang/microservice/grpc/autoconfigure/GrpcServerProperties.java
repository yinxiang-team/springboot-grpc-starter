package com.yinxiang.microservice.grpc.autoconfigure;

import com.yinxiang.microservice.grpc.inject.config.GrpcServiceConfig;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;
import java.util.Set;

@ConfigurationProperties("grpc")
public class GrpcServerProperties {
  public static final int DEFAULT_GRPC_PORT = 9090;
  private static final int DEFAULT_MAX_IN_BOUND_MESSAGE_SIZE = 4 * 1024 * 1024;

  /**
   * gRPC server port
   *
   */
  private int port = DEFAULT_GRPC_PORT;

  /**
   * Enables the embedded grpc server.
   */
  private boolean enabled = true;

  private int maxInboundMessageSize = DEFAULT_MAX_IN_BOUND_MESSAGE_SIZE;

  /**
   * In process server name.
   * If  the value is not empty, the embedded in-process server will be created and started.
   *
   */
  private String inProcessServerName;

  /**
   * Enables server reflection using <a href="https://github.com/grpc/grpc-java/blob/master/documentation/server-reflection-tutorial.md">ProtoReflectionService</a>.
   * Available only from gRPC 1.3 or higher.
   */
  private boolean enableReflection = false;

  private Map<String, GrpcServiceConfig> services;
  private Map<String, Set<String>> headerNames;

  public int getPort() {
    return port;
  }

  public void setPort(int port) {
    this.port = port;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public String getInProcessServerName() {
    return inProcessServerName;
  }

  public void setInProcessServerName(String inProcessServerName) {
    this.inProcessServerName = inProcessServerName;
  }

  public boolean isEnableReflection() {
    return enableReflection;
  }

  public void setEnableReflection(boolean enableReflection) {
    this.enableReflection = enableReflection;
  }

  public Map<String, GrpcServiceConfig> getServices() {
    return services;
  }

  public void setServices(Map<String, GrpcServiceConfig> services) {
    this.services = services;
  }

  public Map<String, Set<String>> getHeaderNames() {
    return headerNames;
  }

  public void setHeaderNames(Map<String, Set<String>> headerNames) {
    this.headerNames = headerNames;
  }

  public int getMaxInboundMessageSize() {
    return maxInboundMessageSize;
  }

  public void setMaxInboundMessageSize(int maxInboundMessageSize) {
    this.maxInboundMessageSize = maxInboundMessageSize;
  }
}

