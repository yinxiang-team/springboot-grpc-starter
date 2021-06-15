package com.yinxiang.microservice.grpc.util;

public final class StringUtils {
  public static String firstUpper(String str) {
    return str.length() > 0 ? str.substring(0, 1).toUpperCase() + (str.length() > 1 ? str.substring(1) : "") : str;
  }

  public static String firstLower(String str) {
    return str.length() > 0 ? str.substring(0, 1).toLowerCase() + (str.length() > 1 ? str.substring(1) : "") : str;
  }

  public static String makeClassName(String str) {
    if (str.contains(".")) {
      return str;
    }
    String[] infos = str.split("-");
    StringBuilder nameBuilder = new StringBuilder();
    for (String info : infos) {
      nameBuilder.append(firstUpper(info));
    }
    return nameBuilder.toString();
  }

  public static String makeAlias(String str) {
    return firstLower(makeClassName(str));
  }
}
