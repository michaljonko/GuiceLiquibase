package pl.coffeepower.guiceliquibase;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.sameInstance;
import static org.hamcrest.collection.IsMapContaining.hasEntry;
import static org.junit.Assert.assertThat;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

import liquibase.resource.ResourceAccessor;
import liquibase.sdk.resource.MockResourceAccessor;

import nl.jqno.equalsverifier.EqualsVerifier;

import org.hsqldb.jdbc.JDBCDataSource;
import org.junit.Test;

import java.util.Collection;
import java.util.Map;

import javax.sql.DataSource;

public class LiquibaseConfigTest {

  @Test
  public void shouldCreateConfigByBuilder() throws Exception {
    LiquibaseConfig.Builder builder = LiquibaseConfig.Builder.of(Fixtures.DATA_SOURCE)
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
    Fixtures.PARAMETERS.forEach((k, v) -> assertThat(config.getParameters(), hasEntry(k, v)));
  }

  @Test
  public void shouldCreateDifferentBuilders() throws Exception {
    LiquibaseConfig.Builder builder = LiquibaseConfig.Builder.of(Fixtures.DATA_SOURCE)
        .withChangeLogPath(Fixtures.CHANGELOG_PATH)
        .withDropFirst(Fixtures.DROP_FIRST)
        .withResourceAccessor(Fixtures.RESOURCE_ACCESSOR);
    Fixtures.CONTEXT.forEach(builder::addContext);
    Fixtures.LABELS.forEach(builder::addLabel);
    Fixtures.PARAMETERS.forEach(builder::addParameter);

    assertThat(builder, not(sameInstance(LiquibaseConfig.Builder.of(builder))));
    assertThat(builder, is(LiquibaseConfig.Builder.of(builder)));
    assertThat(builder.hashCode(), is(LiquibaseConfig.Builder.of(builder).hashCode()));
    assertThat(builder, not(is(LiquibaseConfig.Builder.of(builder).addContext("X"))));
    assertThat(builder, is(LiquibaseConfig.Builder.of(builder).addContext("")));
    assertThat(builder, not(is(LiquibaseConfig.Builder.of(builder).addLabel("X"))));
    assertThat(builder, is(LiquibaseConfig.Builder.of(builder).addLabel("")));
    assertThat(builder, not(is(LiquibaseConfig.Builder.of(builder).addParameter("k", "v"))));
    assertThat(builder, is(LiquibaseConfig.Builder.of(builder).addParameter("", "")));
  }

  @Test
  public void shouldPassEqualsAndHashCodeContracts() throws Exception {
    EqualsVerifier.forClass(LiquibaseConfig.class)
        .usingGetClass()
        .verify();
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