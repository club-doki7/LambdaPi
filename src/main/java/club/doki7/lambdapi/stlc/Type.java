package club.doki7.lambdapi.stlc;

public sealed interface Type {
    record Free(Name name) implements Type {}
    record Fun(Type in, Type out) implements Type {}
}
