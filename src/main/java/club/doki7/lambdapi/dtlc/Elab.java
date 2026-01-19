package club.doki7.lambdapi.dtlc;

import club.doki7.lambdapi.common.Name;
import club.doki7.lambdapi.exc.ElabException;
import club.doki7.lambdapi.syntax.Node;
import club.doki7.lambdapi.syntax.Token;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static club.doki7.lambdapi.common.DeBruijnIndex.findInContext;

public final class Elab {
    public void registerTermFormer(Class<? extends Term.IDynTermBase<?>> clazz) {
        String name = "_Tf_" + clazz.getSimpleName();

        Constructor<?>[] constructors = clazz.getConstructors();
        loop: for (Constructor<?> ctor : constructors) {
            Class<?>[] paramTypes = ctor.getParameterTypes();
            if (paramTypes.length >= 1 && Node.class.isAssignableFrom(paramTypes[0])) {
                List<InferCheckKind> argsKind = new ArrayList<>();
                for (int i = 1; i < paramTypes.length; i++) {
                    if (Term.Inferable.class.isAssignableFrom(paramTypes[i])) {
                        argsKind.add(InferCheckKind.INFER);
                    } else if (Term.Checkable.class.isAssignableFrom(paramTypes[i])) {
                        argsKind.add(InferCheckKind.CHECK);
                    } else {
                        continue loop;
                    }
                }

                termFormers.put(name, new TermFormer(name, ctor, argsKind));
                break;
            }
        }
    }

    public @NotNull Term elab(@NotNull Node node) throws ElabException {
        return elabInferable(node, new ArrayList<>());
    }

    private Term.Inferable elabInferable(@NotNull Node node, @NotNull List<String> ctx)
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
                TermFormer former = termFormers.get(name.lexeme);
                if (former != null && former.argsKind.isEmpty()) {
                    yield elabTermFormer(node, former, List.of(), ctx);
                }

                int index = findInContext(name.lexeme, ctx);
                if (index == -1) {
                    yield new Term.Free(node, new Name.Global(name.lexeme));
                } else {
                    yield new Term.Bound(node, index);
                }
            }
            case Node.App(Node func, List<Node> args) -> {
                Term.Inferable elabFunc = elabInferable(func, ctx);
                if (elabFunc instanceof Term.Free(Node _, Name.Global(String name))) {
                    TermFormer former = termFormers.get(name);
                    if (former != null) {
                        if (args.size() < former.argsKind.size()) {
                            throw new ElabException(
                                    node.location(),
                                    "Term former '" + former.name + "' expects "
                                    + former.argsKind.size() + " argument(s), but got "
                                    + args.size() + "\n"
                                    + "Note: Term former does not support currying."
                            );
                        }

                        elabFunc = elabTermFormer(node, former, args, ctx);
                        args = args.subList(former.argsKind.size(), args.size());
                    }
                }

                if (args.isEmpty()) {
                    yield elabFunc;
                }

                Term.App app;
                for (Node arg : args) {
                    Term.Checkable elabArg = elabCheckable(arg, ctx);
                    app = new Term.App(node, elabFunc, elabArg);
                    elabFunc = app;
                }

                yield elabFunc;
            }
            case Node.Lam _ -> throw new ElabException(
                    node.location(),
                    "In DTLC, lambda expression must be annotated"
            );
        };
    }

    private Term.Checkable elabCheckable(@NotNull Node node, @NotNull List<String> ctx)
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

    private Term.Inferable elabTermFormer(
            @NotNull Node node,
            @NotNull TermFormer former,
            @NotNull List<Node> args,
            @NotNull List<String> ctx
    ) throws ElabException {
        if (args.size() < former.argsKind.size()) {
            throw new ElabException(
                    node.location(),
                    "Term former '" + former.name + "' expects "
                    + former.argsKind.size() + " arguments, but got " + args.size() + "\n"
                    + "Note: Term former does not support currying."
            );
        }

        Object[] ctorArgs = new Object[1 + former.argsKind.size()];
        ctorArgs[0] = node;
        for (int i = 0; i < former.argsKind.size(); i++) {
            Node argNode = args.get(i);
            switch (former.argsKind.get(i)) {
                case INFER -> ctorArgs[i + 1] = elabInferable(argNode, ctx);
                case CHECK -> ctorArgs[i + 1] = elabCheckable(argNode, ctx);
            }
        }

        try {
             return (Term.Inferable) former.ctor.newInstance(ctorArgs);
        } catch (Exception e) {
            ElabException elabE = new ElabException(
                    node.location(),
                    "Failed to construct term former '" + former.name + "': " + e.getMessage()
            );
            elabE.initCause(e);
            throw elabE;
        }
    }

    private enum InferCheckKind { INFER, CHECK }

    public record TermFormer(String name, Constructor<?> ctor, List<InferCheckKind> argsKind) {}

    public final HashMap<String, TermFormer> termFormers = new HashMap<>();
}
