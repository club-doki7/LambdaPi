package club.doki7.lambdapi.syntax;

public sealed interface Node {
    record Ann(Node term, Node annotation) implements Node {}
    record Pi(Token param, Node paramType, Node body) implements Node {}
    record Var(Token name) implements Node {}
    record Lam(Token param, Node body) implements Node {}
    record App(Node func, Node arg) implements Node {}

    final class Aster implements Node {
        public static final Aster INSTANCE = new Aster();

        private Aster() {}
    }
}
