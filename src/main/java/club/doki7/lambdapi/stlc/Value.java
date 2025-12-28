package club.doki7.lambdapi.stlc;

import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

public sealed interface Value {
    record VLam(@NotNull Function<Value, Value> lam) implements Value {
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

    sealed interface VNeutral extends Value permits NFree, NApp {}

    record NFree(@NotNull Name name) implements VNeutral {
        @Override
        public @NotNull String toString() {
            return name.toString();
        }
    }

    record NApp(@NotNull VNeutral func, @NotNull Value arg) implements VNeutral {
        @Override
        public @NotNull String toString() {
            return "(" + func + " " + arg + ")";
        }
    }

    static @NotNull Value vFree(@NotNull Name name) {
        return new NFree(name);
    }
}
