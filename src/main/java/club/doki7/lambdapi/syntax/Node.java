package club.doki7.lambdapi.syntax;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

/// 简单类型 Lambda 演算 λ<sub>→</sub> 和依值类型 Lambda 演算 λ<sub>Π</sub> 的抽象语法树/核心语法节点
///
/// {@snippet lang="bnf" :
/// e, ρ, κ ::= e : ρ        (1) 带注解的词项 // @link substring=(1) target=Ann
///           | *            (2) 类型之类型 // @link substring=(2) target=Aster
///           | Πx : ρ . ρ'  (3) 依值函数类型 // @link substring=(3) target=Pi
///           | x            (4) 变量 // @link substring=(4) target=Var
///           | e e'         (5) 应用 // @link substring=(5) target=App
///           | λx . e       (6) Lambda 抽象 // @link substring=(6) target=Lam
/// }
///
/// 规则 3 同时用于 λ<sub>→</sub> 和 λ<sub>Π</sub>，
/// 因为简单函数类型 `ρ → ρ'` 可以被看作是依值函数类型 `Π_ : ρ . ρ'` 的语法糖
public sealed interface Node {
    @NotNull Token location();

    record Ann(@NotNull Node term, @NotNull Node annotation) implements Node {
        @Override
        public @NotNull Token location() {
            return term.location();
        }

        @Override
        public @NotNull String toString() {
            if (term instanceof Var || term instanceof Aster) {
                return term + " : " + annotation;
            } else {
                return "(" + term + ") : " + annotation;
            }
        }
    }

    record Aster(@NotNull Token aster) implements Node {
        @Override
        public @NotNull Token location() {
            return aster;
        }

        @TestOnly
        public Aster() {
            this(Token.symbol(Token.Kind.ASTER));
        }

        @Override
        public @NotNull String toString() {
            return "*";
        }
    }

    record Pi(@Nullable Token param, @NotNull Node paramType, @NotNull Node body)
            implements Node
    {
        @Override
        public @NotNull Token location() {
            return (param != null) ? param : paramType.location();
        }

        @TestOnly
        public Pi(@Nullable String param, @NotNull Node paramType, @NotNull Node body) {
            Token paramToken = (param != null) ? Token.ident(param) : null;
            this(paramToken, paramType, body);
        }

        @Override
        public @NotNull String toString() {
            if (param != null) {
                return "(Π" + param.lexeme + " : " + paramType + ") → " + body;
            } else {
                if (paramType instanceof Pi
                    || paramType instanceof Ann
                    || paramType instanceof Lam) {
                    return "(" + paramType + ") → " + body;
                } else {
                    return paramType + " → " + body;
                }
            }
        }
    }

    record Var(@NotNull Token name) implements Node {
        @Override
        public @NotNull Token location() {
            return name;
        }

        @TestOnly
        public Var(@NotNull String name) {
            this(Token.ident(name));
        }

        @Override
        public @NotNull String toString() {
            return name.lexeme;
        }
    }

    record App(@NotNull Node func, @NotNull Node arg) implements Node {
        @Override
        public @NotNull Token location() {
            return func.location();
        }

        @Override
        public @NotNull String toString() {
            StringBuilder sb = new StringBuilder();
            if (func instanceof Lam || func instanceof Pi || func instanceof Ann) {
                sb.append("(").append(func).append(")");
            } else {
                sb.append(func);
            }

            sb.append(" ");

            if (arg instanceof App || arg instanceof Lam || arg instanceof Pi || arg instanceof Ann) {
                sb.append("(").append(arg).append(")");
            } else {
                sb.append(arg);
            }

            return sb.toString();
        }
    }

    record Lam(@NotNull Token param, @NotNull Node body) implements Node {
        @Override
        public @NotNull Token location() {
            return param;
        }

        @TestOnly
        public Lam(@NotNull String param, @NotNull Node body) {
            this(Token.ident(param), body);
        }

        @Override
        public @NotNull String toString() {
            if (body instanceof Ann || body instanceof Pi) {
                return "λ" + param.lexeme + ". (" + body + ")";
            } else {
                return "λ" + param.lexeme + ". " + body;
            }
        }
    }
}
