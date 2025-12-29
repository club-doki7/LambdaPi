package club.doki7.lambdapi.stlc;

import club.doki7.lambdapi.common.Name;
import org.jetbrains.annotations.NotNull;

public sealed interface Type {
    record Free(@NotNull Name name) implements Type {
        @Override
        public @NotNull String toString() {
            return name.toString();
        }
    }

    record Fun(@NotNull Type in, @NotNull Type out) implements Type {
        @Override
        public @NotNull String toString() {
            if (in instanceof Fun) {
                return "(" + in + ") -> " + out;
            } else {
                return in + " -> " + out;
            }
        }
    }
}
