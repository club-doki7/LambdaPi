package club.doki7.lambdapi.syntax;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/// ## 抽象语法
///
/// 取自 LambdaPi.pdf 第 2.1 节和第 3.1 节。
///
/// ```bnf
/// e, ρ, κ ::= e : ρ         (1) 带注解的词项
///            | *             (2) 类型之类型
///            | Πx : ρ -> ρ'  (3) 依值函数类型
///            | ρ → ρ'        (4) 非依值函数类型
///            | x             (5) 变量
///            | e e'          (6) 应用
///            | λx . e        (7) Lambda 抽象
/// ```
///
/// - 依值类型 Lambda 演算 (λ<sub>Π</sub>) 使用以上全部规则；
/// - 简单类型 Lambda 演算 (λ<sub>→</sub>) 不使用 (2) 和 (3)。
///
/// {@link Pi Pi} 类型用于表示 (3) 和 (4) 两种情况，因为 ρ → ρ' 可以被看作是 Π_ : ρ -> ρ' 的语法糖。
public sealed interface Node {
    /// 带注解的词项
    ///
    /// @see Node
    record Ann(@NotNull Node term, @NotNull Node annotation) implements Node {}

    /// 类型之类型
    ///
    /// @see Node
    record Aster(@NotNull Token aster) implements Node {}

    /// 依值函数类型和非依值函数类型
    ///
    /// @see Node
    record Pi(@Nullable Token param, @NotNull Node paramType, @NotNull Node body) implements Node {}

    /// 变量
    ///
    /// @see Node
    record Var(@NotNull Token name) implements Node {}

    /// 应用
    ///
    /// @see Node
    record App(@NotNull Node func, @NotNull Node arg) implements Node {}

    /// Lambda 抽象
    ///
    /// @see Node
    record Lam(@NotNull Token param, @NotNull Node body) implements Node {}
}
