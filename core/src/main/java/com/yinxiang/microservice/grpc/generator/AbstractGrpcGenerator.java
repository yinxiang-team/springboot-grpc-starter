package com.yinxiang.microservice.grpc.generator;

import com.google.protobuf.Descriptors.*;
import com.yinxiang.microservice.grpc.controller.ClassPathLoader;
import com.yinxiang.microservice.grpc.generator.infos.ServiceInfo;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.function.Function;

/**
 * A abstract gRPC content generator.
 * @author Huiyuan Fu
 */
public abstract class AbstractGrpcGenerator implements GrpcInfoGenerator {
  /** Marshaller from ServiceDescriptor to ServiceInfo. */
  private final Function<ServiceDescriptor, ServiceInfo> marshaller;
  /** Marshaller from ServiceInfo to String. */
  private final Function<ServiceInfo, String> contentBuilder;

  protected AbstractGrpcGenerator(
          Function<ServiceDescriptor, ServiceInfo> marshaller,
          Function<ServiceInfo, String> contentBuilder
  ) {
    this.marshaller = marshaller;
    this.contentBuilder = contentBuilder;
  }

  @Override
  public void generator(String classPath, String output, String pkg) throws Exception {
    // create the ClassPathLoader
    ClassPathLoader classLoader = new ClassPathLoader(getClass().getClassLoader());
    // create output dir
    File file = new File(output);
    if (!file.exists()) {
      System.out.println(file.createNewFile());
    }
    // scan classes and generate
    for (Class clz : classLoader.loadClasses(classPath)) {
      try {
        // get the method which named 'getDescriptor'
        @SuppressWarnings("unchecked")
        Method descriptor = clz.getMethod("getDescriptor");
        // process the method which return FileDescriptor
        if (FileDescriptor.class.equals(descriptor.getReturnType())) {
          generate(descriptor, output, pkg);
        }
      } catch (NoSuchMethodException e) {
        // do not need do anything
      }
    }
  }

  /**
   * Generate from a proto file.
   * @param method  classpath
   * @param output  files path
   * @param pkg     package of java file
   * @throws InvocationTargetException  InvocationTargetException
   * @throws IllegalAccessException     IllegalAccessException
   */
  private void generate(Method method, String output, String pkg)
          throws InvocationTargetException, IllegalAccessException {
    File dir = new File(output);
    // get FileDescriptor
    FileDescriptor fileDescriptor = (FileDescriptor) method.invoke(null);
    // Generate all service
    fileDescriptor.getServices().forEach(serviceDescriptor -> generate(serviceDescriptor, pkg, dir));
  }

  /**
   * Generate from a service.
   * @param serviceDescriptor ServiceDescriptor
   * @param pkg               package of java file
   * @param dir               output dir
   */
  private void generate(ServiceDescriptor serviceDescriptor, String pkg, File dir) {
    // create ServiceInfo from service
    ServiceInfo serviceInfo = marshaller.apply(serviceDescriptor);
    // set the pkg
    serviceInfo.setPkg(pkg);
    // generate to file if have method
    if (serviceInfo.getMethods().size() > 0) {
      // cover the serverInfo to string
      String content = contentBuilder.apply(serviceInfo);
      // output to a file
      try {
        output(new File(dir, serviceInfo.getFileName() + getFileFormat()), content);
      } catch (Exception e) {
        throw new RuntimeException("" + e.getMessage(), e);
      }
    }
  }

  /**
   * Get the format of the generate file.
   * @return  the format of the generate file
   */
  protected abstract String getFileFormat();
}
