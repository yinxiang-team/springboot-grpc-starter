package com.yinxiang.microservice.grpc.inject;

import org.slf4j.Logger;

/**
 * A base supplier for log, can extend on custom interface which annotation by
 * {@link com.yinxiang.microservice.grpc.inject.annotations.GrpcClient}.
 * @author Huiyuan Fu
 * @since 1.0.0
 */
public interface LogSupplier {
  /**
   * Getter for log.
   * @return  {@link Logger}
   */
  Logger _log();
}
