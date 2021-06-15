package com.yinxiang.microservice.grpc.generator.infos;

import java.util.List;

/**
 * The ServiceDescriptor attributes.
 * @see com.google.protobuf.Descriptors.ServiceDescriptor
 * @author Huiyuan Fu
 */
public class ServiceInfo extends BaseInfo {
  /** Output classes's package. */
  private String pkg;
  /** Service's package. */
  private String javaPackage;
  /** ServiceDescriptor's yinxiang.grpc.http.url option. */
  private String url = "";
  /** The output file. */
  private String fileName;
  /** List of all methods. */
  private List<MethodInfo> methods;

  /** @see #pkg */
  public String getPkg() {
    return pkg;
  }

  /** @see #pkg */
  public void setPkg(String pkg) {
    this.pkg = pkg;
  }

  /** @see #javaPackage */
  public String getJavaPackage() {
    return javaPackage;
  }

  /** @see #javaPackage */
  public void setJavaPackage(String javaPackage) {
    this.javaPackage = javaPackage;
  }

  /** @see #url */
  public String getUrl() {
    return url;
  }

  /** @see #url */
  public void setUrl(String url) {
    this.url = url;
  }

  /** @see #methods */
  public List<MethodInfo> getMethods() {
    return methods;
  }

  /** @see #methods */
  public void setMethods(List<MethodInfo> methods) {
    this.methods = methods;
  }

  /** @see #fileName */
  public String getFileName() {
    return fileName;
  }

  /** @see #fileName */
  public void setFileName(String fileName) {
    this.fileName = fileName;
  }
}
