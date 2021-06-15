package com.yinxiang.microservice.grpc.doc;

import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.yinxiang.microservice.grpc.doc.vcs.GitController;
import com.yinxiang.microservice.grpc.doc.vcs.VersionController;
import com.yinxiang.microservice.grpc.generator.GrpcInfoGenerator;
import org.apache.commons.lang3.tuple.Pair;

import java.io.*;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Optional;

/**
 * The api json files for React generator by gRPC protobuf classes.
 *
 * Can use in maven build plugin like this:
 *
 * <ol>
 *   <li>groupId: org.codehaus.mojo</li>
 *   <li>artifactId: exec-maven-plugin</li>
 *   <li>version: 3.0.0</li>
 *   <li>executions.execution.phase: test</li>
 *   <li>executions.execution.goals.goal: java</li>
 *   <li>executions.execution.configuration.includePluginDependencies: true</li>
 *   <li>executions.execution.configuration.mainClass: com.yinxiang.microservice.grpc.doc.ServiceReactApiGenerator</li>
 *   <li>executions.execution.configuration.arguments.argument: ${basedir}/../test-stub/target/generated-sources/protobuf/</li>
 *   <li>executions.execution.configuration.arguments.argument: ${basedir}/../test-stub/target/classes</li>
 *   <li>executions.execution.configuration.arguments.argument: ${basedir}</li>
 *   <li>executions.execution.configuration.arguments.argument: ssh://git@source.a.b.c.net:7999/web/service-api.git</li>
 *   <li>executions.execution.dependencies.dependency.groupId: com.sun</li>
 *   <li>executions.execution.dependencies.dependency.artifactId: tools</li>
 *   <li>executions.execution.dependencies.dependency.version: 1.8</li>
 *   <li>executions.execution.dependencies.dependency.scope: system</li>
 *   <li>executions.execution.dependencies.dependency.systemPath: ${java.home}/../lib/tools.jar</li>
 * </ol>
 *
 * The first argument is gRPC protobuf classpath.
 * The second argument is resources path.
 * The third argument is git ssh url.
 * @author Huiyuan Fu
 */
public class ServiceReactApiGenerator implements GrpcInfoGenerator {
  private static final String JSON_PATH = "service-api/admin/src/components/apis";
  private static final String JSON_INDEX = "index.json";
  private static final String GIT_NAME = "git remote -v";
  private static final String GIT_BRANCH = "git symbolic-ref --short HEAD";
  private static final Type TYPE = new TypeToken<List<Service>>() {}.getType();

  private final GrpcInfoGenerator jsonGenerator;
  private final VersionController versionController;

  public ServiceReactApiGenerator(String sourcePath) {
    this(new RestApiJsonGenerator(sourcePath), new GitController());
  }

  public ServiceReactApiGenerator(GrpcInfoGenerator jsonGenerator, VersionController versionController) {
    this.jsonGenerator = jsonGenerator;
    this.versionController = versionController;
  }

  @Override
  public void generator(String classPath, String output, String url) throws Exception {
    File vcsDir = new File(output);
    Runtime.getRuntime().exec("rm -rf " + output).waitFor();
    try {
      System.out.println(vcsDir.mkdirs());
      versionController.checkout(url, output);
      File indexFile = new File(new File(vcsDir, JSON_PATH), JSON_INDEX);
      Gson gson = new Gson();
      List<Service> list = gson.fromJson(new InputStreamReader(new FileInputStream(indexFile)), TYPE);
      Pair<File, Branch> pair = changeIndex(
              indexFile,
              list,
              getRepository(getExecResult(GIT_NAME, classPath)),
              getExecResult(GIT_BRANCH, classPath)
      );
      File branchFile = pair.getLeft();
      jsonGenerator.generator(classPath, branchFile.getPath(), url);
      File[] files = branchFile.listFiles();
      if (files != null) {
        List<String> services = pair.getRight().getServices();
        for (File file : files) {
          versionController.add(file);
          if (!services.contains(file.getName())) {
            services.add(file.getName());
          }
        }
        output(indexFile, gson.toJson(list));
      }
      versionController.commit(output + "/service-api");
    } finally {
      Runtime.getRuntime().exec("rm -rf " + output);
    }
  }

  private String getRepository(String repository) {
    return repository.substring(0, repository.lastIndexOf(".git")).substring(repository.lastIndexOf('/') + 1);
  }

  private Pair<File, Branch> changeIndex(File indexFile, List<Service> list, String repository, String branch) {
    Optional<Service> repositoryOption = list.stream()
            .filter(service -> service.getName().equals(repository))
            .findFirst();
    List<Branch> branches = (repositoryOption.isPresent() ? repositoryOption.get() :
            createIndex(indexFile, list, repository, branch)).getBranches();
    File branchFile = new File(new File(indexFile.getParentFile(), repository), branch);
    Optional<Branch> branchOptional = branches.stream().filter(b -> b.getName().equals(branch)).findFirst();
    Branch b = branchOptional.isPresent() ? branchOptional.get() : createBranch(branches, branch);
    System.out.println(branchFile.mkdir());
    return Pair.of(branchFile, b);
  }

  private Service createIndex(File indexFile, List<Service> list, String repository, String branch) {
    Service service = new Service();
    service.setName(repository);
    List<Branch> branches = Lists.newLinkedList();
    createBranch(branches, branch);
    service.setBranches(branches);
    list.add(service);
    System.out.println(new File(indexFile.getParentFile(), repository).mkdir());
    return service;
  }

  private Branch createBranch(List<Branch> branches, String name) {
    Branch branch = new Branch();
    branch.setName(name);
    branch.setServices(Lists.newLinkedList());
    branches.add(branch);
    return branch;
  }

  private String getExecResult(String command, String classPath) throws Exception {
    Process process = Runtime.getRuntime().exec(command, null, new File(classPath));
    InputStream is = process.getErrorStream();
    boolean noError = is.available() == 0;
    if (noError) {
      is = process.getInputStream();
      try (LineNumberReader reader = new LineNumberReader(new InputStreamReader(is))) {
        return reader.readLine();
      }
    }
    byte[] data = new byte[is.available()];
    System.out.println(is.read(data));
    throw new Exception(new String(data));
  }

  public static void main(String[] args) throws Exception {
//    args = new String[] {
//            "/Users/FuHuiyuan/IdeaProjects/springboot-grpc-starter/core/../test-stubs/target/generated-sources/protobuf/",
//            "/Users/FuHuiyuan/IdeaProjects/springboot-grpc-starter/core/../test-stubs/target/classes",
//            "/Users/FuHuiyuan/IdeaProjects/springboot-grpc-starter/core/_service-api",
//            "ssh://git@source.test1.bj.yxops.net:7999/web/service-api.git",
//    };
    System.out.println(args[0]);
    System.out.println(args[1]);
    System.out.println(args[2]);
    System.out.println(args[3]);
    new ServiceReactApiGenerator(args[0]).generator(args[1], args[2], args[3]);
  }
}

class Service {
  private String name;
  private List<Branch> branches;

  String getName() {
    return name;
  }

  void setName(String name) {
    this.name = name;
  }

  List<Branch> getBranches() {
    return branches;
  }

  void setBranches(List<Branch> branches) {
    this.branches = branches;
  }
}

class Branch {
  private String name;
  private List<String> services;

  String getName() {
    return name;
  }

  void setName(String name) {
    this.name = name;
  }

  List<String> getServices() {
    return services;
  }

  void setServices(List<String> services) {
    this.services = services;
  }
}
