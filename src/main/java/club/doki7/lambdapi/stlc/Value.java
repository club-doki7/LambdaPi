package club.doki7.lambdapi.stlc;

import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

public sealed interface Value {
    record VLam(@NotNull Function<Value, Value> lam) implements Value {}

    sealed interface VNeutral extends Value permits NFree, NApp {}

    record NFree(@NotNull Name name) implements VNeutral {}

    record NApp(@NotNull VNeutral func, @NotNull Value arg) implements VNeutral {}

    static @NotNull Value vFree(@NotNull Name name) {
        return new NFree(name);
    }
}
