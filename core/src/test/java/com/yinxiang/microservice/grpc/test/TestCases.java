package com.yinxiang.microservice.grpc.test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.googlecode.protobuf.format.JsonFormat;
import com.yinxiang.microservice.grpc.controller.ClassPathLoader;
import com.yinxiang.microservice.grpc.inject.channels.ReusableChannel;
import com.yinxiang.microservice.grpc.inject.spring.ClassPathGrpcClientScanner;
import com.yinxiang.microservice.grpc.inject.spring.MetricGrpcClientFactoryBean;
import com.yinxiang.microservice.grpc.test.controllers.TestRestfulController;
import com.yinxiang.microservice.grpc.test.controllers.TestServiceController;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.yinxiang.microservice.grpc.inject.Marshaller.*;
import static io.grpc.Status.Code.INVALID_ARGUMENT;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = {TestCases.class})
@ComponentScan({"com.yinxiang.microservice.grpc.test"})
@EnableAutoConfiguration
public class TestCases {
  private static final Logger log = LoggerFactory.getLogger(TestCases.class);
  @Autowired
  private TestGrpcService testGrpcService;
  @Autowired
  private TestServiceController testServiceController;
  @Autowired
  private TestRestfulController testRestfulController;

  static {
    ClassPathGrpcClientScanner.setGrpcClientCreatorClass(MetricGrpcClientFactoryBean.class);
  }

  private void testNoHeader(Runnable runnable) {
    try {
      runnable.run();
      throw new StatusRuntimeException(Status.UNKNOWN);
    } catch (StatusRuntimeException e) {
      if (e.getStatus().getCode() != INVALID_ARGUMENT) {
        throw e;
      }
    }
  }

  @Test
  public void testNoHeader() {
    testNoHeader(() -> testGrpcService.search(1));
    testNoHeader(() -> testGrpcService.detail(2));
    checkArgument(testGrpcService.s(3) == null);
  }

  @Test
  public void testWithAuth() {
    log.info("search result: {}.", checkNotNull(testGrpcService.search(1, "auth").getResult()));
    log.info("searchWithLog result: {}.", checkNotNull(testGrpcService.searchWithLog(2).getResult()));
    log.info("detail result: {}.", checkNotNull(testGrpcService.detail(3, "auth").getResult()));
  }

  @Test
  public void testGrpcController() throws JsonFormat.ParseException {
    Map<String, Object> body = ImmutableMap.of("id", 1);
    HttpHeaders headers = new HttpHeaders();
    headers.add("auth", "auth");
    headers.add("user-agent", "class");
    log.info("search result: {}.", testServiceController.Search(commonToJson(body), headers));
    body = ImmutableMap.of("id", 2);
    log.info("detail result: {}.", testServiceController.Detail(commonToJson(body), headers));
  }

  @Test
  public void testRestController() throws JsonFormat.ParseException {
    Map<String, String> headers = ImmutableMap.of("auth", "auth", "user-agent", "class");
    String arg0 = "search", arg1 = "detail";
    checkArgument((arg0 + arg1).equals(testRestfulController.rest(arg0, arg1, headers)));
    HttpHeaders httpHeaders = new HttpHeaders();
    httpHeaders.add("auth", "auth");
    httpHeaders.add("user-agent", "class");
    checkArgument((arg0 + arg1).equals(testRestfulController.rest(arg0, arg1, httpHeaders)));

    checkArgument("auth".equals(testRestfulController.rest(headers)));
    checkArgument("auth".equals(testRestfulController.rest(httpHeaders)));

    SearchRequest searchRequest = SearchRequest.newBuilder().setId(1).addAllExp(Lists.newArrayList(2.0)).build();
    String searchJson = toJson(searchRequest);
    checkArgument(!searchRequest.toString().equals(testRestfulController.rest(searchJson, headers)));
    testRestfulController.rest(httpHeaders, searchJson);

    MockHttpServletResponse response = new MockHttpServletResponse();
    testRestfulController.error(searchJson, response);
    checkArgument(response.getStatus() == HttpStatus.EXPECTATION_FAILED.value());
    response = new MockHttpServletResponse();
    testRestfulController.error(response);
    checkArgument(response.getStatus() == HttpStatus.INTERNAL_SERVER_ERROR.value());
    response = new MockHttpServletResponse();
    testRestfulController.error3(searchJson, response);
    checkArgument(response.getStatus() == HttpStatus.EXPECTATION_FAILED.value());
  }

  @Test
  public void testMarshaller() throws JsonFormat.ParseException {
    SearchRequest searchRequest = SearchRequest.newBuilder().setId(1).addAllExp(Lists.newArrayList(2.0)).build();
    String searchJson = toJson(searchRequest);
    checkArgument(searchRequest.getExp(0) == fromJson(SearchRequest.newBuilder(), searchJson).getExp(0));
    checkArgument(fromJsonButNot(SearchRequest.newBuilder(), "abc").getExpList().size() == 0);

    TestData data = new TestData();
    data.setId(10);
    data.setExp(Lists.newArrayList(33.3d));
    checkArgument(fromJsonButNot(SearchRequest.newBuilder(), commonToJson(data)).getId() == data.getId());
    checkArgument(fromObjectButNot(SearchRequest.newBuilder(), data).getExp(0) == data.getExp().get(0));
    checkArgument(toObject(searchRequest, TestData.class).getId() == searchRequest.getId());
    data.setId(20);
  }

  @Test
  public void testClassPathLoader() throws IOException {
    ClassPathLoader classPathLoader = new ClassPathLoader(ClassLoader.getSystemClassLoader());
    classPathLoader.loadClasses("");
  }

  @Test
  public void testReusableChannel() {
    checkArgument(!new ReusableChannel("localhost:8006").isShutdown());
  }
}

class TestData {
  private int id;
  private List<Double> exp;

  int getId() {
    return id;
  }

  void setId(int id) {
    this.id = id;
  }

  List<Double> getExp() {
    return exp;
  }

  void setExp(List<Double> exp) {
    this.exp = exp;
  }
}
