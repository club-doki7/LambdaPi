package club.doki7.lambdapi.stlc;

import club.doki7.lambdapi.syntax.Node;
import club.doki7.lambdapi.util.ConsList;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.function.Function;

public final class Eval {
    public static Value eval(Term term, Map<String, Value> globals) {
        return eval(term, ConsList.nil(), Map.copyOf(globals));
    }

    public static Term reify(Value value) {
        return reify(0, value);
    }

    private static Value eval(Term term, ConsList<Value> env, Map<String, Value> globals) {
        return switch (term) {
            case Term.Ann(Node _, Term t, Type _) -> eval(t, env, globals);
            case Term.Free(Node _, Name name) -> {
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
                    yield Value.vFree(name);
                }
            }
            case Term.Bound(Node _, int index) -> env.get(index);
            case Term.App(Node _, Term.Inferable f, Term.Checkable arg) -> vApp(
                    eval(f, env, globals),
                    eval(arg, env, globals)
            );

            case Term.Inf(Node _, Term.Inferable inf) -> eval(inf, env, globals);
            case Term.Lam(Node _, Term.Checkable body) -> new Value.VLam(
                    x -> eval(body, ConsList.cons(x, env), globals)
            );
        };
    }

    private static @NotNull Value vApp(Value func, Value arg) {
        return switch (func) {
            case Value.VLam(Function<Value, Value> lam) -> lam.apply(arg);
            case Value.VNeutral n -> new Value.NApp(n, arg);
        };
    }

    private static Term.@NotNull Checkable reify(int depth, Value value) {
        return switch (value) {
            case Value.VLam(Function<Value, Value> lam) -> new Term.Lam(
                    null,
                    reify(depth + 1, lam.apply(Value.vFree(new Name.Quote(depth))))
            );
            case Value.VNeutral n -> new Term.Inf(
                    null,
                    neutralReify(depth, n)
            );
        };
    }

    private static Term.Inferable neutralReify(int depth, Value.VNeutral n) {
        return switch (n) {
            case Value.NFree(Name name) -> {
                if (name instanceof Name.Quote(int k)) {
                    yield new Term.Bound(null, depth - k - 1);
                } else {
                    yield new Term.Free(null, name);
                }
            }
            case Value.NApp(Value.VNeutral func, Value arg) -> new Term.App(
                    null,
                    neutralReify(depth, func),
                    reify(depth, arg)
            );
        };
    }
}
