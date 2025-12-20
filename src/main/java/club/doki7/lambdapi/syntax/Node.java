package club.doki7.lambdapi.syntax;

import org.jetbrains.annotations.NotNull;

public sealed interface Node {
    record Ann(@NotNull Node term, @NotNull Node annotation) implements Node {}
    record Pi(@NotNull Token param, @NotNull Node paramType, @NotNull Node body) implements Node {}
    record Var(@NotNull Token name) implements Node {}
    record Lam(@NotNull Token param, @NotNull Node body) implements Node {}
    record App(@NotNull Node func, @NotNull Node arg) implements Node {}

    @NotNull Aster ASTER = Aster.INSTANCE;
    final class Aster implements Node {
        public static final @NotNull Aster INSTANCE = new Aster();

        private Aster() {}
    }
}
