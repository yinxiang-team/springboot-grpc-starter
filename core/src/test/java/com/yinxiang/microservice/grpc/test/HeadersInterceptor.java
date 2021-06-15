package com.yinxiang.microservice.grpc.test;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.yinxiang.grpc.http.Headers;
import com.yinxiang.grpc.http.HeadersFilter;
import com.yinxiang.microservice.grpc.GrpcGlobalInterceptor;
import com.yinxiang.microservice.grpc.autoconfigure.GrpcServerProperties;
import com.yinxiang.microservice.grpc.inject.header.HttpHeadersInterceptor;
import io.grpc.Metadata;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import static com.google.common.base.Preconditions.checkArgument;

@GrpcGlobalInterceptor
public class HeadersInterceptor extends HttpHeadersInterceptor {
  public HeadersInterceptor(GrpcServerProperties properties) throws Exception {
    super(extendGrpcServerProperties(properties));
  }

  private static GrpcServerProperties extendGrpcServerProperties(GrpcServerProperties properties) {
    GrpcServerProperties ret = new GrpcServerProperties();
    ret.setEnabled(properties.isEnabled());
    ret.setEnableReflection(properties.isEnableReflection());
    ret.setHeaderNames(properties.getHeaderNames() == null ? Maps.newHashMap() : properties.getHeaderNames());
    ret.setInProcessServerName(properties.getInProcessServerName());
    ret.setPort(properties.getPort());
    ret.setServices(properties.getServices());

    appendHeaderNames(ret.getHeaderNames(), "AUTHORIZATION".toLowerCase(), "auth", "auth-token");
    appendHeaderNames(ret.getHeaderNames(), "FORWARDED".toLowerCase(), "x-forward-for");
    return ret;
  }

  private static void appendHeaderNames(Map<String, Set<String>> headerNames, String header, String... names) {
    if (names.length > 0) {
      Set<String> set = Sets.newHashSet(headerNames.computeIfAbsent(header, name -> Sets.newHashSet()));
      set.addAll(Arrays.asList(names));
      headerNames.put(header, set);
    }
  }

  @Override
  protected Map<String, Predicate<String>> putCheckers(Map<String, Predicate<String>> checkers) {
    super.putCheckers(checkers);
    checkers.put("AUTHORIZATION", HeadersInterceptor::checkAuth);
    return checkers;
  }

  @Override
  protected Object processExtendHeaders(Metadata metadata, HeadersFilter headerNames) {
    checkArgument("auth".equals(getHeader(metadata, "auth", Sets.newHashSet())));
//    getHeader(metadata, "host", Strings::isNullOrEmpty);
    checkArgument("auth".equals(getHeader(metadata, "auth")));
    return null;
  }

  private static boolean checkAuth(String auth) {
    return "auth".equals(auth);
  }

  static String getAuth() {
    return getHeader().map(Headers::getAUTHORIZATION).orElse("");
  }

  static String getUserAgent() {
    return getHeader().map(Headers::getUSERAGENT).orElse("");
  }

  static String getIp() {
    return getHeader().map(Headers::getFORWARDED).orElse("");
  }
}
