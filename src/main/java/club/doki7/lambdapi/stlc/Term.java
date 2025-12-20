package club.doki7.lambdapi.stlc;

import org.jetbrains.annotations.NotNull;

public sealed interface Term {
    sealed interface Inferable extends Term permits Ann, Bound, Free, App {}
    sealed interface Checkable extends Term permits Inf, Lam {}

    record Ann(@NotNull Checkable term, @NotNull Type annotation) implements Inferable {}
    record Bound(int index) implements Inferable {}
    record Free(@NotNull Name name) implements Inferable {}
    record App(@NotNull Inferable f, @NotNull Checkable arg) implements Inferable {}

    record Inf(@NotNull Inferable term) implements Checkable {}
    record Lam(@NotNull Checkable body) implements Checkable {}
}
