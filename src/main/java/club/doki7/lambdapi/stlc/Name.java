package club.doki7.lambdapi.stlc;

public sealed interface Name {
    record Global(String name) implements Name {}

    record Local(int index) implements Name {}

    record Quote(int index) implements Name {}
}
