package pl.coffeepower.guiceliquibase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.mockito.Mockito.mock;
import static pl.coffeepower.guiceliquibase.GuiceLiquibaseConfig.Builder;

import com.google.common.collect.Lists;

import com.jparams.verifier.tostring.NameStyle;
import com.jparams.verifier.tostring.ToStringVerifier;

import nl.jqno.equalsverifier.EqualsVerifier;

import org.hsqldb.jdbc.JDBCDataSource;
import org.junit.jupiter.api.Test;

import java.util.List;

import javax.sql.DataSource;

class GuiceLiquibaseConfigTest {

  @Test
  void shouldCreateEmptyConfigs() {
    GuiceLiquibaseConfig config = Builder.of()
        .build();

    assertThat(config)
        .isNotNull();
    assertThat(config.getConfigs())
        .isEmpty();
  }

  @Test
  void shouldCreateConfigForSingleLiquibaseConfig() {
    final LiquibaseConfig liquibaseConfig = LiquibaseConfig.Builder.of(mock(DataSource.class))
        .build();

    GuiceLiquibaseConfig config = Builder.of(liquibaseConfig)
        .build();

    assertThat(config)
        .isNotNull();
    assertThat(config.getConfigs())
        .singleElement()
        .isEqualTo(liquibaseConfig);
  }

  @Test
  public void shouldCreateConfigForMultipleLiquibaseConfigs() {
    final LiquibaseConfig firstLiquibaseConfig = LiquibaseConfig.Builder.of(mock(DataSource.class))
        .build();
    final LiquibaseConfig secondLiquibaseConfig = LiquibaseConfig.Builder.of(mock(DataSource.class))
        .build();
    final List<LiquibaseConfig> configs = Lists.newArrayList(firstLiquibaseConfig, secondLiquibaseConfig);

    GuiceLiquibaseConfig config = Builder.of()
        .withLiquibaseConfigs(configs)
        .build();

    assertThat(config)
        .isNotNull();
    assertThat(config.getConfigs())
        .containsExactlyInAnyOrder(firstLiquibaseConfig, secondLiquibaseConfig);
  }

  @Test
  void shouldThrowExceptionForNotDefinedConfig() {
    assertThatNullPointerException()
        .isThrownBy(() -> Builder.of(null))
        .withMessageContaining("config must be defined.");
  }

  @Test
  void shouldThrowExceptionForNotDefinedConfigAddedToBuilder() {
    assertThatNullPointerException()
        .isThrownBy(() -> Builder.of().withLiquibaseConfig(null))
        .withMessageContaining("config must be defined.");
  }

  @Test
  void shouldThrowExceptionForNotDefinedConfigsAddedToBuilder() {
    assertThatNullPointerException()
        .isThrownBy(() -> Builder.of().withLiquibaseConfigs(null))
        .withMessageContaining("configs must be defined.");
  }

  @Test
  void shouldThrowExceptionForConfigsWithNotDefinedElement() {
    assertThatNullPointerException()
        .isThrownBy(() -> Builder.of()
            .withLiquibaseConfigs(Lists.newArrayList(
                LiquibaseConfig.Builder.of(new JDBCDataSource()).build(), null)))
        .withMessageContaining("config must be defined.");
  }

  @Test
  void shouldPassEqualsAndHashCodeContracts() {
    EqualsVerifier.forClass(GuiceLiquibaseConfig.class)
        .usingGetClass()
        .verify();
  }

  @Test
  void shouldPassEqualsAndHashCodeContractsForBuilder() {
    EqualsVerifier.forClass(Builder.class)
        .usingGetClass()
        .verify();
  }

  @Test
  void verifyToString() {
    ToStringVerifier.forClass(GuiceLiquibaseConfig.class)
        .withClassName(NameStyle.SIMPLE_NAME).verify();
  }
}
