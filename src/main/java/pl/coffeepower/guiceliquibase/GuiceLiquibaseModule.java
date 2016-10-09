package pl.coffeepower.guiceliquibase;

import com.google.inject.AbstractModule;

import lombok.NonNull;

import javax.inject.Inject;

public final class GuiceLiquibaseModule extends AbstractModule {

    protected void configure() {
        bind(PostConstructListener.class).to(GuiceLiquibase.class).asEagerSingleton();
        requestInjection(this);
    }

    @Inject
    private void callPostConstruct(@NonNull PostConstructListener listener) {
        listener.postConstruct();
    }
}
