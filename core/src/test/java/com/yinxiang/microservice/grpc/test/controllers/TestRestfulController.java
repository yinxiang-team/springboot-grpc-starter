package com.yinxiang.microservice.grpc.test.controllers;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.googlecode.protobuf.format.JsonFormat;
import com.yinxiang.grpc.http.Headers;
import com.yinxiang.microservice.grpc.controller.BaseGrpcController;
import com.yinxiang.microservice.grpc.inject.Marshaller;
import com.yinxiang.microservice.grpc.test.SearchRequest;
import com.yinxiang.microservice.grpc.test.TestCases;
import com.yinxiang.microservice.grpc.test.TestServiceGrpc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.util.Map;

import static com.yinxiang.microservice.grpc.inject.header.StubHeadersProcessor.createMetadata;

@RestController
public class TestRestfulController extends BaseGrpcController {
  private static final Logger log = LoggerFactory.getLogger(TestCases.class);
  private final TestServiceGrpc.TestServiceImplBase grpc;

  public TestRestfulController(TestServiceGrpc.TestServiceImplBase grpc) {
    this.grpc = grpc;
  }

  @RequestMapping(value = "/rest1", method = RequestMethod.GET)
  public String rest(String arg0, String arg1, Map<String, String> headers) {
    return _processHeadersAndDo(headers, message -> {
      Preconditions.checkArgument(!Strings.isNullOrEmpty(message.getAUTHORIZATION()));
      log.info("TestRestfulController.rest header.auth: {}", message.getAUTHORIZATION());
      return arg0 + arg1;
    });
  }

  @RequestMapping(value = "/rest2", method = RequestMethod.GET)
  public String rest(String arg0, String arg1, MultiValueMap<String, String> headers) {
    return _processHeadersAndDo(headers, message -> {
      Preconditions.checkArgument(!Strings.isNullOrEmpty(message.getAUTHORIZATION()));
      log.info("TestRestfulController.rest header.auth: {}", message.getAUTHORIZATION());
      return arg0 + arg1;
    });
  }

  @RequestMapping(value = "/rest3", method = RequestMethod.POST)
  public String rest(@RequestBody String params, @RequestHeader Map<String, String> headers) throws JsonFormat.ParseException {
    return _process(params, headers, SearchRequest.newBuilder(), grpc::search, TestServiceGrpc.getSearchMethod());
  }

  @RequestMapping(value = "/rest4", method = RequestMethod.GET)
  public String rest(@RequestHeader Map<String, String> headers) {
    _processHeadersAndDo(headers, header -> {}, TestServiceGrpc.getSearchMethod());
    _processHeadersAndDo(headers, header -> {});
    return _processHeadersAndDo(headers, Headers::getAUTHORIZATION);
  }

  @RequestMapping(value = "/rest5", method = RequestMethod.GET)
  public String rest(@RequestHeader MultiValueMap<String, String> headers) {
    _processHeadersAndDo(headers, metadata -> {}, TestServiceGrpc.getSearchMethod());
    _processHeadersAndDo(headers, metadata -> {});
    return _processHeadersAndDo(headers, Headers::getAUTHORIZATION);
  }

  @RequestMapping(value = "/rest6", method = RequestMethod.GET)
  public void rest(@RequestHeader MultiValueMap<String, String> headers, @RequestBody String params) {
    SearchRequest request = Marshaller.fromJsonButNot(SearchRequest.newBuilder(), params).build();
    _processInterceptors(createMetadata(headers), metadata -> {}, TestServiceGrpc.getSearchMethod()).onMessage(request);
  }

  @RequestMapping(value = "/error1", method = RequestMethod.GET)
  public void error(@RequestBody String params, HttpServletResponse response) {
    SearchRequest request = Marshaller.fromJsonButNot(SearchRequest.newBuilder(), params).build();
    log.info(responseError(response, request, HttpStatus.EXPECTATION_FAILED));
  }

  @RequestMapping(value = "/error2", method = RequestMethod.GET)
  public void error(HttpServletResponse response) {
    log.info(responseError(response, SearchRequest.newBuilder().build()));
  }

  @RequestMapping(value = "/error3", method = RequestMethod.GET)
  public void error3(@RequestBody String params, HttpServletResponse response) {
    SearchRequest request = Marshaller.fromJsonButNot(SearchRequest.newBuilder(), params).build();
    log.info(responseError(response, request, HttpStatus.EXPECTATION_FAILED.value()));
  }
}
