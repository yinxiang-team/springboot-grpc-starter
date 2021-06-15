package com.yinxiang.microservice.grpc.controller;

import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

/**
 * A loader for load extend class path.
 * @author Huiyuan Fu
 * @since 1.0.0
 */
public class ClassPathLoader extends ClassLoader {
  private static final Logger log = LoggerFactory.getLogger(ClassPathLoader.class);

  public ClassPathLoader(ClassLoader parent) {
    super(parent);
  }

  /**
   * Load all classes from classpath.
   * @param classpath extend classpath
   * @return  the list of classes
   * @throws IOException  IOException
   */
  public List<Class> loadClasses(String classpath) throws IOException {
    log.info("{}.loadClasses load classpath: {}.", getClass().getName(), classpath);
    List<Class> classes = Lists.newLinkedList();
    loadClasses(new File(classpath), classes, "");
    return classes;
  }

  /**
   * Load all classes from a directory.
   * @param dir     a directory
   * @param classes the list of classes
   * @param pkg     the package of parent
   * @throws IOException  IOException
   */
  private void loadClasses(File dir, List<Class> classes, String pkg) throws IOException {
    File[] files = dir.listFiles();
    if (files == null) {
      return;
    }
    for (File file : files) {
      // Recursion load in directory
      if (file.isDirectory()) {
        loadClasses(file, classes, pkg + file.getName() + ".");
      }
      // load .class file
      else if (file.getName().endsWith(".class")) {
        // read byte code
        FileInputStream fis = new FileInputStream(file);
        byte[] b = new byte[(int) file.length()];
        int offset = fis.read(b);
        if (offset != b.length) {
          log.debug("offset != length.", offset, b.length);
        }
        // get the name of class
        String className = pkg + file.getName().replace(".class", "");
        try {
          // define the class into JVM
          Class clz = defineClass(className, b, 0, b.length);
          // resolve class
          resolveClass(clz);
          // add to list
          classes.add(clz);
          log.info("{}.loadClasses load class: {}.", getClass().getName(), clz.getName());
        } catch (Throwable e) {
          log.error("ClassPathLoader.loadClasses Exception: ", e);
        }
      }
    }
  }
}
