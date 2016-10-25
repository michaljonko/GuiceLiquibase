package pl.coffeepower.guiceliquibase;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.junit.Assert.assertThat;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Collections;

import javax.sql.DataSource;

@RunWith(MockitoJUnitRunner.class)
public class GuiceLiquibaseConfigTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Mock
  private DataSource dataSource;

  @Test
  public void shouldCreateEmptyConfig() throws Exception {
    GuiceLiquibaseConfig config = GuiceLiquibaseConfig.Builder
        .createConfigSet()
        .build();
    assertThat(config, notNullValue());
    assertThat(config.getConfigs(), empty());
  }

  @Test
  public void shouldCreateEmptyConfigs() throws Exception {
    GuiceLiquibaseConfig config = GuiceLiquibaseConfig.Builder
        .createConfigSet()
        .withLiquibaseConfigs(Collections.emptyList())
        .build();
    assertThat(config, notNullValue());
    assertThat(config.getConfigs(), empty());
  }

  @Test
  public void shouldCreateConfig() throws Exception {
    GuiceLiquibaseConfig.LiquibaseConfig liquibaseConfig =
        new GuiceLiquibaseConfig.LiquibaseConfig(dataSource);
    GuiceLiquibaseConfig config = GuiceLiquibaseConfig.Builder
        .createConfigSet()
        .withLiquibaseConfig(liquibaseConfig)
        .build();
    assertThat(config, notNullValue());
    assertThat(config.getConfigs(), containsInAnyOrder(liquibaseConfig));
  }

  @Test
  public void shouldThrowExceptionForNotDefinedConfig() throws Exception {
    expectedException.expect(NullPointerException.class);
    GuiceLiquibaseConfig.Builder.createConfigSet()
        .withLiquibaseConfig(null)
        .build();

  }

  @Test
  public void shouldThrowExceptionForNotDefinedConfigs() throws Exception {
    expectedException.expect(NullPointerException.class);
    GuiceLiquibaseConfig.Builder.createConfigSet()
        .withLiquibaseConfigs(null)
        .build();

  }
}