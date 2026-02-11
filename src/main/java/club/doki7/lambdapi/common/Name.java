package club.doki7.lambdapi.common;

import org.jetbrains.annotations.NotNull;

public sealed interface Name {
    record Global(@NotNull String name) implements Name {
        @Override
        public @NotNull String toString() {
            return name;
        }
    }

    record Local(int depth) implements Name {
        @Override
        public @NotNull String toString() {
            return DeBruijnIndex.superscriptNum('L', depth);
        }
    }

    record Quote(int depth) implements Name {
        @Override
        public @NotNull String toString() {
            return DeBruijnIndex.superscriptNum('Q', depth);
        }
    }
}
