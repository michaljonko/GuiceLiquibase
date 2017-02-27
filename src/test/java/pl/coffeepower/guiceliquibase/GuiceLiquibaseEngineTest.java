package pl.coffeepower.guiceliquibase;

import static org.mockito.Mockito.verifyZeroInteractions;

import com.google.common.util.concurrent.Monitor;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Key;
import com.google.inject.ProvisionException;

import liquibase.configuration.GlobalConfiguration;
import liquibase.configuration.LiquibaseConfiguration;

import nl.jqno.equalsverifier.EqualsVerifier;

import org.hamcrest.Matchers;
import org.hsqldb.jdbc.JDBCDataSource;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import pl.coffeepower.guiceliquibase.annotation.GuiceLiquibase;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import javax.sql.DataSource;

import be.joengenduvel.java.verifiers.ToStringVerifier;

@RunWith(MockitoJUnitRunner.class)
public class GuiceLiquibaseEngineTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Mock
  private DataSource dataSource;

  @Test
  public void shouldNotExecuteEngineProcessWhenShouldRunIsDisabled() throws Exception {
    GuiceLiquibaseConfig config = GuiceLiquibaseConfig.Builder
        .of(LiquibaseConfig.Builder
            .of(dataSource)
            .build())
        .build();

    try {
      LiquibaseConfiguration.getInstance().getConfiguration(GlobalConfiguration.class)
          .setShouldRun(false);

      invokeEngineProcess(createGuiceLiquibaseEngine(config));

      verifyZeroInteractions(dataSource);
    } finally {
      LiquibaseConfiguration.getInstance().getConfiguration(GlobalConfiguration.class)
          .setShouldRun(true);
    }
  }

  @Test
  public void shouldThrowExceptionForEmptyConfigSet() throws Exception {
    expectedException.expect(ProvisionException.class);
    expectedException.expectCause(Matchers.instanceOf(IllegalArgumentException.class));
    expectedException.expectMessage(Matchers.containsString("Injected configuration set is empty."));

    createGuiceLiquibaseEngine(Fixtures.EMPTY_CONFIG_SET);
  }

  @Test
  public void shouldPassEqualsAndHashCodeContracts() throws Exception {
    EqualsVerifier.forClass(GuiceLiquibaseModule.GuiceLiquibaseEngine.class)
        .usingGetClass()
        .withPrefabValues(
            GuiceLiquibaseConfig.class,
            Fixtures.EMPTY_CONFIG_SET,
            Fixtures.SINGLETON_CONFIG_SET)
        .withPrefabValues(
            Monitor.class,
            new Monitor(true),
            new Monitor(false))
        .verify();
  }

  @Test
  public void verifyToString() throws Exception {

    ToStringVerifier.forClass(GuiceLiquibaseModule.GuiceLiquibaseEngine.class)
        .ignore("dataSource", "resourceAccessor")
        .containsClassName(createGuiceLiquibaseEngine(Fixtures.SINGLETON_CONFIG_SET));
  }

  private GuiceLiquibaseModule.GuiceLiquibaseEngine createGuiceLiquibaseEngine(
      final GuiceLiquibaseConfig config) {
    return Guice.createInjector(
        new AbstractModule() {
          @Override
          protected void configure() {
            bind(Key.get(GuiceLiquibaseConfig.class, GuiceLiquibase.class)).toInstance(config);
            bind(GuiceLiquibaseModule.GuiceLiquibaseEngine.class);
          }
        })
        .getInstance(GuiceLiquibaseModule.GuiceLiquibaseEngine.class);
  }

  private void invokeEngineProcess(GuiceLiquibaseModule.GuiceLiquibaseEngine engine) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
    Method processMethod =
        GuiceLiquibaseModule.GuiceLiquibaseEngine.class.getDeclaredMethod("process");
    processMethod.setAccessible(true);
    processMethod.invoke(engine);
  }

  private static final class Fixtures {

    private static final GuiceLiquibaseConfig EMPTY_CONFIG_SET =
        GuiceLiquibaseConfig.Builder.of().build();
    private static final GuiceLiquibaseConfig SINGLETON_CONFIG_SET =
        GuiceLiquibaseConfig.Builder
            .of(LiquibaseConfig.Builder.of(new JDBCDataSource()).build())
            .build();
  }
}