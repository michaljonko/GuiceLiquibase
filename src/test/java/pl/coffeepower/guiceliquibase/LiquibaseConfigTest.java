package pl.coffeepower.guiceliquibase;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.sameInstance;
import static org.hamcrest.collection.IsMapContaining.hasEntry;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static pl.coffeepower.guiceliquibase.LiquibaseConfig.Builder;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

import liquibase.resource.ResourceAccessor;
import liquibase.sdk.resource.MockResourceAccessor;

import nl.jqno.equalsverifier.EqualsVerifier;

import org.hsqldb.jdbc.JDBCDataSource;
import org.hsqldb.jdbc.JDBCPool;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Answers;
import org.mockito.MockSettings;
import org.mockito.Mockito;

import java.util.Collection;
import java.util.Map;

import javax.sql.DataSource;

import be.joengenduvel.java.verifiers.ToStringVerifier;

public class LiquibaseConfigTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void shouldCreateConfigByBuilder() throws Exception {
    Builder builder = Builder.of(Fixtures.DATA_SOURCE)
        .withChangeLogPath(Fixtures.CHANGELOG_PATH)
        .withDropFirst(Fixtures.DROP_FIRST)
        .withResourceAccessor(Fixtures.RESOURCE_ACCESSOR);
    Fixtures.CONTEXT.forEach(builder::addContext);
    Fixtures.LABELS.forEach(builder::addLabel);
    Fixtures.PARAMETERS.forEach(builder::addParameter);

    LiquibaseConfig config = builder.build();

    assertThat(config, not(sameInstance(builder.build())));
    assertThat(config, is(builder.build()));
    assertThat(config.getDataSource(), is(Fixtures.DATA_SOURCE));
    assertThat(config.getChangeLogPath(), is(Fixtures.CHANGELOG_PATH));
    assertThat(config.getResourceAccessor(), is(Fixtures.RESOURCE_ACCESSOR));
    assertThat(config.getContexts(), hasSize(Fixtures.CONTEXT.size()));
    assertThat(config.getContexts(), containsInAnyOrder(Fixtures.CONTEXT.toArray(new String[0])));
    assertThat(config.getLabels(), hasSize(Fixtures.LABELS.size()));
    assertThat(config.getLabels(), containsInAnyOrder(Fixtures.LABELS.toArray(new String[0])));
    assertThat(config.getParameters().size(), is(Fixtures.PARAMETERS.size()));
    Fixtures.PARAMETERS.forEach(
        (key, value) -> assertThat(config.getParameters(), hasEntry(key, value)));
  }

  @Test
  public void shouldCreateDifferentBuilders() throws Exception {
    Builder builder = Builder.of(Fixtures.DATA_SOURCE)
        .withChangeLogPath(Fixtures.CHANGELOG_PATH)
        .withDropFirst(Fixtures.DROP_FIRST)
        .withResourceAccessor(Fixtures.RESOURCE_ACCESSOR);
    Fixtures.CONTEXT.forEach(builder::addContext);
    Fixtures.LABELS.forEach(builder::addLabel);
    Fixtures.PARAMETERS.forEach(builder::addParameter);

    assertThat(builder, is(Builder.of(builder)));
    assertThat(builder, not(sameInstance(Builder.of(builder))));
    assertThat(builder, is(Builder.of(builder).addContext("")));
    assertThat(builder, not(is(Builder.of(builder).addContext("X"))));
    assertThat(builder, is(Builder.of(builder).addLabel("")));
    assertThat(builder, not(is(Builder.of(builder).addLabel("X"))));
    assertThat(builder, is(Builder.of(builder).addParameter("", "")));
    assertThat(builder, not(is(Builder.of(builder).addParameter("k", "v"))));
  }

  @Test
  public void shouldThrowExceptionForBuilderWithEmptyChangeLogPath() throws Exception {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage(containsString("changeLogPath must be defined."));

    Builder.of(Fixtures.DATA_SOURCE)
        .withChangeLogPath("")
        .build();
  }

  @Test
  public void shouldThrowExceptionForBuilderWithNotDefinedChangeLogPath() throws Exception {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage(containsString("changeLogPath must be defined."));

    Builder.of(Fixtures.DATA_SOURCE)
        .withChangeLogPath(null)
        .build();
  }

  @Test
  public void shouldThrowExceptionForBuilderWithNotDefinedResourceAccessor() throws Exception {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage(containsString("resourceAccessor must be defined."));

    Builder.of(Fixtures.DATA_SOURCE)
        .withResourceAccessor(null)
        .build();
  }

  @Test
  public void shouldPassEqualsAndHashCodeContracts() throws Exception {
    EqualsVerifier.forClass(LiquibaseConfig.class)
        .usingGetClass()
        .verify();
  }

  @Test
  public void shouldPassEqualsAndHashCodeContractsForBuilder() throws Exception {
    Builder builder = Builder.of(Fixtures.DATA_SOURCE);
    Builder builderClone = Builder.of(builder).withChangeLogPath(Fixtures.CHANGELOG_PATH);

    assertThat(builder.hashCode(), is(builder.hashCode()));
    assertThat(builder, is(builder));
    assertThat(builder.hashCode(), is(builderClone.hashCode()));
    assertThat(builder.hashCode(), not(is(Builder.of(new JDBCPool()))));
    assertThat(builder, not(is(builderClone)));
    assertThat(builder.equals(null), is(false));
  }

  @Test
  public void shouldPassHashCodeForBuilder() throws Exception {
    DataSource firstDataSource = mock(DataSource.class);
    DataSource secondDataSource = mock(DataSource.class);

    assertThat(Builder.of(firstDataSource).hashCode(),
        not(is(Builder.of(secondDataSource).hashCode())));
    assertThat(Builder.of(firstDataSource).hashCode(), is(Builder.of(firstDataSource).hashCode()));
  }

  @Test
  public void verifyToString() throws Exception {
    ToStringVerifier.forClass(LiquibaseConfig.class)
        .ignore("dataSource", "resourceAccessor")
        .containsClassName(Builder.of(Fixtures.DATA_SOURCE).build());
  }

  private static final class Fixtures {

    private static final DataSource DATA_SOURCE = new JDBCDataSource();
    private static final String CHANGELOG_PATH = "changelog/path";
    private static final boolean DROP_FIRST = true;
    private static final ResourceAccessor RESOURCE_ACCESSOR = new MockResourceAccessor();
    private static final Collection<String> CONTEXT = Lists.newArrayList("context1", "context2");
    private static final Collection<String> LABELS = Lists.newArrayList("label1", "label2");
    private static final Map<String, String> PARAMETERS = ImmutableMap.of("k1", "v1", "k2", "v2");
  }
}