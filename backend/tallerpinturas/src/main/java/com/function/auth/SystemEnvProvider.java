package com.function.auth;

public class SystemEnvProvider implements EnvProvider {
  @Override
  public String get(String key) {
    return System.getenv(key);
  }
}
