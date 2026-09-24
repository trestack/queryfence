package com.northwind.shop;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;

/** Base class: the application context on a real Postgres, one transaction per test. */
@SpringBootTest
@Transactional
abstract class AbstractShopTest {

  /** The tenant the request context would carry. */
  protected static final long TENANT = 7L;

  /** Another customer of the platform, whose rows must never show up. */
  protected static final long OTHER_TENANT = 8L;

  @DynamicPropertySource
  static void datasource(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", Databases.POSTGRES::getJdbcUrl);
    registry.add("spring.datasource.username", Databases.POSTGRES::getUsername);
    registry.add("spring.datasource.password", Databases.POSTGRES::getPassword);
  }
}
