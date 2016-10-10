package pl.coffeepower.guiceliquibase;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

import java.util.Objects;

public class GuiceLiquibaseConfig {

    private final String changeLogPath;

    private GuiceLiquibaseConfig(String changeLogPath) {
        this.changeLogPath = changeLogPath;
    }

    public String getChangeLogPath() {
        return changeLogPath;
    }

    @Override
    public int hashCode() {
        return Objects.hash(changeLogPath);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GuiceLiquibaseConfig that = (GuiceLiquibaseConfig) o;
        return Objects.equals(changeLogPath, that.changeLogPath);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("GuiceLiquibaseConfig{");
        sb.append("changeLogPath='").append(changeLogPath).append('\'');
        sb.append('}');
        return sb.toString();
    }

    public static final class Builder {

        private String changeLogPath = "liquibase/changeLog.xml";

        private Builder() {
        }

        public static Builder aConfig() {
            return new Builder();
        }

        public void withChangeLog(String changeLog) {
            this.changeLogPath = changeLog;
        }

        public GuiceLiquibaseConfig build() {
            Preconditions.checkArgument(
                    !Strings.isNullOrEmpty(changeLogPath), "changeLogPath cannot be empty.");
            return new GuiceLiquibaseConfig(changeLogPath);
        }

        @Override
        public int hashCode() {
            return Objects.hash(changeLogPath);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Builder builder = (Builder) o;
            return Objects.equals(changeLogPath, builder.changeLogPath);
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("Builder{");
            sb.append("changeLogPath='").append(changeLogPath).append('\'');
            sb.append('}');
            return sb.toString();
        }
    }
}
