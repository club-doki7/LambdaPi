package club.doki7.lambdapi.ind;

import club.doki7.lambdapi.common.Name;
import club.doki7.lambdapi.dtlc.*;
import club.doki7.lambdapi.exc.TypeCheckException;
import club.doki7.lambdapi.syntax.Node;
import club.doki7.lambdapi.util.ConsList;
import club.doki7.lambdapi.util.Pair;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;

public final class IndVec {
    public record Vec(Node node,
                      Term.Checkable type,
                      Term.Checkable len) implements Term.InferableTF {
        @Override
        public Type infer(int depth,
                          ConsList<Pair<Name.Local, Type>> ctx,
                          Globals globals) throws TypeCheckException {
            InferCheck.check(depth, ctx, globals, type, Type.of(new Value.VStar(node)));
            InferCheck.check(depth, ctx, globals, len, Type.of(new IndNat.VNat(node)));
            return Type.of(new Value.VStar(node));
        }

        @Override
        public Value eval(ConsList<Value> env, Map<String, Value> globals) {
            return new VVec(
                    node,
                    Type.of(Eval.eval(type, env, globals)),
                    Eval.eval(len, env, globals)
            );
        }

        @Override
        public InferableTF subst(int depth, Free r) {
            return new Vec(
                    node,
                    InferCheck.subst(depth, r, type),
                    InferCheck.subst(depth, r, len)
            );
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Vec vec)) return false;
            return type.equals(vec.type) && len.equals(vec.len);
        }

        @Override
        public int hashCode() {
            return Objects.hash(Vec.class, type, len);
        }

        @Override
        public @NotNull String toString() {
            return "Vec<" + type + ", " + len + ">";
        }
    }

    public record Nil(Node node, Term.Checkable type) implements Term.InferableTF {
        @Override
        public Type infer(int depth,
                          ConsList<Pair<Name.Local, Type>> ctx,
                          Globals globals) throws TypeCheckException {
            InferCheck.check(depth, ctx, globals, type, Type.of(new Value.VStar(node)));
            return Type.of(new VVec(
                    node,
                    Type.of(Eval.eval(type, globals.values())),
                    new IndNat.VZero(node)
            ));
        }

        @Override
        public Value eval(ConsList<Value> env, Map<String, Value> globals) {
            return new VNil(
                    node,
                    Eval.eval(type, env, globals)
            );
        }

        @Override
        public InferableTF subst(int depth, Free r) {
            return new Nil(
                    node,
                    InferCheck.subst(depth, r, type)
            );
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Nil nil)) return false;
            return type.equals(nil.type);
        }

        @Override
        public int hashCode() {
            return Objects.hash(Nil.class, type);
        }

        @Override
        public @NotNull String toString() {
            return "Vec<" + type + ", 0>[]";
        }
    }

    public record Cons(Node node,
                       Term.Checkable type,
                       Term.Checkable len,
                       Term.Checkable head,
                       Term.Checkable tail) implements Term.InferableTF {

        @Override
        public Type infer(int depth,
                          ConsList<Pair<Name.Local, Type>> ctx,
                          Globals globals) throws TypeCheckException {
            InferCheck.check(depth, ctx, globals, type, Type.of(new Value.VStar(node)));
            InferCheck.check(depth, ctx, globals, len, Type.of(new IndNat.VNat(node)));

            Type tvType = Type.of(Eval.eval(type, globals.values()));
            Value vLen = Eval.eval(len, globals.values());
            InferCheck.check(depth, ctx, globals, head, tvType);

            Type vecType = Type.of(new VVec(node, tvType, vLen));
            InferCheck.check(depth, ctx, globals, tail, vecType);

            return Type.of(new VVec(node, tvType, new IndNat.VSucc(node, vLen)));
        }

        @Override
        public Value eval(ConsList<Value> env, Map<String, Value> globals) {
            return new IndVec.VCons(
                    node,
                    Eval.eval(type, env, globals),
                    Eval.eval(len, env, globals),
                    Eval.eval(head, env, globals),
                    Eval.eval(tail, env, globals)
            );
        }

        @Override
        public InferableTF subst(int depth, Free r) {
            return new Cons(
                    node,
                    InferCheck.subst(depth, r, type),
                    InferCheck.subst(depth, r, len),
                    InferCheck.subst(depth, r, head),
                    InferCheck.subst(depth, r, tail)
            );
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof Cons cons)) return false;
            return type.equals(cons.type)
                    && len.equals(cons.len)
                    && head.equals(cons.head)
                    && tail.equals(cons.tail);
        }

        @Override
        public int hashCode() {
            return Objects.hash(Cons.class, type, len, head, tail);
        }

        @Override
        public @NotNull String toString() {
            ArrayList<Term> elements = new ArrayList<>();
            elements.add(head);

            Term.Checkable currentTail = tail;
            while (currentTail instanceof Term.Inf(Node _, IndVec.Cons cons)) {
                elements.add(cons.head());
                currentTail = cons.tail();
            }

            return "Vec<" + type + ", " + len + ">[" +
                    String.join("; ", elements.stream().map(Object::toString).toList()) +
                    "]";
        }
    }

    public record VecElim(Node node,
                          Term.Checkable type,
                          Term.Checkable motive,
                          Term.Checkable base,
                          Term.Checkable step,
                          Term.Checkable len,
                          Term.Checkable scrut) implements Term.InferableTF {
        @Override
        public Type infer(int depth,
                          ConsList<Pair<Name.Local, Type>> ctx,
                          Globals globals) throws TypeCheckException {
            return null;
        }

        @Override
        public Value eval(ConsList<Value> env, Map<String, Value> globals) {
            return null;
        }

        @Override
        public InferableTF subst(int depth, Free r) {
            return null;
        }
    }

    public record VVec(Node node, Type type, Value len) implements Value.CValue {
        @Override
        public @NotNull Term.Checkable reify(int depth) {
            return new Term.Checkable.Inf(
                    node,
                    new Vec(node, Eval.reify(depth, type.value()), Eval.reify(depth, len))
            );
        }

        @Override
        public @NotNull Value vApp(Value arg) {
            throw new IllegalStateException("Cannot apply vector type to an argument.");
        }
    }

    public record VNil(Node node, Value type) implements Value.CValue {
        @Override
        public @NotNull Term.Checkable reify(int depth) {
            return new Term.Checkable.Inf(
                    node,
                    new Nil(node, Eval.reify(depth, type))
            );
        }

        @Override
        public @NotNull Value vApp(Value arg) {
            throw new IllegalStateException("Cannot apply nil to an argument.");
        }
    }

    public record VCons(Node node,
                        Value type,
                        Value len,
                        Value head,
                        Value tail) implements Value.CValue {
        @Override
        public @NotNull Term.Checkable reify(int depth) {
            return new Term.Checkable.Inf(
                    node,
                    new Cons(
                            node,
                            Eval.reify(depth, type),
                            Eval.reify(depth, len),
                            Eval.reify(depth, head),
                            Eval.reify(depth, tail)
                    )
            );
        }

        @Override
        public @NotNull Value vApp(Value arg) {
            throw new IllegalStateException("Cannot apply cons to an argument.");
        }
    }
}
