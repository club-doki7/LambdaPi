package club.doki7.lambdapi.dtlc;

import club.doki7.lambdapi.common.Name;
import club.doki7.lambdapi.exc.TypeCheckException;
import club.doki7.lambdapi.syntax.Node;
import club.doki7.lambdapi.util.ConsList;
import club.doki7.lambdapi.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.function.Function;

public final class InferCheck {
    // TODO this `globals` is erroneous because it's trying to represent both env and type context
    public static @NotNull Value infer(Term.Inferable inferable, Map<String, Value> globals)
        throws TypeCheckException
    {
        return infer(0, ConsList.nil(), globals, inferable);
    }

    private static @NotNull Value infer(
            int depth,
            ConsList<Pair<Name.Local, Value>> ctx,
            Map<String, Value> globals,
            Term.Inferable inferable
    ) throws TypeCheckException {
        return switch (inferable) {
            case Term.Ann(Node _, Term.Checkable checkable, Term.Checkable annotation) -> {
                // TODO check annotation to have type *
                Value annotationEval = Eval.eval(annotation, globals);
                check(depth, ctx, globals, checkable, annotationEval);
                yield annotationEval;
            }
            case Term.Free(Node node, Name name) -> {
                @Nullable Pair<Name.Local, Value> entry = ctx.findFirst(p -> p.first().equals(name));
                if (entry != null) {
                    yield entry.second();
                }

                if (name instanceof Name.Global(String strName)) {
                    @Nullable Value type = globals.get(strName);
                    if (type != null) {
                        yield type;
                    }
                }

                throw new TypeCheckException(
                        node.location(),
                        "Undefined variable identifier " + name
                );
            }
            case Term.Star(Node node) -> new Value.VStar(node);
            case Term.App(Node node, Term.Inferable f, Term.Checkable arg) -> {
                Value fType = infer(depth, ctx, globals, f);
                if (!(fType instanceof Value.VPi(Node _, Value in, Function<Value, Value> out))) {
                    throw new TypeCheckException(
                            node.location(),
                            "Expected function type in application"
                    );
                }
                check(depth, ctx, globals, arg, in);
                yield out.apply(Eval.eval(arg, globals));
            }
            case Term.Pi(Node node, Term.Checkable in, Term.Checkable out) -> {
                Value vStar = new Value.VStar(node);
                check(depth, ctx, globals, in, vStar);
                Value inEval = Eval.eval(in, globals);
                Name.Local local = new Name.Local(depth);
                check(
                        depth + 1,
                        ConsList.cons(new Pair<>(local, inEval), ctx),
                        globals,
                        subst(0, new Term.Free(node, local), out),
                        vStar
                );
                yield vStar;
            }
            case Term.Bound _ -> throw new IllegalStateException(
                    "Unexpected bound variable in type checking phase"
            );
        };
    }

    private static void check(
            int depth,
            ConsList<Pair<Name.Local, Value>> ctx,
            Map<String, Value> globals,
            Term.Checkable checkable,
            Value expected
    ) throws TypeCheckException {
        switch (checkable) {
            case Term.Inf(Node node, Term.Inferable inferable) -> {
                Value inferred = infer(depth, ctx, globals, inferable);
                Term inferredReadback = Eval.reify(inferred);
                Term expectedReadback = Eval.reify(expected);
                if (!inferredReadback.equals(expectedReadback)) {
                    throw new TypeCheckException(
                            node.location(),
                            "Type mismatch, expected " + expectedReadback
                            + ", inferred " + inferredReadback
                    );
                }
            }
            case Term.Lam(Node node, Term.Checkable body) -> {
                if (!(expected instanceof Value.VPi(Node _,
                                                    Value in,
                                                    Function<Value, Value> out))) {
                    throw new TypeCheckException(
                            node.location(),
                            "Lambda terms can be only checked as function type, got " + expected
                    );
                }

                Name.Local local = new Name.Local(depth);
                check(
                        depth + 1,
                        ConsList.cons(new Pair<>(local, in), ctx),
                        globals,
                        subst(0, new Term.Free(node, local), body),
                        out.apply(Value.vFree(node, local))
                );
            }
        }
    }

    private static Term.Inferable subst(int depth, Term.Free r, Term.Inferable inferable) {
        return switch (inferable) {
            case Term.Ann(Node node, Term.Checkable term, Term.Checkable ann) -> new Term.Ann(
                    node,
                    subst(depth, r, term),
                    subst(depth, r, ann)
            );
            case Term.Bound bound -> bound.index() == depth ? r : bound;
            case Term.Free free -> free;
            case Term.App(Node node, Term.Inferable f, Term.Checkable arg) -> new Term.App(
                    node,
                    subst(depth, r, f),
                    subst(depth, r, arg)
            );
            case Term.Star star -> star;
            case Term.Pi(Node node, Term.Checkable in, Term.Checkable out) -> new Term.Pi(
                    node,
                    subst(depth, r, in),
                    subst(depth + 1, r, out)
            );
        };
    }

    private static Term.Checkable subst(int depth, Term.Free r, Term.Checkable checkable) {
        return switch (checkable) {
            case Term.Inf(Node node, Term.Inferable term) -> new Term.Inf(
                    node,
                    subst(depth, r, term)
            );
            case Term.Lam(Node node, Term.Checkable body) -> new Term.Lam(
                    node,
                    subst(depth + 1, r, body)
            );
        };
    }
}
