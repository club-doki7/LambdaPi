package club.doki7.lambdapi.stlc;

import club.doki7.lambdapi.exc.TypeCheckException;
import club.doki7.lambdapi.syntax.Node;
import club.doki7.lambdapi.syntax.Token;
import club.doki7.lambdapi.util.ConsList;
import club.doki7.lambdapi.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Objects;

public final class InferCheck {
    public sealed interface Kind permits HasKind, HasType {}

    public record HasKind() implements Kind {
        @Override
        public @NotNull String toString() {
            return ":: *";
        }
    }

    public record HasType(@NotNull Type type) implements Kind {
        @Override
        public @NotNull String toString() {
            return ":: " + type;
        }
    }

    public static @NotNull Type infer(
            Map<String, Kind> globals,
            Term.Inferable inferable
    ) throws TypeCheckException {
        return infer(0, ConsList.nil(), globals, inferable);
    }

    public static void checkKind(
            Token location,
            Map<String, Kind> globals,
            Type type
    ) throws TypeCheckException {
        checkKind(location, ConsList.nil(), globals, type);
    }

    private static @NotNull Type infer(
            int depth,
            ConsList<Pair<Name.Local, Kind>> ctx,
            Map<String, Kind> globals,
            Term.Inferable inferable
    ) throws TypeCheckException {
        switch (inferable) {
            case Term.Ann(Node node, Term.Checkable term, Type annotation) -> {
                checkKind(Objects.requireNonNull(node).location(), ctx, globals, annotation);
                check(depth, ctx, globals, term, annotation);
                return annotation;
            }
            case Term.Free(Node node, Name name) -> {
                @Nullable Pair<Name.Local, Kind> entry = ctx.findFirst(p -> p.first().equals(name));
                if (entry != null) {
                    if (!(entry.second() instanceof HasType(Type t))) {
                        throw new TypeCheckException(
                                Objects.requireNonNull(node).location(),
                                "In STLC, type is not allowed at term level"
                        );
                    }
                    return t;
                }

                if (name instanceof Name.Global(String strName)) {
                    Kind kind = globals.get(strName);
                    if (kind != null) {
                        if (!(kind instanceof HasType(Type t))) {
                            throw new TypeCheckException(
                                    Objects.requireNonNull(node).location(),
                                    "In STLC, type is not allowed at term level"
                            );
                        }
                        return t;
                    }
                }

                throw new TypeCheckException(
                        Objects.requireNonNull(node).location(),
                        "Undefined variable identifier " + name
                );
            }
            case Term.App(Node node, Term.Inferable f, Term.Checkable arg) -> {
                Type fType = infer(depth, ctx, globals, f);
                if (!(fType instanceof Type.Fun(Type in, Type out))) {
                    throw new TypeCheckException(
                            Objects.requireNonNull(node).location(),
                            "Expected function type in application"
                    );
                }
                check(depth, ctx, globals, arg, in);
                return out;
            }
            case Term.Bound _ -> throw new IllegalStateException(
                    "Unexpected bound variable in type checking phase"
            );
        }
    }

    private static void check(
            int depth,
            ConsList<Pair<Name.Local, Kind>> ctx,
            Map<String, Kind> globals,
            Term.Checkable checkable,
            Type expected
    ) throws TypeCheckException {
        switch (checkable) {
            case Term.Inf(Node node, Term.Inferable inferable) -> {
                Type inferred = infer(depth, ctx, globals, inferable);
                if (!inferred.equals(expected)) {
                    throw new TypeCheckException(
                            Objects.requireNonNull(node).location(),
                            "Type mismatch, expected " + expected + ", inferred " + inferred
                    );
                }
            }
            case Term.Lam(Node node, Term.Checkable body) -> {
                if (!(expected instanceof Type.Fun(Type in, Type out))) {
                    throw new TypeCheckException(
                            Objects.requireNonNull(node).location(),
                            "Lambda terms can be only checked as function type, got " + expected
                    );
                }

                var newCtx = ConsList.cons(new Pair<>(new Name.Local(depth), new HasType(in)), ctx);
                Term.Checkable newBody = subst(0, new Term.Free(node, new Name.Local(depth)), body);
                check(depth + 1, newCtx, globals, newBody, out);
            }
        }
    }

    private static void checkKind(
            Token location,
            ConsList<Pair<Name.Local, Kind>> ctx,
            Map<String, Kind> globals,
            Type type
    ) throws TypeCheckException {
        switch (type) {
            case Type.Free(Name.Global(String strName)) -> {
                @Nullable Kind kind = globals.get(strName);
                if (kind == null) {
                    throw new TypeCheckException(location, "Undefined type identifier " + strName);
                }
                if (!(kind instanceof HasKind)) {
                    throw new TypeCheckException(location, strName + " is not a type");
                }
            }
            case Type.Free(Name.Local _) -> {
                throw new IllegalStateException("Unexpected local in type checking phase");
//                Pair<Name.Local, Kind> binding = ctx.findFirst(p -> p.first().index() == index);
//                if (binding == null) {
//                    throw new TypeCheckException(location, "Unbound local type variable L" + index);
//                }
//                if (!(binding.second() instanceof HasKind)) {
//                    throw new TypeCheckException(location, "L" + index + " is not a type");
//                }
            }
            case Type.Free(Name.Quote _) -> throw new IllegalStateException(
                    "Unexpected quote in type checking phase"
            );
            case Type.Fun(Type in, Type out) -> {
                checkKind(location, ctx, globals, in);
                checkKind(location, ctx, globals, out);
            }
        }
    }

    private static Term.Inferable subst(int depth, Term.Free r, Term.Inferable inferable) {
        return switch (inferable) {
            case Term.Ann(Node node, Term.Checkable term, Type annotation) -> new Term.Ann(
                    node,
                    subst(depth, r, term),
                    annotation
            );
            case Term.Bound bound -> bound.index() == depth ? r : bound;
            case Term.Free free -> free;
            case Term.App(Node node, Term.Inferable f, Term.Checkable arg) -> new Term.App(
                    node,
                    subst(depth, r, f),
                    subst(depth, r, arg)
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
