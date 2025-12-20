package club.doki7.lambdapi.stlc;

import org.jetbrains.annotations.NotNull;

public sealed interface Type {
    record Free(@NotNull Name name) implements Type {}
    record Fun(@NotNull Type in, @NotNull Type out) implements Type {}
}
