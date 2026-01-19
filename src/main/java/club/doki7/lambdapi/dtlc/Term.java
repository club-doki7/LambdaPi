package club.doki7.lambdapi.dtlc;

import club.doki7.lambdapi.common.Name;
import club.doki7.lambdapi.exc.TypeCheckException;
import club.doki7.lambdapi.syntax.Node;
import club.doki7.lambdapi.util.ConsList;
import club.doki7.lambdapi.util.Pair;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Objects;

public sealed interface Term {
    @NotNull Node node();

    sealed interface Inferable extends Term permits Ann, Star, Pi, Bound, Free, App, InferableTF {}
    sealed interface Checkable extends Term permits Inf, Lam, CheckableTF {}

    record Ann(@NotNull Node node, @NotNull Checkable term, @NotNull Checkable annotation)
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
                if (inf instanceof Star || inf instanceof Free || inf instanceof Bound) {
                    return term + " : " + annotation;
                }
            }

            return "(" + term + ") : " + annotation;
        }
    }

    record Star(@NotNull Node node) implements Inferable {
        @Override
        public boolean equals(Object o) {
            return o instanceof Star;
        }

        @Override
        public int hashCode() {
            return Star.class.hashCode();
        }

        @Override
        public @NotNull String toString() {
            return "*";
        }
    }

    record Pi(@NotNull Node node, @NotNull Checkable paramType, @NotNull Checkable bodyType)
            implements Inferable
    {
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Pi pi)) return false;

            return paramType.equals(pi.paramType) && bodyType.equals(pi.bodyType);
        }

        @Override
        public int hashCode() {
            return Objects.hash(paramType, bodyType);
        }

        @Override
        public @NotNull String toString() {
            if (paramType instanceof Term.Inf(Node _, Term.Pi _)) {
                return "∀ (" + paramType + ") → " + bodyType;
            }
            return "∀ " + paramType + " → " + bodyType;
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

    record Inf(@NotNull Node node, @NotNull Inferable inferable) implements Checkable {
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Inf inf)) return false;

            return this.inferable.equals(inf.inferable);
        }

        @Override
        public int hashCode() {
            return Objects.hash(inferable);
        }

        @Override
        public @NotNull String toString() {
            return inferable.toString();
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
            return Objects.hash(body);
        }

        @Override
        public @NotNull String toString() {
            return "λ " + body;
        }
    }

    interface ITermFormer<T> {
        Value eval(ConsList<Value> env, Map<String, Value> globals);

        T subst(int depth, Term.Free r);
    }

    non-sealed interface CheckableTF extends Term.Checkable, ITermFormer<CheckableTF> {
        void check(int depth,
                   ConsList<Pair<Name.Local, Type>> ctx,
                   Globals globals,
                   Type expected) throws TypeCheckException;
    }

    non-sealed interface InferableTF extends Term.Inferable, ITermFormer<InferableTF> {
        Type infer(int depth,
                   ConsList<Pair<Name.Local, Type>> ctx,
                   Globals globals) throws TypeCheckException;
    }
}
