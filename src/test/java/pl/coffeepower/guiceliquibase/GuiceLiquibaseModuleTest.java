package pl.coffeepower.guiceliquibase;

import com.google.inject.Guice;
import com.google.inject.Injector;

import org.junit.Test;

public class GuiceLiquibaseModuleTest {

    @Test
    public void name() throws Exception {
        Injector injector = Guice.createInjector(new GuiceLiquibaseModule());
        injector.getInstance(GuiceLiquibase.class);
    }
}