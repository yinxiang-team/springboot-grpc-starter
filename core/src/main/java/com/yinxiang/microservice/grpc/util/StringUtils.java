package com.yinxiang.microservice.grpc.util;

/**
 * The utils string.
 * @author Huiyuan Fu
 * @since 1.0.0
 */
public final class StringUtils {
  /**
   * Fix the first char upper case.
   * @param str string
   * @return  result
   */
  public static String firstUpper(String str) {
    return str.length() > 0 ? str.substring(0, 1).toUpperCase() + (str.length() > 1 ? str.substring(1) : "") : str;
  }
}
