package pl.coffeepower.guiceliquibase;

public class GuiceLiquibase implements PostConstructListener {

    @Override
    public void postConstruct() {
        System.out.println("called postConstruct");
    }
}
