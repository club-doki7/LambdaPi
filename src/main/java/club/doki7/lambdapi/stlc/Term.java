package club.doki7.lambdapi.stlc;

public sealed interface Term {
    sealed interface Inferable extends Term permits Ann, Bound, Free, App {}
    sealed interface Checkable extends Term permits Inf, Lam {}

    record Ann(Checkable term, Type annotation) implements Inferable {}
    record Bound(int index) implements Inferable {}
    record Free(Name name) implements Inferable {}
    record App(Inferable f, Checkable arg) implements Inferable {}

    record Inf(Inferable term) implements Checkable {}
    record Lam(Checkable body) implements Checkable {}
}
