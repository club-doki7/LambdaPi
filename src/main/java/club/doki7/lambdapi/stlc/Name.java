package club.doki7.lambdapi.stlc;

import org.jetbrains.annotations.NotNull;

public sealed interface Name {
    record Global(@NotNull String name) implements Name {
        @Override
        public @NotNull String toString() {
            return name;
        }
    }

    record Local(int index) implements Name {
        @Override
        public @NotNull String toString() {
            return "L" + index;
        }
    }

    record Quote(int index) implements Name {
        @Override
        public @NotNull String toString() {
            return "Q" + index;
        }
    }
}
