package club.doki7.lambdapi.dtlc;

import org.jetbrains.annotations.NotNull;

public record Type(@NotNull Value value) {
    public static Type of(@NotNull Value value) {
        return new Type(value);
    }

    @Override
    public @NotNull String toString() {
        return value.toString();
    }
}
