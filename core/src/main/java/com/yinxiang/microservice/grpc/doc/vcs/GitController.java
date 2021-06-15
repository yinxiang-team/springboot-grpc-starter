package com.yinxiang.microservice.grpc.doc.vcs;

import com.google.common.base.Preconditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * Based git for version controller.
 * @author Huiyuan Fu
 */
public class GitController implements VersionController {
  private static final Logger log = LoggerFactory.getLogger(GitController.class);

  @Override
  public void checkout(String url, String localPath) throws Exception {
    Runtime.getRuntime().exec("git clone " + url, null, new File(localPath)).waitFor();
    log.info("GitController.checkout: {} to {}", url, localPath);
  }

  @Override
  public void commit(String localPath) throws Exception {
    File gitFile = new File(localPath);
    Preconditions.checkArgument(gitFile.exists());
    Runtime.getRuntime().exec("git commit -a -m \"GitController\"", null, gitFile).waitFor();
    Runtime.getRuntime().exec("git push", null, gitFile).waitFor();
    log.info("GitController.commit: {}", gitFile.getName());
  }

  @Override
  public void add(File file) throws Exception {
    Runtime.getRuntime().exec("git add " + file.getName(), null, file.getParentFile()).waitFor();
    log.info("GitController.add: {}", file.getName());
  }
}
