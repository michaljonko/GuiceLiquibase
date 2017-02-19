package pl.coffeepower.guiceliquibase;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.junit.Assert.assertThat;

import nl.jqno.equalsverifier.EqualsVerifier;

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
        .of()
        .build();
    assertThat(config, notNullValue());
    assertThat(config.getConfigs(), empty());
  }

  @Test
  public void shouldCreateEmptyConfigs() throws Exception {
    GuiceLiquibaseConfig config = GuiceLiquibaseConfig.Builder
        .of()
        .build();
    assertThat(config, notNullValue());
    assertThat(config.getConfigs(), empty());
  }

  @Test
  public void shouldCreateConfig() throws Exception {
    LiquibaseConfig liquibaseConfig = LiquibaseConfig.Builder.of(dataSource).build();
    GuiceLiquibaseConfig config = GuiceLiquibaseConfig.Builder
        .of(liquibaseConfig)
        .build();
    assertThat(config, notNullValue());
    assertThat(config.getConfigs(), containsInAnyOrder(liquibaseConfig));
  }

  @Test
  public void shouldPassEqualsAndHashCodeContracts() throws Exception {
    EqualsVerifier.forClass(GuiceLiquibaseConfig.class)
        .usingGetClass()
        .verify();
  }

  @Test
  public void shouldThrowExceptionForNotDefinedConfig() throws Exception {
    expectedException.expect(NullPointerException.class);
    GuiceLiquibaseConfig.Builder.of(null)
        .build();
  }

  @Test
  public void shouldThrowExceptionForNotDefinedConfigs() throws Exception {
    expectedException.expect(NullPointerException.class);
    GuiceLiquibaseConfig.Builder.of()
        .withLiquibaseConfigs(null)
        .build();
  }
}