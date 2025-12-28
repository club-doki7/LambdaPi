package club.doki7.lambdapi.stlc;

import club.doki7.lambdapi.exc.ElabException;
import club.doki7.lambdapi.syntax.Node;
import club.doki7.lambdapi.syntax.Token;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class Elab {
    public static @NotNull Term elab(@NotNull Node node) throws ElabException {
        return elabInferable(node, new ArrayList<>());
    }

    private static Term.Inferable elabInferable(Node node, List<String> ctx) throws ElabException {
        return switch (node) {
            case Node.Ann(Node term, Node annotation) -> {
                Term.Checkable elabTerm = elabCheckable(term, ctx);
                Type elabType = elabType(annotation);
                yield new Term.Ann(node, elabTerm, elabType);
            }
            case Node.Var(Token name) -> {
                int index = findInContext(name.lexeme, ctx);
                if (index >= 0) {
                    yield new Term.Bound(node, index);
                } else {
                    yield new Term.Free(node, new Name.Global(name.lexeme));
                }
            }
            case Node.App(Node func, Node arg) -> {
                Term.Inferable elabFunc = elabInferable(func, ctx);
                Term.Checkable elabArg = elabCheckable(arg, ctx);
                yield new Term.App(node, elabFunc, elabArg);
            }
            case Node.Lam lam -> throw new ElabException(
                    lam.param(),
                    "In STLC, lambda expression must be annotated"
            );
            case Node.Pi pi -> {
                if (pi.param() != null) {
                    throw new ElabException(
                            pi.param(),
                            "STLC does not support dependent function types (Π/∀)"
                    );
                } else {
                    Token location = extractToken(pi.paramType());
                    throw new ElabException(
                            location,
                            "In STLC, function arrow is not allowed at term level"
                    );
                }
            }
            case Node.Aster(Token aster) -> throw new ElabException(
                    aster,
                    "STLC does not support type universes (*)"
            );
        };
    }

    private static Term.Checkable elabCheckable(Node node, List<String> ctx) throws ElabException {
        if (Objects.requireNonNull(node) instanceof Node.Lam(Token param, Node body)) {
            List<String> newCtx = new ArrayList<>(ctx);
            newCtx.addLast(param.lexeme);
            Term.Checkable elabBody = elabCheckable(body, newCtx);
            return new Term.Lam(node, elabBody);
        }

        Term.Inferable inf = elabInferable(node, ctx);
        return new Term.Inf(node, inf);
    }

    private static Type elabType(Node node) throws ElabException {
        return switch (node) {
            case Node.Var(Token name) -> new Type.Free(new Name.Global(name.lexeme));
            case Node.Pi(Token param, Node paramType, Node body) -> {
                if (param != null) {
                    throw new ElabException(
                            param,
                            "STLC does not support dependent function types (Π/∀)"
                    );
                }
                Type in = elabType(paramType);
                Type out = elabType(body);
                yield new Type.Fun(in, out);
            }
            case Node.Ann ann -> {
                Token location = extractToken(ann.term());
                throw new ElabException(
                        location,
                        "In STLC, type annotation is not allowed at type/kind level"
                );
            }
            case Node.App app -> {
                Token location = extractToken(app.func());
                throw new ElabException(
                        location,
                        "In STLC, function application is not allowed at type/kind level"
                );
            }
            case Node.Lam lam -> throw new ElabException(
                    lam.param(),
                    "In STLC, lambda expression is not allowed at type/kind level"
            );
            case Node.Aster(Token aster) -> throw new ElabException(
                    aster,
                    "In STLC, type universes (*) are not supported, unless in axiom declarations"
            );
        };
    }

    private static int findInContext(@NotNull String name, @NotNull List<String> ctx) {
        for (int i = ctx.size() - 1; i >= 0; i--) {
            if (ctx.get(i).equals(name)) {
                return ctx.size() - 1 - i;
            }
        }
        return -1;
    }

    private static Token extractToken(Node node) {
        return switch (node) {
            case Node.Ann ann -> extractToken(ann.term());
            case Node.Aster(Token aster) -> aster;
            case Node.Pi pi -> pi.param() != null ? pi.param() : extractToken(pi.paramType());
            case Node.Var(Token name) -> name;
            case Node.App app -> extractToken(app.func());
            case Node.Lam(Token param, Node _) -> param;
        };
    }
}
