package club.doki7.lambdapi.ind;

import club.doki7.lambdapi.common.Name;
import club.doki7.lambdapi.dtlc.*;
import club.doki7.lambdapi.exc.TypeCheckException;
import club.doki7.lambdapi.syntax.Node;
import club.doki7.lambdapi.util.ConsList;
import club.doki7.lambdapi.util.Pair;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

public final class IndNat {
    public record Nat(Node node) implements Term.InferableTF {
        @Override
        public Type infer(int depth,
                          ConsList<Pair<Name.Local, Type>> ctx,
                          Globals globals) {
            return new Type(new Value.VStar(node));
        }

        @Override
        public Value eval(ConsList<Value> env, Map<String, Value> globals) {
            return new VNat(node);
        }

        @Override
        public InferableTF subst(int depth, Free r) {
            return this;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            return o instanceof Nat;
        }

        @Override
        public int hashCode() {
            return Nat.class.hashCode();
        }

        @Override
        public @NotNull String toString() {
            return "Nat";
        }
    }

    public record Zero(Node node) implements Term.InferableTF {
        @Override
        public Type infer(int depth,
                          ConsList<Pair<Name.Local, Type>> ctx,
                          Globals globals) {
            return new Type(new VNat(node));
        }

        @Override
        public Value eval(ConsList<Value> env, Map<String, Value> globals) {
            return new VZero(node);
        }

        @Override
        public InferableTF subst(int depth, Free r) {
            return this;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            return o instanceof Zero;
        }

        @Override
        public int hashCode() {
            return Zero.class.hashCode();
        }

        @Override
        public @NotNull String toString() {
            return "0";
        }
    }

    public record Succ(Node node, Term.Checkable pred) implements Term.InferableTF {
        @Override
        public Type infer(int depth,
                          ConsList<Pair<Name.Local, Type>> ctx,
                          Globals globals) throws TypeCheckException {
            Type natType = new Type(new VNat(node));
            InferCheck.check(depth, ctx, globals, pred, natType);
            return natType;
        }

        @Override
        public Value eval(ConsList<Value> env, Map<String, Value> globals) {
            return new VSucc(node, Eval.eval(pred, env, globals));
        }

        @Override
        public InferableTF subst(int depth, Free r) {
            return new Succ(node, InferCheck.subst(depth, r, pred));
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Succ succ)) return false;
            return this.pred.equals(succ.pred);
        }

        @Override
        public int hashCode() {
            return Objects.hash(Succ.class, pred);
        }

        @Override
        public @NotNull String toString() {
            int sCounter = 1;
            Term.Checkable current = pred;
            while (current instanceof Term.Inf(Node _, Succ(_, Term.Checkable next))) {
                sCounter++;
                current = next;
            }

            if (current instanceof Term.Inf(Node _, Zero _)) {
                return Integer.toString(sCounter);
            } else if (sCounter == 1) {
                return "(suc " + pred + ")";
            } else {
                return "(suc_" + sCounter + " " + current + ")";
            }
        }
    }

    public record NatElim(Node node,
                          Term.Checkable motive,
                          Term.Checkable base,
                          Term.Checkable step,
                          Term.Checkable scrut) implements Term.InferableTF {
        @Override
        public Type infer(int depth,
                          ConsList<Pair<Name.Local, Type>> ctx,
                          Globals globals) throws TypeCheckException {
            Type natType = new Type(new VNat(node));

            // motive : forall (n : Nat) -> *
            Type motiveType = new Type(new Value.VPi(
                    node,
                    natType,
                    _ -> new Type(new Value.VStar(node))
            ));
            InferCheck.check(depth, ctx, globals, motive, motiveType);

            Value vMotive = Eval.eval(motive, globals.values());

            // base : motive 0
            Type baseType = new Type(Eval.vApp(vMotive, new VZero(node)));
            InferCheck.check(depth, ctx, globals, base, baseType);

            // step: forall (n : Nat) -> motive n -> motive (S n)
            Type stepType = new Type(new Value.VPi(
                    node,
                    natType,
                    n -> new Type(new Value.VPi(
                            node,
                            new Type(Eval.vApp(vMotive, n)),
                            _ -> new Type(Eval.vApp(vMotive, new VSucc(node, n)))
                    ))
            ));
            InferCheck.check(depth, ctx, globals, step, stepType);

            // scrut : Nat
            InferCheck.check(depth, ctx, globals, scrut, natType);

            Value vScrut = Eval.eval(scrut, globals.values());
            return new Type(Eval.vApp(vMotive, vScrut));
        }

        @Override
        public Value eval(ConsList<Value> env, Map<String, Value> globals) {
            Value vBase = Eval.eval(base, env, globals);
            Value vStep = Eval.eval(step, env, globals);
            Function<Value, Value> rec = new Function<>() {
                @Override
                public Value apply(Value v) {
                    return switch (v) {
                        case VZero _ -> vBase;
                        case VSucc(Node _, Value pred) -> Eval.vApp(
                                Eval.vApp(vStep, pred),
                                this.apply(pred)
                        );
                        case Value.VNeutral vn -> new NNatElim(
                                node,
                                Eval.eval(motive, env, globals),
                                vBase,
                                vStep,
                                vn
                        );
                        default -> throw new IllegalStateException(
                                "Unexpected value in NatElim recursion: " + v
                        );
                    };
                }
            };
            return rec.apply(Eval.eval(scrut, env, globals));
        }

        @Override
        public InferableTF subst(int depth, Free r) {
            return new NatElim(
                    node,
                    InferCheck.subst(depth, r, motive),
                    InferCheck.subst(depth, r, base),
                    InferCheck.subst(depth, r, step),
                    InferCheck.subst(depth, r, scrut)
            );
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof NatElim natElim)) return false;
            return this.motive.equals(natElim.motive)
                    && this.base.equals(natElim.base)
                    && this.step.equals(natElim.step)
                    && this.scrut.equals(natElim.scrut);
        }

        @Override
        public int hashCode() {
            return Objects.hash(NatElim.class, motive, base, step, scrut);
        }

        @Override
        public @NotNull String toString() {
            return "(natElim " + motive + " " + base + " " + step + " " + scrut + ")";
        }
    }

    public record VNat(Node node) implements Value.CValue {
        @Override
        public @NotNull Term.Checkable reify(int depth) {
            return new Term.Inf(node, new Nat(node));
        }

        @Override
        public @NotNull Value vApp(Value arg) {
            throw new IllegalStateException("Cannot apply natural number type to an argument.");
        }
    }

    public record VZero(Node node) implements Value.CValue {
        @Override
        public @NotNull Term.Checkable reify(int depth) {
            return new Term.Inf(node, new Zero(node));
        }

        @Override
        public @NotNull Value vApp(Value arg) {
            throw new IllegalStateException("Cannot apply zero constructor to an argument.");
        }
    }

    public record VSucc(Node node, Value pred) implements Value.CValue {
        @Override
        public @NotNull Term.Checkable reify(int depth) {
            return new Term.Inf(node, new Succ(node, Eval.reify(depth, pred)));
        }

        @Override
        public @NotNull Value vApp(Value arg) {
            throw new IllegalStateException("Cannot apply successor constructor to an argument.");
        }
    }

    public record NNatElim(Node node,
                           Value motive,
                           Value base,
                           Value step,
                           Value.VNeutral nScrut) implements Value.CNeutral {
        @Override
        public @NotNull Term.Inferable neutralReify(int depth) {
            Term.Inferable scrutReify = Eval.neutralReify(depth, nScrut);
            return new NatElim(
                    node,
                    Eval.reify(depth, motive),
                    Eval.reify(depth, base),
                    Eval.reify(depth, step),
                    new Term.Inf(scrutReify.node(), scrutReify)
            );
        }
    }
}
