package club.doki7.lambdapi.dtlc;

import org.jetbrains.annotations.NotNull;

// TODO: make use of this nominal alias
public record Type(@NotNull Value value) {
    @Override
    public @NotNull String toString() {
        return value.toString();
    }
}
