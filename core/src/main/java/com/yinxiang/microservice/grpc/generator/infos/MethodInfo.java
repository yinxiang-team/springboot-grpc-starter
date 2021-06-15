package com.yinxiang.microservice.grpc.generator.infos;

import com.google.common.collect.Lists;

import java.util.List;

/**
 * The MethodDescriptor attributes.
 * @see com.google.protobuf.Descriptors.MethodDescriptor
 * @see com.google.api.HttpRule
 * @author Huiyuan Fu
 */
public class MethodInfo extends BaseInfo {
  /** MethodDescriptor's google.grpc.http(post/get/put/delete/patch) option. */
  private String path;
  /** The HttpRule of MethodDescriptor google.grpc.http(post/get/put/delete/patch) option. */
  private String method;
  /** MethodDescriptor's input. */
  private MessageInfo input;
  /** MethodDescriptor's output. */
  private MessageInfo output;
  /** MethodDescriptor's yinxiang.grpc.http.headers option. */
  private List<String> headers = Lists.newLinkedList();
  /** parent service. */
  private ServiceInfo serviceInfo;

  /** @see #path */
  public String getPath() {
    return path;
  }

  /** @see #path */
  public void setPath(String path) {
    this.path = path;
  }

  /** @see #method */
  public String getMethod() {
    return method;
  }

  /** @see #method */
  public void setMethod(String method) {
    this.method = method;
  }

  /** @see #input */
  public MessageInfo getInput() {
    return input;
  }

  /** @see #input */
  public void setInput(MessageInfo input) {
    this.input = input;
  }

  /** @see #output */
  public MessageInfo getOutput() {
    return output;
  }

  /** @see #output */
  public void setOutput(MessageInfo output) {
    this.output = output;
  }

  /** @see #headers */
  public List<String> getHeaders() {
    return headers;
  }

  /** @see #headers */
  public void setHeaders(List<String> headers) {
    this.headers = headers;
  }

  /** @see #serviceInfo */
  public ServiceInfo getServiceInfo() {
    return serviceInfo;
  }

  /** @see #serviceInfo */
  public void setServiceInfo(ServiceInfo serviceInfo) {
    this.serviceInfo = serviceInfo;
  }
}
