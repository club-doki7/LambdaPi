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
import java.util.function.BiFunction;

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
                    Type.of(Eval.eval(type, env, globals)),
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

            return "Vec["
                   + type + "; "
                   + String.join(", ", elements.stream().map(Object::toString).toList())
                   + "]";
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
            Type univType = Type.of(new Value.VStar(node));
            Type natType = Type.of(new IndNat.VNat(node));

            InferCheck.check(depth, ctx, globals, type, univType);
            Type tvType = Type.of(Eval.eval(type, globals.values()));

            Type motiveType = Type.of(new Value.VPi(
                    node,
                    natType,
                    k -> Type.of(new Value.VPi(
                            node,
                            Type.of(new VVec(node, tvType, k)),
                            _ -> univType
                    ))
            ));
            InferCheck.check(depth, ctx, globals, motive, motiveType);
            Value vMotive = Eval.eval(motive, globals.values());

            Value vBaseCaseType = Eval.vApp(vMotive, new IndNat.VZero(node));
            vBaseCaseType = Eval.vApp(vBaseCaseType, new VNil(node, tvType.value()));
            InferCheck.check(depth, ctx, globals, base, Type.of(vBaseCaseType));

            Type stepType = Type.of(new Value.VPi(
                    node,
                    natType,
                    l -> Type.of(new Value.VPi(
                            node,
                            tvType,
                            y -> Type.of(new Value.VPi(
                                    node,
                                    Type.of(new VVec(node, tvType, l)),
                                    ys -> Type.of(new Value.VPi(
                                            node,
                                            Type.of(Eval.vApp(Eval.vApp(vMotive, l), ys)),
                                            _ -> Type.of(Eval.vApp(
                                                    Eval.vApp(
                                                            vMotive,
                                                            new IndNat.VSucc(node, l)
                                                    ),
                                                    new VCons(node, tvType, l, y, ys)
                                            ))
                                    ))
                            ))
                    ))
            ));
            InferCheck.check(depth, ctx, globals, step, stepType);

            InferCheck.check(depth, ctx, globals, len, natType);
            Value vLen = Eval.eval(len, globals.values());

            InferCheck.check(
                    depth,
                    ctx,
                    globals,
                    scrut,
                    Type.of(new VVec(node, tvType, vLen))
            );
            Value vScrut = Eval.eval(scrut, globals.values());
            return Type.of(Eval.vApp(Eval.vApp(vMotive, vLen), vScrut));
        }

        @Override
        public Value eval(ConsList<Value> env, Map<String, Value> globals) {
            Value vBase = Eval.eval(base, env, globals);
            Value vStep = Eval.eval(step, env, globals);

            BiFunction<Value, Value, Value> rec = new BiFunction<>() {
                @Override
                public Value apply(Value vLen, Value vVec) {
                    return switch (vVec) {
                        case VNil _ -> vBase;
                        case VCons(Node _, Type _, Value len1, Value head, Value tail) -> {
                            Value step1 = Eval.vApp(vStep, len1);
                            Value step2 = Eval.vApp(step1, head);
                            Value step3 = Eval.vApp(step2, tail);
                            yield Eval.vApp(step3, this.apply(len1, tail));
                        }
                        case Value.VNeutral vn -> new NVecElim(
                                node,
                                Eval.eval(type, env, globals),
                                Eval.eval(motive, env, globals),
                                vBase,
                                vStep,
                                vLen,
                                vn
                        );
                        default -> throw new IllegalStateException(
                                "Unexpected value in VecElim recursion: " + vVec
                        );
                    };
                }
            };

            Value vLen = Eval.eval(len, env, globals);
            Value vScrut = Eval.eval(scrut, env, globals);
            return rec.apply(vLen, vScrut);
        }

        @Override
        public InferableTF subst(int depth, Free r) {
            return new VecElim(
                    node,
                    InferCheck.subst(depth, r, type),
                    InferCheck.subst(depth, r, motive),
                    InferCheck.subst(depth, r, base),
                    InferCheck.subst(depth, r, step),
                    InferCheck.subst(depth, r, len),
                    InferCheck.subst(depth, r, scrut)
            );
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof VecElim vecElim)) return false;
            return type.equals(vecElim.type)
                    && motive.equals(vecElim.motive)
                    && base.equals(vecElim.base)
                    && step.equals(vecElim.step)
                    && len.equals(vecElim.len)
                    && scrut.equals(vecElim.scrut);
        }

        @Override
        public int hashCode() {
            return Objects.hash(VecElim.class, type, motive, base, step, len, scrut);
        }

        @Override
        public @NotNull String toString() {
            return "(vecElim "
                   + type + " "
                   + motive + " "
                   + base + " "
                   + step + " "
                   + len + " "
                   + scrut
                   + ")";
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
                        Type type,
                        Value len,
                        Value head,
                        Value tail) implements Value.CValue {
        @Override
        public @NotNull Term.Checkable reify(int depth) {
            return new Term.Checkable.Inf(
                    node,
                    new Cons(
                            node,
                            Eval.reify(depth, type.value()),
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

    public record NVecElim(Node node,
                           Value type,
                           Value motive,
                           Value base,
                           Value step,
                           Value len,
                           Value.VNeutral nScrut) implements Value.CNeutral {
        @Override
        public @NotNull Term.Inferable neutralReify(int depth) {
            Term.Inferable scrutReify = Eval.neutralReify(depth, nScrut);
            return new VecElim(
                    node,
                    Eval.reify(depth, type),
                    Eval.reify(depth, motive),
                    Eval.reify(depth, base),
                    Eval.reify(depth, step),
                    Eval.reify(depth, len),
                    new Term.Inf(scrutReify.node(), scrutReify)
            );
        }
    }
}
