package club.doki7.lambdapi.stlc;

import club.doki7.lambdapi.common.Name;
import club.doki7.lambdapi.exc.ElabException;
import club.doki7.lambdapi.syntax.Node;
import club.doki7.lambdapi.syntax.Token;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import static club.doki7.lambdapi.common.DeBruijnIndex.findInContext;

public final class Elab {
    public static @NotNull Term elab(@NotNull Node node) throws ElabException {
        return elabInferable(node, new ArrayList<>());
    }

    public static @NotNull Type elabType(@NotNull Node node) throws ElabException {
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
            case Node.Ann ann -> throw new ElabException(
                    ann.location(),
                    "In STLC, type annotation is not allowed at type/kind level"
            );
            case Node.App app -> throw new ElabException(
                    app.location(),
                    "In STLC, function application is not allowed at type/kind level"
            );
            case Node.Lam lam -> throw new ElabException(
                    lam.location(),
                    "In STLC, lambda expression is not allowed at type/kind level"
            );
            case Node.Aster aster -> throw new ElabException(
                    aster.location(),
                    "In STLC, type universes (*) are not supported, unless in axiom declarations"
            );
        };
    }

    private static Term.Inferable elabInferable(@NotNull Node node, @NotNull List<String> ctx)
            throws ElabException
    {
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
            case Node.App(Node func, List<Node> args) -> {
                Term.Inferable elabFunc = elabInferable(func, ctx);
                Term.Checkable elabArg;
                Term.App app;

                for (Node arg : args) {
                    elabArg = elabCheckable(arg, ctx);
                    app = new Term.App(node, elabFunc, elabArg);
                    elabFunc = app;
                }

                yield elabFunc;
            }
            case Node.Lam lam -> throw new ElabException(
                    lam.param(),
                    "In STLC, lambda expression must be annotated"
            );
            case Node.Pi pi -> {
                if (pi.param() != null) {
                    throw new ElabException(
                            pi.location(),
                            "STLC does not support dependent function types (Π/∀)"
                    );
                } else {
                    throw new ElabException(
                            pi.location(),
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

    private static Term.Checkable elabCheckable(@NotNull Node node, @NotNull List<String> ctx)
            throws ElabException
    {
        if (node instanceof Node.Lam(Token param, Node body)) {
            ctx.add(param.lexeme);
            Term.Checkable elabBody = elabCheckable(body, ctx);
            ctx.removeLast();
            return new Term.Lam(node, elabBody);
        }

        Term.Inferable inf = elabInferable(node, ctx);
        return new Term.Inf(node, inf);
    }
}
