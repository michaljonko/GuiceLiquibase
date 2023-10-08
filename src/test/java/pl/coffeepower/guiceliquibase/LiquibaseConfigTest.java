package pl.coffeepower.guiceliquibase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.mockito.Mockito.mock;
import static pl.coffeepower.guiceliquibase.LiquibaseConfig.Builder;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.jparams.verifier.tostring.NameStyle;
import com.jparams.verifier.tostring.ToStringVerifier;
import java.util.Collection;
import java.util.Map;
import javax.sql.DataSource;
import liquibase.resource.ResourceAccessor;
import liquibase.sdk.resource.MockResourceAccessor;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.hsqldb.jdbc.JDBCDataSource;
import org.junit.jupiter.api.Test;

class LiquibaseConfigTest {

  @Test
  void shouldCreateConfigByBuilder() {
    Builder builder = Builder.of(Fixtures.DATA_SOURCE)
        .withChangeLogPath(Fixtures.CHANGELOG_PATH)
        .withDropFirst(Fixtures.DROP_FIRST)
        .withShouldRun(Fixtures.SHOULD_RUN)
        .withResourceAccessor(Fixtures.RESOURCE_ACCESSOR)
        .withContexts(Fixtures.CONTEXT)
        .withLabels(Fixtures.LABELS)
        .withParameters(Fixtures.PARAMETERS);

    LiquibaseConfig config = builder.build();

    assertThat(config)
        .isNotSameAs(builder.build())
        .isEqualTo(builder.build());
    assertThat(config.getDataSource())
        .isEqualTo(Fixtures.DATA_SOURCE);
    assertThat(config.getChangeLogPath())
        .isEqualTo(Fixtures.CHANGELOG_PATH);
    assertThat(config.getResourceAccessor())
        .isEqualTo(Fixtures.RESOURCE_ACCESSOR);
    assertThat(config.getContexts())
        .containsExactlyInAnyOrderElementsOf(Fixtures.CONTEXT);
    assertThat(config.getLabels())
        .containsExactlyInAnyOrderElementsOf(Fixtures.LABELS);
    assertThat(config.getParameters())
        .containsExactlyEntriesOf(Fixtures.PARAMETERS);
  }

  @Test
  void shouldCreateDifferentBuilders() {
    Builder builder = Builder.of(Fixtures.DATA_SOURCE)
        .withChangeLogPath(Fixtures.CHANGELOG_PATH)
        .withDropFirst(Fixtures.DROP_FIRST)
        .withShouldRun(Fixtures.SHOULD_RUN)
        .withResourceAccessor(Fixtures.RESOURCE_ACCESSOR)
        .withContexts(Fixtures.CONTEXT)
        .withLabels(Fixtures.LABELS)
        .withParameters(Fixtures.PARAMETERS);

    assertThat(builder)
        .isEqualTo(Builder.of(builder));
    assertThat(builder)
        .isNotSameAs(Builder.of(builder));
    assertThat(builder)
        .isEqualTo(Builder.of(builder).withContext(""));
    assertThat(builder)
        .isNotEqualTo(Builder.of(builder).withContext("X"));
    assertThat(builder).isEqualTo(Builder.of(builder).withLabel(""));
    assertThat(builder)
        .isNotEqualTo(Builder.of(builder).withLabel("X"));
    assertThat(builder)
        .isEqualTo(Builder.of(builder).withParameter("", ""));
    assertThat(builder)
        .isNotEqualTo(Builder.of(builder).withParameter("k", "v"));
  }

  @Test
  void shouldThrowExceptionForBuilderWithEmptyChangeLogPath() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> Builder.of(Fixtures.DATA_SOURCE)
            .withChangeLogPath("")
            .build())
        .withMessageContaining("changeLogPath must be defined.");
  }

  @Test
  void shouldThrowExceptionForBuilderWithNotDefinedChangeLogPath() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> Builder.of(Fixtures.DATA_SOURCE)
            .withChangeLogPath(null)
            .build())
        .withMessageContaining("changeLogPath must be defined.");
  }

  @Test
  void shouldThrowExceptionForBuilderWithNotDefinedResourceAccessor() {
    assertThatNullPointerException()
        .isThrownBy(() -> Builder.of(Fixtures.DATA_SOURCE)
            .withResourceAccessor(null)
            .build())
        .withMessageContaining("resourceAccessor must be defined.");
  }

  @Test
  void shouldPassEqualsAndHashCodeContracts() {
    EqualsVerifier.forClass(LiquibaseConfig.class)
        .usingGetClass()
        .verify();
  }

  @Test
  void shouldPassEqualsAndHashCodeContractsForBuilder() {
    Builder builder = Builder.of(Fixtures.DATA_SOURCE);
    Builder builderClone = Builder.of(builder)
        .withChangeLogPath(Fixtures.CHANGELOG_PATH);

    assertThat(builder.hashCode())
        .isEqualTo(builderClone.hashCode());
    assertThat(builder)
        .isNotEqualTo(builderClone);
  }

  @Test
  void shouldPassHashCodeForBuilder() {
    DataSource firstDataSource = mock(DataSource.class);
    DataSource secondDataSource = mock(DataSource.class);

    assertThat(Builder.of(firstDataSource).hashCode())
        .isNotEqualTo(Builder.of(secondDataSource).hashCode());
  }

  @Test
  void verifyToString() throws Exception {
    ToStringVerifier.forClass(LiquibaseConfig.class).withClassName(NameStyle.SIMPLE_NAME)
        .withIgnoredFields("dataSource", "resourceAccessor").verify();
  }

  private static final class Fixtures {

    private static final DataSource DATA_SOURCE = new JDBCDataSource();
    private static final String CHANGELOG_PATH = "changelog/path";
    private static final boolean DROP_FIRST = true;
    private static final boolean SHOULD_RUN = true;
    private static final ResourceAccessor RESOURCE_ACCESSOR = new MockResourceAccessor();
    private static final Collection<String> CONTEXT = Lists.newArrayList("context1", "context2");
    private static final Collection<String> LABELS = Lists.newArrayList("label1", "label2");
    private static final Map<String, String> PARAMETERS = ImmutableMap.of("k1", "v1", "k2", "v2");
  }
}
