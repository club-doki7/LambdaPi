package club.doki7.lambdapi.stlc;

import club.doki7.lambdapi.syntax.Node;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public sealed interface Term {
    @Nullable Node node();

    sealed interface Inferable extends Term permits Ann, Bound, Free, App {}
    sealed interface Checkable extends Term permits Inf, Lam {}

    record Ann(@Nullable Node node, @NotNull Checkable term, @NotNull Type annotation)
            implements Inferable
    {
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Ann ann)) return false;

            return term.equals(ann.term) && annotation.equals(ann.annotation);
        }

        @Override
        public int hashCode() {
            return Objects.hash(term, annotation);
        }

        @Override
        public @NotNull String toString() {
            if (term instanceof Checkable.Inf(Node _, Inferable inf)) {
                if (inf instanceof Free || inf instanceof Bound) {
                    return term + " : " + annotation;
                }
            }

            return "(" + term + ") : " + annotation;
        }
    }

    record Bound(@Nullable Node node, int index) implements Inferable {
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Bound bound)) return false;

            return this.index == bound.index;
        }

        @Override
        public int hashCode() {
            return Objects.hash(Bound.class, index);
        }

        @Override
        public @NotNull String toString() {
            return Integer.toString(index);
        }
    }

    record Free(@Nullable Node node, @NotNull Name name) implements Inferable {
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Free free)) return false;

            return this.name.equals(free.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(Free.class, name);
        }

        @Override
        public @NotNull String toString() {
            return "Free(" + name + ")";
        }
    }

    record App(@Nullable Node node, @NotNull Inferable f, @NotNull Checkable arg)
            implements Inferable
    {
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof App app)) return false;

            return this.f.equals(app.f) && this.arg.equals(app.arg);
        }

        @Override
        public int hashCode() {
            return Objects.hash(f, arg);
        }

        @Override
        public @NotNull String toString() {
            StringBuilder sb = new StringBuilder();

            if (f instanceof Ann) {
                sb.append("(").append(f).append(")");
            } else {
                sb.append(f);
            }

            sb.append(" ");

            if (arg instanceof Lam
                || (arg instanceof Inf(Node _, Inferable inf)
                    && (inf instanceof App || inf instanceof Ann))) {
                sb.append("(").append(arg).append(")");
            } else {
                sb.append(arg);
            }

            return sb.toString();
        }
    }

    record Inf(@Nullable Node node, @NotNull Inferable term) implements Checkable {
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Inf inf)) return false;

            return this.term.equals(inf.term);
        }

        @Override
        public int hashCode() {
            return Objects.hash(Inf.class, term);
        }

        @Override
        public @NotNull String toString() {
            return term.toString();
        }
    }

    record Lam(@Nullable Node node, @NotNull Checkable body) implements Checkable {
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Lam lam)) return false;

            return this.body.equals(lam.body);
        }

        @Override
        public int hashCode() {
            return Objects.hash(Lam.class, body);
        }

        @Override
        public @NotNull String toString() {
            return "λ " + body;
        }
    }
}
