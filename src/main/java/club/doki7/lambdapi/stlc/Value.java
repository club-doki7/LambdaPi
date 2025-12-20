package club.doki7.lambdapi.stlc;

import java.util.function.Function;

public sealed interface Value {
    record VLam(Function<Value, Value> lam) implements Value {}

    sealed interface VNeutral extends Value permits NFree, NApp {}

    record NFree(Name name) implements VNeutral {}

    record NApp(VNeutral func, Value arg) implements VNeutral {}

    static Value vFree(Name name) {
        return new NFree(name);
    }
}
