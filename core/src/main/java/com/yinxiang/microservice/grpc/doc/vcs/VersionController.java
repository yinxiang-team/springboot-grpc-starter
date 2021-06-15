package com.yinxiang.microservice.grpc.doc.vcs;

import java.io.File;

/**
 * The version controller.
 * @author Huiyuan Fu
 */
public interface VersionController {
  /**
   * Check out the newest version resources from repository.
   * @param url       version url
   * @param localPath target path
   * @throws Exception  Exception
   */
  void checkout(String url, String localPath) throws Exception;

  /**
   * Commit resources to repository.
   * @param localPath target path
   * @throws Exception  Exception
   */
  void commit(String localPath) throws Exception;

  /**
   * Add a file to resources.
   * @param file  new file
   * @throws Exception  Exception
   */
  void add(File file) throws Exception;
}
