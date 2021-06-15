package com.yinxiang.microservice.grpc.generator.processors;

import com.google.common.collect.Lists;
import com.google.protobuf.DescriptorProtos.ServiceOptions;
import com.google.protobuf.Descriptors.*;
import com.sun.javadoc.Doc;
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
public final class ServiceProcessor extends BaseDescriptorProcessor<ServiceDescriptor, ServiceOptions, ServiceInfo> {
  /** The base url of service. */
  private static final String PROTO_SERVICE_URL = Http.url.getDescriptor().getFullName();

  /** Marshaller from MethodDescriptor to MethodInfo. */
  private final Function<MethodDescriptor, MethodInfo> methodMarshaller;

  /**
   * Create a full ServiceProcessor.
   * @return  ServiceProcessor
   */
  public static ServiceProcessor createFullServiceProcessor(String sourcePath) {
    EnumProcessor enumProcessor = new EnumProcessor(new EnumValueProcessor(), sourcePath + "java/");
    FieldProcessor fieldProcessor = new FieldProcessor(enumProcessor, sourcePath);
    MessageProcessor messageProcessor = new MessageProcessor(enumProcessor, fieldProcessor, sourcePath + "java/");
    fieldProcessor.setMessageMarshaller(messageProcessor);
    return new ServiceProcessor(new MethodProcessor(messageProcessor), sourcePath + "grpc-java/");
  }

  public ServiceProcessor(Function<MethodDescriptor, MethodInfo> methodMarshaller, String sourcePath) {
    super(sourcePath);
    this.methodMarshaller = methodMarshaller;
  }

  @Override
  protected void addProperties(ServiceDescriptor descriptor, ServiceInfo serviceInfo) {
    GrpcUtils.executeFirstOptionValue(getOptions(descriptor), PROTO_SERVICE_URL, serviceInfo::setUrl);
    String path = replaceDot(descriptor.getFile().getOptions().getJavaPackage());
    String className = descriptor.getName() + "Grpc";
    getClassDoc(path, className, className + "." + descriptor.getName() + "ImplBase")
            .ifPresent(classDoc -> {
              Doc[] docs = classDoc.methods();
              // get all gRPC
              List<MethodInfo> methods = Lists.newLinkedList();
              descriptor.getMethods().forEach(method -> {
                MethodInfo methodInfo = methodMarshaller.apply(method);
                if (methodInfo != null) {
                  methodInfo.setCommentByDoc(docs);
                  methods.add(methodInfo);
                  methodInfo.setServiceInfo(serviceInfo);
                }
              });
              serviceInfo.setMethods(methods);
              serviceInfo.setJavaPackage(descriptor.getFile().getOptions().getJavaPackage());
            });
  }
}
