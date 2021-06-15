package com.yinxiang.microservice.grpc.inject.factories;

import org.springframework.beans.BeansException;

public class InjectGrpcException extends BeansException {
  public InjectGrpcException(String msg) {
    super(msg);
  }

  public InjectGrpcException(String msg, Throwable cause) {
    super(msg, cause);
  }
}
