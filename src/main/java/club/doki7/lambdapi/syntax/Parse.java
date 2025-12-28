package club.doki7.lambdapi.syntax;

/// 简单类型 Lambda 演算 λ<sub>→</sub> 和依值类型 Lambda 演算 λ<sub>Π</sub> 的解析器
///
/// 具体语法：
///
/// {@snippet lang="bnf" :
///expr ::= expr ':' arrow-expr
///        | arrow-expr
///
/// arrow-expr ::= forall identifier ':' simple-expr generic-arrow arrow-expr
///              | forall '(' identifier-list ':' expr ')' generic-arrow arrow-expr
///              | lambda identifier lambda-arrow arrow-expr
///              | app-expr generic-arrow arrow-expr
///              | app-expr
///
/// app-expr ::= app-expr simple-expr
///            | simple-expr
///
/// simple-expr ::= '(' expr ')'
///               | identifier
///               | '*'
///
/// forall ::= 'forall' | 'Π' | '∀'
/// identifier-list ::= identifier (',' identifier)*
/// generic-arrow ::= '->' | '→' | '.' | ','
/// lambda ::= 'λ' | 'lambda' | '\'
/// lambda-arrow ::= '→' | '->' | '.'
/// }
public final class Parse {
}
