package com.yinxiang.microservice.grpc.controller;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.protobuf.Message;
import com.googlecode.protobuf.format.JsonFormat;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * A StreamObserver of rest controller.
 * @param <V> message type
 * @author Huiyuan Fu
 * @since 1.0.0
 */
class RestControllerStreamObserver<V extends Message> implements StreamObserver<V> {
  private static final Logger log = LoggerFactory.getLogger(RestControllerStreamObserver.class);
  /** List of messages. */
  private final List<V> list = Lists.newLinkedList();
  /** protobuf - json formatter. */
  private final JsonFormat jsonFormat;

  RestControllerStreamObserver(JsonFormat jsonFormat) {
    this.jsonFormat = jsonFormat;
  }

  @Override
  public void onNext(V v) {
    list.add(v);
  }

  @Override
  public void onError(Throwable throwable) {
    log.error("RestControllerStreamObserver onError: " + throwable.getMessage(), throwable);
  }

  @Override
  public void onCompleted() {}

  /**
   * Generate the json string result.
   * To a json when only one response.
   * To a json array when more then one responses.
   * @return the json string
   */
  @Override
  public String toString() {
    // detecting the presence of a message
    Preconditions.checkArgument(list.size() > 0);
    // return the json of only one message
    if (list.size() == 1) {
      return jsonFormat.printToString(list.get(0));
    }
    // return the json array of messages when more then one message
    else {
      StringBuilder builder = new StringBuilder("[");
      list.forEach(message -> builder.append(jsonFormat.printToString(message)).append(","));
      builder.setLength(builder.length() - 1);
      return builder.append("]").toString();
    }
  }
}
