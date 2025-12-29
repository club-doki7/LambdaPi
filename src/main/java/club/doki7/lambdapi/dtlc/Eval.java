package club.doki7.lambdapi.dtlc;

import club.doki7.lambdapi.common.Name;
import club.doki7.lambdapi.syntax.Node;
import club.doki7.lambdapi.util.ConsList;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.function.Function;

public final class Eval {
    public static Value eval(Term term, Map<String, Value> globals) {
        return eval(term, ConsList.nil(), Map.copyOf(globals));
    }

    private static Value eval(Term term, ConsList<Value> env, Map<String, Value> globals) {
        return switch (term) {
            case Term.Ann(Node _, Term e, Term _) -> eval(e, env, globals);
            case Term.Free(Node node, Name name) -> {
                if (name instanceof Name.Global(String strName)) {
                    Value val = globals.get(strName);
                    if (val != null) {
                        yield val;
                    } else {
                        throw new IllegalStateException(
                                "Unbound global name should have been rejected by the type checker."
                        );
                    }
                } else {
                    yield Value.vFree(node, name);
                }
            }
            case Term.Bound(Node _, int index) -> env.get(index);
            case Term.App(Node _, Term.Inferable f, Term.Checkable arg) -> vApp(
                    eval(f, env, globals),
                    eval(arg, env, globals)
            );
            case Term.Inf(Node _, Term.Inferable inf) -> eval(inf, env, globals);
            case Term.Lam(Node node, Term.Checkable body) -> new Value.VLam(
                    node,
                    x -> eval(body, ConsList.cons(x, env), globals)
            );
            case Term.Star(Node node) -> new Value.VStar(node);
            case Term.Pi(Node node, Term.Checkable in, Term.Checkable out) -> new Value.VPi(
                    node,
                    eval(in, env, globals),
                    x -> eval(out, ConsList.cons(x, env), globals)
            );
        };
    }

    private static @NotNull Value vApp(Value func, Value arg) {
        return switch (func) {
            case Value.VLam(Node _, Function<Value, Value> lam) -> lam.apply(arg);
            case Value.VNeutral n -> new Value.NApp(n.node(), n, arg);
            case Value.VPi _ -> throw new IllegalStateException("Should not apply a Pi type");
            case Value.VStar _ -> throw new IllegalStateException("Should not apply a Star type");
        };
    }

    private static @NotNull Term.Checkable reify(int depth, Value value) {
        return switch (value) {
            case Value.VLam(Node node, Function<Value, Value> lam) -> new Term.Lam(
                    node,
                    reify(depth + 1, lam.apply(Value.vFree(node, new Name.Quote(depth))))
            );
            case Value.VNeutral n -> new Term.Inf(n.node(), neutralReify(depth, n));
            case Value.VPi(Node node, Value in, Function<Value, Value> out) -> new Term.Inf(
                    node,
                    new Term.Pi(
                            node,
                            reify(depth, in),
                            reify(
                                    depth + 1,
                                    out.apply(Value.vFree(node, new Name.Quote(depth)))
                            )
                    )
            );
            case Value.VStar(Node node) -> new Term.Inf(node, new Term.Star(node));
        };
    }

    private static Term.Inferable neutralReify(int depth, Value.VNeutral n) {
        return switch (n) {
            case Value.NFree(Node node, Name name) -> {
                if (name instanceof Name.Quote(int k)) {
                    yield new Term.Bound(node, depth - k - 1);
                } else {
                    yield new Term.Free(node, name);
                }
            }
            case Value.NApp(Node node, Value.VNeutral func, Value arg) -> new Term.App(
                    node,
                    neutralReify(depth, func),
                    reify(depth, arg)
            );
        };
    }
}
