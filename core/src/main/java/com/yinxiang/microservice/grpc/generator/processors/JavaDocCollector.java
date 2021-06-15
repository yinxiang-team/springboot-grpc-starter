package com.yinxiang.microservice.grpc.generator.processors;

import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.RootDoc;
import com.yinxiang.microservice.grpc.doc.RestApiJsonGenerator;

import java.util.Optional;
import java.util.function.BiFunction;

public class JavaDocCollector implements BiFunction<String, String, Optional<ClassDoc>> {

  /** 文档根节点 */
  private static RootDoc root;

  /**
   * javadoc调用入口
   *
   * @param root
   * @return
   */
  public static boolean start(RootDoc root) {
    JavaDocCollector.root=root;
    return true;
  }

  @Override
  public Optional<ClassDoc> apply(String path, String classname) {
    com.sun.tools.javadoc.Main.execute(new String[]{
            "-doclet",
            RestApiJsonGenerator.class.getName(),
            "-docletpath",
            RestApiJsonGenerator.class.getResource("/").getPath(),
            "-encoding",
            "utf-8",
            path
    });
    ClassDoc[] classes = root.classes();
    for (ClassDoc classDoc : classes) {
      if (classname.equals(classDoc.name())) {
        return Optional.of(classDoc);
      }
    }
//    throw new RuntimeException("Do not found classname: " + classname + " in path: " + path);
    return Optional.empty();
  }
}
