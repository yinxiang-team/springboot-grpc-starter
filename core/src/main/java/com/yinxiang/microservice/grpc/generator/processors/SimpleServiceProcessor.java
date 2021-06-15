package com.yinxiang.microservice.grpc.generator.processors;

import com.google.common.collect.Lists;
import com.google.protobuf.DescriptorProtos.ServiceOptions;
import com.google.protobuf.Descriptors.MethodDescriptor;
import com.google.protobuf.Descriptors.ServiceDescriptor;
import com.yinxiang.grpc.http.Http;
import com.yinxiang.microservice.grpc.generator.infos.MethodInfo;
import com.yinxiang.microservice.grpc.generator.infos.ServiceInfo;
import com.yinxiang.microservice.grpc.util.GrpcUtils;

import java.util.List;
import java.util.function.Function;

/**
 * Processing a service.
 * @author Huiyuan Fu
 */
public final class SimpleServiceProcessor extends BaseDescriptorProcessor<ServiceDescriptor, ServiceOptions, ServiceInfo> {
  /** The base url of service. */
  private static final String PROTO_SERVICE_URL = Http.url.getDescriptor().getFullName();

  /** Marshaller from MethodDescriptor to MethodInfo. */
  private final Function<MethodDescriptor, MethodInfo> methodMarshaller;

  public SimpleServiceProcessor(Function<MethodDescriptor, MethodInfo> methodMarshaller) {
    super();
    this.methodMarshaller = methodMarshaller;
  }

  @Override
  protected void addProperties(ServiceDescriptor descriptor, ServiceInfo serviceInfo) {
    GrpcUtils.executeFirstOptionValue(getOptions(descriptor), PROTO_SERVICE_URL, serviceInfo::setUrl);
    List<MethodInfo> methods = Lists.newLinkedList();
    descriptor.getMethods().forEach(method -> {
      MethodInfo methodInfo = methodMarshaller.apply(method);
      if (methodInfo != null) {
        methods.add(methodInfo);
        methodInfo.setServiceInfo(serviceInfo);
      }
    });
    serviceInfo.setMethods(methods);
    serviceInfo.setJavaPackage(descriptor.getFile().getOptions().getJavaPackage());
  }
}
