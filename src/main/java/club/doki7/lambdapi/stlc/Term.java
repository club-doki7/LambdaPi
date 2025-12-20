package club.doki7.lambdapi.stlc;

import club.doki7.lambdapi.syntax.Node;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public sealed interface Term {
    @NotNull Node node();

    sealed interface Inferable extends Term permits Ann, Bound, Free, App {}
    sealed interface Checkable extends Term permits Inf, Lam {}

    record Ann(@NotNull Node node, @NotNull Checkable term, @NotNull Type annotation)
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
            throw new UnsupportedOperationException("Not supported yet");
        }
    }

    record Bound(@NotNull Node node, int index) implements Inferable {
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

    record Free(@NotNull Node node, @NotNull Name name) implements Inferable {
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
            return name.toString();
        }
    }

    record App(@NotNull Node node, @NotNull Inferable f, @NotNull Checkable arg)
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
            throw new UnsupportedOperationException("Not supported yet");
        }
    }

    record Inf(@NotNull Node node, @NotNull Inferable term) implements Checkable {
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

    record Lam(@NotNull Node node, @NotNull Checkable body) implements Checkable {
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
