package com.yinxiang.microservice.grpc.inject.config;

/**
 * Config of gRPC server.
 * @author Huiyuan Fu
 * @since 1.0.0
 */
public class GrpcServiceConfig {
  /** Server host. */
  private String host;
  /** Server port */
  private int port;
  /** Unique alias of stub's package. */
  private String name;

  /** @see #host */
  public String getHost() {
    return host;
  }

  /** @see #host */
  public void setHost(String host) {
    this.host = host;
  }

  /** @see #port */
  public int getPort() {
    return port;
  }

  /** @see #port */
  public void setPort(int port) {
    this.port = port;
  }

  /** @see #name */
  public String getName() {
    return name;
  }

  /** @see #name */
  public void setName(String name) {
    this.name = name;
  }
}
