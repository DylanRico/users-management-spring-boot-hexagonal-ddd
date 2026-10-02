package com.jcaa.usersmanagement.infrastructure.config;

import com.jcaa.usersmanagement.infrastructure.adapter.persistence.config.DatabaseConfig;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration(proxyBeanMethods = false)
public class DataSourceSpringConfig {

  private static final String PROP_DB_HOST     = "${db.host}";
  private static final String PROP_DB_PORT     = "${db.port}";
  private static final String PROP_DB_NAME     = "${db.name}";
  private static final String PROP_DB_USERNAME = "${db.username}";
  private static final String PROP_DB_PASSWORD = "${db.password}";
  private static final String PROP_DB_SSL_MODE = "${db.ssl-mode}";

  private static final String LOG_DATASOURCE_INIT = "[DataSourceSpringConfig] DataSource inicializado. host={} port={}";

  @Value(PROP_DB_HOST)
  private String dbHost;

  @Value(PROP_DB_PORT)
  private int dbPort;

  @Value(PROP_DB_NAME)
  private String dbName;

  @Value(PROP_DB_USERNAME)
  private String dbUsername;

  @Value(PROP_DB_PASSWORD)
  private String dbPassword;

  @Value(PROP_DB_SSL_MODE)
  private String dbSslMode;

  @Value("${db.engine:mysql}")
  private String dbEngine;

  @Value("${db.url:}")
  private String dbUrl;

  @Value("${db.pool-size:10}")
  private int poolSize;

  @Bean
  public DataSource dataSource() {
    final DatabaseConfig config =
        new DatabaseConfig(dbHost, dbPort, dbName, dbUsername, dbPassword, dbSslMode);

    if (!dbEngine.equals("mysql") && !dbEngine.equals("postgresql")) {
      throw new IllegalArgumentException("DB_ENGINE debe ser mysql o postgresql");
    }
    final String jdbcUrl = dbUrl.isBlank() && dbEngine.equals("mysql") ? config.buildJdbcUrl() : dbUrl;
    final String prefix = dbEngine.equals("mysql") ? "jdbc:mysql:" : "jdbc:postgresql:";
    if (!jdbcUrl.startsWith(prefix)) {
      throw new IllegalArgumentException("DB_URL debe corresponder a DB_ENGINE");
    }
    final HikariConfig hikariConfig = new HikariConfig();
    hikariConfig.setJdbcUrl(jdbcUrl);
    hikariConfig.setUsername(config.username());
    hikariConfig.setPassword(config.password());
    hikariConfig.setMaximumPoolSize(poolSize);
    hikariConfig.setMinimumIdle(Math.min(2, poolSize));
    hikariConfig.setConnectionTimeout(30_000);

    log.info(LOG_DATASOURCE_INIT, dbHost, dbPort);
    return new HikariDataSource(hikariConfig);
  }
}

