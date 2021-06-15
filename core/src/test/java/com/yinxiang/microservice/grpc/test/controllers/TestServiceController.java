package com.yinxiang.microservice.grpc.test.controllers;

import com.googlecode.protobuf.format.JsonFormat;
import com.yinxiang.microservice.grpc.test.TestServiceGrpc;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;

/**
 * This is a generate file.
 * 2021年6月10日 星期四
 * A rest http local proxy for TestService.
 * Powered by com.yinxiang.microservice.grpc.controller.
 */
@RestController
public class TestServiceController extends com.yinxiang.microservice.grpc.controller.BaseGrpcController {
	private final TestServiceGrpc.TestServiceImplBase grpc;

	public TestServiceController(TestServiceGrpc.TestServiceImplBase grpc) {
		this.grpc = grpc;
	}

	@RequestMapping(value = "/search", method = RequestMethod.POST)
	public String Search(@RequestBody String params, @RequestHeader MultiValueMap<String, String> headers) throws JsonFormat.ParseException {
		return _process(params, headers, com.yinxiang.microservice.grpc.test.SearchRequest.newBuilder(), grpc::search, TestServiceGrpc.getSearchMethod());
	}

	@RequestMapping(value = "/detail", method = RequestMethod.POST)
	public String Detail(@RequestBody String params, @RequestHeader MultiValueMap<String, String> headers) throws JsonFormat.ParseException {
		return _process(params, headers, com.yinxiang.microservice.grpc.test.DetailRequest.newBuilder(), grpc::detail, TestServiceGrpc.getDetailMethod());
	}
}