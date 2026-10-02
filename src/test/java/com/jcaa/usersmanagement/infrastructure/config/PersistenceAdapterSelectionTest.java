package com.jcaa.usersmanagement.infrastructure.config;

import com.jcaa.usersmanagement.application.port.out.SaveUserPort;
import com.jcaa.usersmanagement.infrastructure.adapter.persistence.repository.UserRepositoryMySQL;
import com.jcaa.usersmanagement.infrastructure.adapter.persistence.repository.UserRepositoryPostgreSQL;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class PersistenceAdapterSelectionTest {
  private final ApplicationContextRunner context = new ApplicationContextRunner()
      .withBean(DataSource.class, () -> mock(DataSource.class))
      .withUserConfiguration(UserRepositoryMySQL.class, UserRepositoryPostgreSQL.class);

  @Test
  void selectsExactlyOneAdapterForEachEngine() {
    context.run(app -> assertThat(app.getBean(SaveUserPort.class)).isExactlyInstanceOf(UserRepositoryMySQL.class));
    context.withPropertyValues("db.engine=postgresql").run(app ->
        assertThat(app.getBean(SaveUserPort.class)).isExactlyInstanceOf(UserRepositoryPostgreSQL.class));
    context.withPropertyValues("db.engine=unsupported").run(app ->
        assertThat(app).doesNotHaveBean(SaveUserPort.class));
  }
}
