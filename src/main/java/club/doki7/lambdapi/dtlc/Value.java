package club.doki7.lambdapi.dtlc;

import club.doki7.lambdapi.common.Name;
import club.doki7.lambdapi.syntax.Node;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

public sealed interface Value {
    @NotNull Node node();

    record VLam(@NotNull Node node, @NotNull Function<Value, Value> lam) implements Value {
        @Override
        public @NotNull String toString() {
            return "VLam";
        }

        @Override
        public boolean equals(Object obj) {
            throw new UnsupportedOperationException(
                    "Cannot compare VLam values for equality. "
                    + "Note: use reify to retrieve a syntactic representation from HOAS structures."
            );
        }
    }

    record VStar(@NotNull Node node) implements Value {
        @Override
        public @NotNull String toString() {
            return "VStar";
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof VStar;
        }

        @Override
        public int hashCode() {
            return VStar.class.hashCode();
        }
    }

    record VPi(@NotNull Node node, @NotNull Value in, @NotNull Function<Value, Value> out)
            implements Value
    {
        @Override
        public @NotNull String toString() {
            return "VPi";
        }

        @Override
        public boolean equals(Object obj) {
            throw new UnsupportedOperationException(
                    "Cannot compare VPi values for equality. "
                    + "Note: use reify to retrieve a syntactic representation from HOAS structures."
            );
        }
    }

    sealed interface VNeutral extends Value permits NFree, NApp {}

    record NFree(@NotNull Node node, @NotNull Name name) implements VNeutral {
        @Override
        public @NotNull String toString() {
            return name.toString();
        }
    }

    record NApp(@NotNull Node node, @NotNull VNeutral func, @NotNull Value arg)
            implements VNeutral
    {
        @Override
        public @NotNull String toString() {
            return "(" + func + " " + arg + ")";
        }
    }
}
