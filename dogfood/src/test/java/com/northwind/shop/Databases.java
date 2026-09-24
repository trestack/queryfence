package com.northwind.shop;

import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/** One Postgres container for the whole test run. */
final class Databases {

  static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>(DockerImageName.parse("postgres:17-alpine"));

  static {
    POSTGRES.start();
  }

  private Databases() {}
}
