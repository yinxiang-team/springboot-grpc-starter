package com.yinxiang.microservice.grpc.generator;

import java.io.File;
import java.io.FileWriter;

/**
 * A interface of gRPC content generator.
 * @author Huiyuan Fu
 */
public interface GrpcInfoGenerator {
  /**
   * Generate content to files.
   * @param classPath classpath
   * @param output    files path
   * @param pkg       package of java file
   * @throws Exception  Exception
   */
  void generator(String classPath, String output, String pkg) throws Exception;

  /**
   * Output content to a file.
   * @param file    file which need output to
   * @param content output content
   * @throws Exception  Exception
   */
  default void output(File file, String content) throws Exception {
    if (!file.exists()) {
      System.out.println(file.createNewFile());
    }
    // output content
    try (FileWriter writer = new FileWriter(file)) {
      writer.append(content);
    }
  }
}
