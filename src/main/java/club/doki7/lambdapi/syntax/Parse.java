package club.doki7.lambdapi.syntax;

import club.doki7.lambdapi.exc.ParseException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/// 简单类型 Lambda 演算 λ<sub>→</sub> 和依值类型 Lambda 演算 λ<sub>Π</sub> 的解析器
///
/// 具体语法：
///
/// {@snippet lang="bnf" :
///program ::= declaration*
///
/// declaration ::= 'axiom' identifier-list in expr
///              | 'defun' identifier '=' expr
///              | 'check' expr
///
/// expr ::= expr in arrow-expr
///        | arrow-expr
///
/// arrow-expr ::= forall identifier in simple-expr generic-arrow arrow-expr
///              | forall '(' identifier-list in expr ')' generic-arrow arrow-expr
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
/// in ::= ':' | '::' | '<:' | '∈' | 'in'
/// identifier-list ::= identifier (',' identifier)*
/// generic-arrow ::= '->' | '→' | '.' | ','
/// lambda ::= 'λ' | 'lambda' | '\'
/// lambda-arrow ::= '→' | '->' | '.'
/// }
public final class Parse {
    public static @NotNull PNode parseProgram(@NotNull ArrayList<Token> tokens) throws ParseException {
        Parse p = new Parse(tokens);
        return p.parseProgram();
    }

    public static @NotNull Node parseExpr(@NotNull ArrayList<Token> tokens) throws ParseException {
        Parse p = new Parse(tokens);
        return p.parseExpr();
    }

    private Parse(@NotNull ArrayList<Token> tokens) {
        this.tokens = tokens;
        this.pos = 0;
    }

    private @NotNull PNode parseProgram() throws ParseException {
        List<PNode> items = new ArrayList<>();
        while (peek() != null) {
            items.add(parseDeclaration());
        }
        return new PNode.Program(items);
    }

    private @NotNull PNode parseDeclaration() throws ParseException {
        Token token = expectConsume(DECL_KINDS);

        if (token.kind == Token.Kind.KW_AXIOM) {
            List<Token> names = parseIdentifierList();
            expectConsume(Token.Kind.COLON);
            Node type = parseExpr();
            return new PNode.Axiom(names, type);
        } else if (token.kind == Token.Kind.KW_DEFUN) {
            Token name = expectConsume(Token.Kind.IDENT);
            expectConsume(Token.Kind.EQ);
            Node value = parseExpr();
            return new PNode.Defun(name, value);
        } else if (token.kind == Token.Kind.KW_CHECK) {
            Node term = parseExpr();
            return new PNode.Check(term);
        } else {
            throw new IllegalStateException("Unexpected declaration kind: " + token.kind);
        }
    }

    private @NotNull Node parseExpr() throws ParseException {
        Node left = parseArrowExpr();
        while (check(Token.Kind.COLON)) {
            consume();
            Node right = parseArrowExpr();
            left = new Node.Ann(left, right);
        }
        return left;
    }

    private @NotNull Node parseArrowExpr() throws ParseException {
        if (check(Token.Kind.PI)) {
            return parsePi();
        }

        if (check(Token.Kind.LAMBDA)) {
            return parseLambda();
        }

        Node left = parseAppExpr();
        if (check(GENERIC_ARROW_KINDS)) {
            consume();
            Node right = parseArrowExpr();
            return new Node.Pi((String) null, left, right);
        }
        return left;
    }

    private @NotNull Node parsePi() throws ParseException {
        consume();
        if (check(Token.Kind.LPAREN)) {
            consume();
            List<Token> idents = parseIdentifierList();
            expectConsume(Token.Kind.COLON);
            Node type = parseExpr();
            expectConsume(Token.Kind.RPAREN);
            expectConsume(GENERIC_ARROW_KINDS);
            Node body = parseArrowExpr();
            for (int i = idents.size() - 1; i >= 0; i--) {
                body = new Node.Pi(idents.get(i), type, body);
            }
            return body;
        } else {
            Token ident = expectConsume(Token.Kind.IDENT);
            expectConsume(Token.Kind.COLON);
            Node type = parseSimpleExpr();
            expectConsume(GENERIC_ARROW_KINDS);
            Node body = parseArrowExpr();
            return new Node.Pi(ident, type, body);
        }
    }

    private @NotNull List<Token> parseIdentifierList() throws ParseException {
        List<Token> idents = new ArrayList<>();
        idents.add(expectConsume(Token.Kind.IDENT));
        while (check(Token.Kind.COMMA)) {
            consume();
            idents.add(expectConsume(Token.Kind.IDENT));
        }
        return idents;
    }

    private @NotNull Node parseLambda() throws ParseException {
        consume();
        Token ident = expectConsume(Token.Kind.IDENT);
        expectConsume(LAMBDA_ARROW_KINDS);
        Node body = parseArrowExpr();
        return new Node.Lam(ident, body);
    }

    private @NotNull Node parseAppExpr() throws ParseException {
        Node left = parseSimpleExpr();
        Node right = tryParseSimpleExpr();
        if (right == null) {
            return left;
        }

        List<Node> args = new ArrayList<>();
        args.add(right);

        while (true) {
            right = tryParseSimpleExpr();
            if (right == null) {
                break;
            }
            args.add(right);
        }

        return new Node.App(left, args);
    }

    private @Nullable Node tryParseSimpleExpr() throws ParseException {
        Token t = peek();
        if (t == null) return null;
        return switch (t.kind) {
            case LPAREN -> {
                consume();
                Node inner = parseExpr();
                expectConsume(Token.Kind.RPAREN);
                yield inner;
            }
            case IDENT -> {
                consume();
                yield new Node.Var(t);
            }
            case ASTER -> {
                consume();
                yield new Node.Aster(t);
            }
            default -> null;
        };
    }

    private @NotNull Node parseSimpleExpr() throws ParseException {
        Node result = tryParseSimpleExpr();
        if (result == null) {
            Token t = peek();
            if (t == null) {
                @NotNull Token last = tokens.getLast();
                throw new ParseException(last, "Unexpected end of input, expected simple expression");
            }
            throw new ParseException(t, "Expected simple expression but got " + t.kind);
        }
        return result;
    }

    private @NotNull Token expectConsume(Token.Kind kind) throws ParseException {
        Token t = consume();
        if (t == null) {
            @NotNull Token last = tokens.getLast();
            throw new ParseException(last, "Unexpected end of input, expected " + kind);
        }
        if (t.kind != kind) {
            throw new ParseException(t, "Expected " + kind + " but got " + t.kind);
        }
        return t;
    }

    private Token expectConsume(Set<Token.Kind> kinds) throws ParseException {
        Token t = consume();
        String kindsString = buildKindsString(kinds);
        if (t == null) {
            @NotNull Token last = tokens.getLast();
            throw new ParseException(last, "Unexpected end of input, expected one of " + kindsString);
        }
        if (!kinds.contains(t.kind)) {
            throw new ParseException(t, "Expected one of " + kindsString + " but got " + t.kind);
        }
        return t;
    }

    private String buildKindsString(Set<Token.Kind> kinds) {
        Token.Kind[] kindsArray = kinds.toArray(new Token.Kind[0]);

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < kindsArray.length; i++) {
            sb.append(kindsArray[i]);
            if (i < kindsArray.length - 2) {
                sb.append(", ");
            } else if (i == kindsArray.length - 2) {
                sb.append(" or ");
            }
        }
        return sb.toString();
    }

    private @Nullable Token peek() {
        if (pos < tokens.size()) {
            return tokens.get(pos);
        }
        return null;
    }

    private @Nullable Token consume() {
        if (pos < tokens.size()) {
            return tokens.get(pos++);
        }
        return null;
    }

    private boolean check(@NotNull Token.Kind kind) {
        Token t = peek();
        return t != null && t.kind == kind;
    }

    private boolean check(@NotNull Set<Token.Kind> kinds) {
        Token t = peek();
        return t != null && kinds.contains(t.kind);
    }

    private final @NotNull ArrayList<Token> tokens;
    private int pos;

    private static final @NotNull Set<Token.Kind> DECL_KINDS = Set.of(
            Token.Kind.KW_AXIOM,
            Token.Kind.KW_DEFUN,
            Token.Kind.KW_CHECK
    );

    private static final @NotNull Set<Token.Kind> GENERIC_ARROW_KINDS = Set.of(
            Token.Kind.ARROW,
            Token.Kind.DOT,
            Token.Kind.COMMA
    );

    private static final @NotNull Set<Token.Kind> LAMBDA_ARROW_KINDS = Set.of(
            Token.Kind.ARROW,
            Token.Kind.DOT
    );
}
