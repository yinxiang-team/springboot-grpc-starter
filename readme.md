springboot-grpc

# MAVEN:

```
<dependency>
  <groupId>com.yinxiang.microservice</groupId>
  <artifactId>grpc-starter-core</artifactId>
  <version>1.0.0</version>
</dependency>
```

# SUMMARY:

1. gRPC client inject.
2. HTTP to gRPC server interceptor adapt.
3. New standard for gRPC metadata(http headers).

## gRPC client inject

**work step:**

1. Spring execute auto config load `GrpcInjectAutoConfiguration`.
2. `GrpcInjectAutoConfiguration` import `AutoConfiguredGrpcClientScannerRegistrar`.
3. Spring call `AutoConfiguredGrpcClientScannerRegistrar#registerBeanDefinitions`.
4. Create `ClassPathGrpcClientScanner` and scan interfaces which with `GrpcClient` annotation.
5. Replace bean class to `GrpcClientCreator` and autowire it's properties.
6. Spring load `GrpcServerProperties`.
7. Spring create a `GrpcClientCreator` with custom interface which annotation by `@GrpcClient`.
8. Spring autowire `GrpcServerProperties` to `GrpcClientCreator` and record aliases of services.
9. Spring call `GrpcClientCreator`'s create method.
10. `GrpcClientCreator` create `AbstractSub`.
11. `GrpcClientCreator` create `GrpcClientProxyFactory`, 
    `GrpcClientProxyFactory` create a implement class for custom interface by lib `javasissit` and create the instance.
12. Return the instance to Spring.

**yml file config:**
```
grpc:
    # this application's gRPC port
    port: 8000
    enableReflection: true
    # map of services
    services: 
        # the name of gRPC service
        test-service: 
            # the host of gRPC service
            host: localhost
            # the port of gRPC service
            port: 8006
            # the unique alias of gRPC service's stub package in services scope
            name: grpc.test
        ...
```
**example:**
```
package com.yinxiang.microservice.grpc.test;

import com.yinxiang.microservice.grpc.inject.LogSupplier;
import com.yinxiang.microservice.grpc.inject.StubInterceptor;
import com.yinxiang.microservice.grpc.inject.annotations.GrpcClient;
import com.yinxiang.microservice.grpc.inject.annotations.GrpcHeader;
import com.yinxiang.microservice.grpc.inject.annotations.GrpcMethod;
import com.yinxiang.microservice.grpc.inject.annotations.GrpcParam;
import com.yinxiang.microservice.grpc.inject.header.StubHeadersProcessor;

import static com.google.common.base.Preconditions.checkNotNull;

@GrpcClient(stub = TestServiceGrpc.TestServiceBlockingStub.class)
public interface TestGrpcService extends LogSupplier {
  @GrpcMethod(method = "search", safe = true)
  SearchReply s(@GrpcParam(param = "id") int id);
  SearchReply search(@GrpcParam(param = "id") int id);
  DetailReply detail(@GrpcParam(param = "id") int id);
  SearchReply search(@GrpcParam(param = "id") int id, @GrpcHeader(name = "auth") String a);
  SearchReply search(@GrpcParam(param = "id") int id, StubInterceptor<TestServiceGrpc.TestServiceBlockingStub> interceptor);
  DetailReply detail(@GrpcParam(param = "id") int a, @GrpcHeader(name = "auth", type = String.class) String auth);

  default SearchReply searchWithLog(int id) {
    _log().info("will search with id: {}.", id);
    search(id, stub -> StubHeadersProcessor.withHeader(checkNotNull(stub), "auth", "auth"));
    return search(id, stub -> StubHeadersProcessor.withHeader(checkNotNull(stub), "auth", String.class, "auth"));
  }
}
```

## HTTP to gRPC server interceptor adapt

**restful:**

* Extends `BaseGrpcController`.
* Please call one of 
```
void _processHeadersAndDo(MultiValueMap<String, String> headers, Consumer<Headers> consumer, MethodDescriptor method)
void _processHeadersAndDo(Map<String, String> headers, Consumer<Headers> consumer, MethodDescriptor method)
void _processHeadersAndDo(MultiValueMap<String, String> headers, Consumer<Headers> consumer)
void _processHeadersAndDo(Map<String, String> headers, Consumer<Headers> consumer)
void _processHeadersAndDo(MultiValueMap<String, String> headers, Function<Headers, T> supplier)
void _processHeadersAndDo(MultiValueMap<String, String> headers, Consumer<Headers> consumer)
void _processHeadersAndDo(Map<String, String> headers, Consumer<Headers> consumer)
T _processHeadersAndDo(MultiValueMap<String, String> headers, Function<Headers, T> supplier)
T _processHeadersAndDo(Map<String, String> headers, Function<Headers, T> supplier)
```
* In the `BaseGrpcController` headers will transform to a `Metadata`.
* `BaseGrpcController` call all interceptors and process the headers in method(`_process(Metadata metadata, Consumer<T> consumer, MethodDescriptor method)`).
* `BaseGrpcController` bring the headers call the callback.

**gRPC:**

* `BaseGrpcController` also support gRPC method call from restful Controller.
* Use maven build plugin can generator some controllers from proto's service, 
    and the methods of these controller will call
```
_process(
          String params,
          MultiValueMap<String, String> headers,
          B.Builder builder,
          BiConsumer<B, StreamObserver<T>> consumer,
          MethodDescriptor method
  ) throws JsonFormat.ParseException
```

## New gRPC headers process standard

* In yinxiang/grpc/http/header.proto has the `Headers` and `HeadersFilter` message. 
* In yinxiang/grpc/http/http.proto extend `google.protobuf.MethodOptions` with `HeadersFilter`. 
* `HeadersFilter` tell `HttpHeadersInterceptor` which values need check in `Metadata`, please assign at rpc option like this:
```
  rpc Search (SearchRequest) returns (SearchReply) {
    option (google.api.http) = {
      post: "/search"
    };
    option (yinxiang.grpc.http.headers) = {};
  }
```
* Then `HttpHeadersInterceptor` will collect the `Metadata`'s values which checked by `HeadersFilter` and set into `Headers`.
* The `Headers` can get from a static method `HttpHeadersInterceptor.getHeader()`, it cached in the current `Context` by 
    `ThreadLocal`, and the `HttpHeadersInterceptor` must the last interceptor.
* In processing message stage, `AbstractHeadersInterceptor.HeaderServerCallListener` can assign to a specify field of message by option:
```
message Request {
  yinxiang.grpc.http.Headers headers = 3 [(yinxiang.grpc.http.referenceHeader) = ""];
}
```

License
=======
    Copyright (c) 2012-2021 by Yinxiangbiji Corporation, All rights reserved.

    Use of the source code and binary libraries included in this package
    is permitted under the following terms:

    Redistribution and use in source and binary forms, with or without
    modification, are permitted provided that the following conditions
    are met:

        1. Redistributions of source code must retain the above copyright
        notice, this list of conditions and the following disclaimer.
        2. Redistributions in binary form must reproduce the above copyright
        notice, this list of conditions and the following disclaimer in the
        documentation and/or other materials provided with the distribution.

    THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
    IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
    OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
    IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
    INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
    NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
    DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
    THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
    (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
    THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
