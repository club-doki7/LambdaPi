package club.doki7.lambdapi.dtlc;

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

    private static Term.Inferable elabInferable(@NotNull Node node, @NotNull List<String> ctx)
            throws ElabException
    {
        return switch (node) {
            case Node.Ann(Node term, Node annotation) -> {
                Term.Checkable elabTerm = elabCheckable(term, ctx);
                Term.Checkable elabAnnotation = elabCheckable(annotation, ctx);
                yield new Term.Ann(node, elabTerm, elabAnnotation);
            }
            case Node.Aster _ -> new Term.Star(node);
            case Node.Pi(Token param, Node paramType, Node body) -> {
                Term.Checkable in = elabCheckable(paramType, ctx);
                if (param != null) {
                    ctx.add(param.lexeme);
                } else {
                    ctx.add("!anon");
                }
                Term.Checkable out = elabCheckable(body, ctx);
                ctx.removeLast();
                yield new Term.Pi(node, in, out);
            }
            case Node.Var(Token name) -> {
                int index = findInContext(name.lexeme, ctx);
                if (index == -1) {
                    yield new Term.Free(node, new Name.Global(name.lexeme));
                } else {
                    yield new Term.Bound(node, index);
                }
            }
            case Node.App(Node func, Node arg) -> {
                Term.Inferable elabFunc = elabInferable(func, ctx);
                Term.Checkable elabArg =  elabCheckable(arg,  ctx);
                yield new Term.App(node, elabFunc, elabArg);
            }
            case Node.Lam _ -> throw new ElabException(
                    node.location(),
                    "In DTLC, lambda expression must be annotated"
            );
        };
    }

    private static Term.Checkable elabCheckable(@NotNull Node node, @NotNull List<String> ctx)
            throws ElabException
    {
        if (node instanceof Node.Lam(Token param, Node body)) {
            List<String> newCtx = new ArrayList<>(ctx);
            newCtx.addLast(param.lexeme);
            Term.Checkable elabBody = elabCheckable(body, newCtx);
            return new Term.Lam(node, elabBody);
        }

        Term.Inferable inf = elabInferable(node, ctx);
        return new Term.Inf(node, inf);
    }
}
