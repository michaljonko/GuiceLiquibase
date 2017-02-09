package pl.coffeepower.guiceliquibase;

import nl.jqno.equalsverifier.EqualsVerifier;

import org.junit.Test;

public class LiquibaseConfigTest {

  @Test
  public void shouldPassEqualsAndHashCodeContracts() throws Exception {
    EqualsVerifier.forClass(LiquibaseConfig.class)
        .usingGetClass()
        .verify();
  }

}