package pl.coffeepower.guiceliquibase;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static pl.coffeepower.guiceliquibase.GuiceLiquibaseConfig.Builder;

import com.google.common.collect.Lists;

import nl.jqno.equalsverifier.EqualsVerifier;

import org.hsqldb.jdbc.JDBCDataSource;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.List;

import javax.sql.DataSource;

import be.joengenduvel.java.verifiers.ToStringVerifier;

public class GuiceLiquibaseConfigTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void shouldCreateEmptyConfigs() throws Exception {
    GuiceLiquibaseConfig config = Builder.of()
        .build();

    assertThat(config, notNullValue());
    assertThat(config.getConfigs(), empty());
  }

  @Test
  public void shouldCreateConfigForSingleLiquibaseConfig() throws Exception {
    LiquibaseConfig liquibaseConfig = LiquibaseConfig.Builder.of(mock(DataSource.class))
        .build();
    GuiceLiquibaseConfig config = Builder.of(liquibaseConfig)
        .build();

    assertThat(config, notNullValue());
    assertThat(config.getConfigs(), containsInAnyOrder(liquibaseConfig));
  }

  @Test
  public void shouldCreateConfigForMultipleLiquibaseConfigs() throws Exception {
    LiquibaseConfig firstLiquibaseConfig = LiquibaseConfig.Builder.of(mock(DataSource.class))
        .build();
    LiquibaseConfig secondLiquibaseConfig = LiquibaseConfig.Builder.of(mock(DataSource.class))
        .build();
    List<LiquibaseConfig> configs = Lists.newArrayList(firstLiquibaseConfig, secondLiquibaseConfig);
    GuiceLiquibaseConfig config = Builder.of()
        .withLiquibaseConfigs(configs)
        .build();

    assertThat(config, notNullValue());
    assertThat(config.getConfigs(), hasSize(2));
    assertThat(config.getConfigs(), containsInAnyOrder(firstLiquibaseConfig, secondLiquibaseConfig));
  }

  @Test
  public void shouldThrowExceptionForNotDefinedConfig() throws Exception {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage(containsString("config must be defined."));

    Builder.of(null);
  }

  @Test
  public void shouldThrowExceptionForNotDefinedConfigAddedToBuilder() throws Exception {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage(containsString("config must be defined."));

    Builder.of()
        .withLiquibaseConfig(null);
  }

  @Test
  public void shouldThrowExceptionForNotDefinedConfigsAddedToBuilder() throws Exception {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage(containsString("configs must be defined."));

    Builder.of()
        .withLiquibaseConfigs(null);
  }

  @Test
  public void shouldThrowExceptionForConfigsWithNotDefinedElement() throws Exception {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage(containsString("config must be defined."));

    List<LiquibaseConfig> configs = Lists.newArrayList(
        LiquibaseConfig.Builder.of(new JDBCDataSource()).build(),
        null);
    Builder.of()
        .withLiquibaseConfigs(configs);
  }

  @Test
  public void shouldPassEqualsAndHashCodeContracts() throws Exception {
    EqualsVerifier.forClass(GuiceLiquibaseConfig.class)
        .usingGetClass()
        .verify();
  }

  @Test
  public void shouldPassEqualsAndHashCodeContractsForBuilder() throws Exception {
    EqualsVerifier.forClass(Builder.class)
        .usingGetClass()
        .verify();
  }

  @Test
  public void verifyToString() throws Exception {
    ToStringVerifier.forClass(GuiceLiquibaseConfig.class)
        .containsClassName(Builder.of().build());
  }
}