package com.jcaa.usersmanagement.infrastructure.adapter.persistence.repository;

import javax.sql.DataSource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

/** PostgreSQL comparte las consultas JDBC y el mapeo del adaptador MySQL. */
@Repository
@ConditionalOnProperty(name = "db.engine", havingValue = "postgresql")
public class UserRepositoryPostgreSQL extends UserRepositoryMySQL {
  public UserRepositoryPostgreSQL(DataSource dataSource) {
    super(dataSource);
  }
}
