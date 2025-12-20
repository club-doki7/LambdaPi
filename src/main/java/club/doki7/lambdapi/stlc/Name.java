package club.doki7.lambdapi.stlc;

import org.jetbrains.annotations.NotNull;

public sealed interface Name {
    record Global(@NotNull String name) implements Name {}

    record Local(int index) implements Name {}

    record Quote(int index) implements Name {}
}
