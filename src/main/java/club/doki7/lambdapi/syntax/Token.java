package club.doki7.lambdapi.syntax;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

public final class Token {
    public enum Kind {
        /// 标识符
        IDENT,
        /// 左括号
        LPAREN,
        /// 右括号
        RPAREN,
        /// `λ` 或者 `\`
        LAMBDA,
        /// `->` 和 `→`
        ARROW,
        /// `.`
        DOT,
        /// `,`
        COMMA,
        /// `*`
        ASTER,
        /// `Π`, `∀` 或者 `forall`
        PI,
        /// `:`, `::`, `<:`, `∈` 或者 `in`
        COLON,

        /// 外围语言 (PNode 所定义) 所用的一些关键字
        KW_AXIOM,
        KW_CLAIM,
        DEFUN,
        KW_SORRY
    }

    public final Kind kind;
    public final String lexeme;
    public final int line;
    public final int col;

    public Token(@NotNull Kind kind,
                 @NotNull String lexeme,
                 int line,
                 int col) {
        this.kind = kind;
        this.lexeme = lexeme;
        this.line = line;
        this.col = col;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Token other = (Token) obj;
        if (this.kind != other.kind) return false;
        if (this.kind == Kind.IDENT) {
            return this.lexeme.equals(other.lexeme);
        } else {
            return true;
        }
    }

    @Override
    public @NotNull String toString() {
        return lexeme;
    }

    @Override
    public int hashCode() {
        if (this.kind == Kind.IDENT) {
            return Objects.hash(kind, lexeme);
        } else {
            // Token(COLON, "::") == Token(COLON, ":") 但 Token(COLON, ":") != ":"
            // 所以用第二个空字符串来区分 Token 和 String
            // 虽然按理说不会有人把 Token 和 String 装进同一个 HashMap
            return Objects.hash(kind, "");
        }
    }

    public static @NotNull Token ident(@NotNull String lexeme) {
        return new Token(Kind.IDENT, lexeme, -1, -1);
    }

    public static @NotNull Token symbol(@NotNull Kind kind) {
        return new Token(kind, switch (kind) {
            case LPAREN -> "(";
            case RPAREN -> ")";
            case LAMBDA -> "λ";
            case ARROW -> "→";
            case DOT -> ".";
            case COMMA -> ",";
            case ASTER -> "*";
            case PI -> "Π";
            case COLON -> ":";
            case IDENT -> throw new IllegalArgumentException("IDENT token requires a lexeme");
            case KW_AXIOM -> "axiom";
            case KW_CLAIM -> "claim";
            case DEFUN -> "defun";
            case KW_SORRY -> "sorry";
        }, -1, -1);
    }

    public static @NotNull ArrayList<Token> tokenize(@NotNull String input) {
        TokenizeContext ctx = new TokenizeContext();
        ctx.tokenize(input);
        return ctx.tokens;
    }

    private static class TokenizeContext {
        private final ArrayList<Token> tokens = new ArrayList<>();
        private final StringBuilder currentToken = new StringBuilder();
        private int line = 1;
        private int col = 1;

        private void tokenize(String input) {
            char[] charArray = input.toCharArray();
            for (int i = 0; i < charArray.length; i++) {
                char c = charArray[i];
                switch (c) {
                    case ' ', '\t', '\r', '\f' -> {
                        concludeToken();
                        col++;
                    }
                    case '\n' -> {
                        concludeToken();
                        line++;
                        col = 1;
                    }
                    case '(' -> {
                        concludeToken();
                        tokens.add(new Token(Kind.LPAREN, "(", line, col));
                        col++;
                    }
                    case ')' -> {
                        concludeToken();
                        tokens.add(new Token(Kind.RPAREN, ")", line, col));
                        col++;
                    }
                    case 'λ', '\\' -> {
                        concludeToken();
                        tokens.add(new Token(Kind.LAMBDA, String.valueOf(c), line, col));
                        col++;
                    }
                    case '-' -> {
                        if (i + 1 < charArray.length) {
                            if (charArray[i + 1] == '>') {
                                concludeToken();
                                tokens.add(new Token(Kind.ARROW, "->", line, col));
                                i++;
                                col += 2;
                                continue;
                            } else if (charArray[i + 1] == '-') {
                                concludeToken();
                                i += 2;
                                col += 2;
                                while (i < charArray.length) {
                                    char cc = charArray[i];
                                    if (cc == '\n') {
                                        line++;
                                        col = 1;
                                        break;
                                    } else {
                                        i++;
                                        col++;
                                    }
                                }
                                continue;
                            }
                        }

                        currentToken.append(c);
                        col++;
                    }
                    case '→' -> {
                        concludeToken();
                        tokens.add(new Token(Kind.ARROW, String.valueOf(c), line, col));
                        col++;
                    }
                    case '.' -> {
                        concludeToken();
                        tokens.add(new Token(Kind.DOT, ".", line, col));
                        col++;
                    }
                    case ',' -> {
                        concludeToken();
                        tokens.add(new Token(Kind.COMMA, ",", line, col));
                        col++;
                    }
                    case ':' -> {
                        concludeToken();
                        if (i + 1 < charArray.length && charArray[i + 1] == ':') {
                            tokens.add(new Token(Kind.COLON, "::", line, col));
                            i++;
                            col += 2;
                        } else {
                            tokens.add(new Token(Kind.COLON, ":", line, col));
                            col++;
                        }
                    }
                    case '∈' -> {
                        concludeToken();
                        tokens.add(new Token(Kind.COLON, String.valueOf(c), line, col));
                        col++;
                    }
                    case '<' -> {
                        if (i + 1 < charArray.length && charArray[i + 1] == ':') {
                            concludeToken();
                            tokens.add(new Token(Kind.COLON, "<:", line, col));
                            i++;
                            col += 2;
                        } else {
                            currentToken.append(c);
                            col++;
                        }
                    }
                    case '*' -> {
                        concludeToken();
                        tokens.add(new Token(Kind.ASTER, "*", line, col));
                        col++;
                    }
                    case 'Π', '∀' -> {
                        concludeToken();
                        tokens.add(new Token(Kind.PI, String.valueOf(c), line, col));
                        col++;
                    }
                    default -> {
                        currentToken.append(c);
                        col++;
                    }
                }
            }
            concludeToken();
        }

        private void concludeToken() {
            if (currentToken.isEmpty()) {
                return;
            }
            String lexeme = currentToken.toString();
            if (KEYWORDS.containsKey(lexeme)) {
                tokens.add(new Token(KEYWORDS.get(lexeme), lexeme, line, col - lexeme.length()));
                currentToken.setLength(0);
                return;
            }

            tokens.add(new Token(Kind.IDENT, lexeme, line, col - lexeme.length()));
            currentToken.setLength(0);
        }

        private static final @NotNull HashMap<@NotNull String, @NotNull Kind> KEYWORDS;
        static {
            KEYWORDS = new HashMap<>();
            KEYWORDS.put("forall", Kind.PI);
            KEYWORDS.put("in", Kind.COLON);
            KEYWORDS.put("axiom", Kind.KW_AXIOM);
            KEYWORDS.put("postulate", Kind.KW_AXIOM);
            KEYWORDS.put("claim", Kind.KW_CLAIM);
            KEYWORDS.put("declare", Kind.KW_CLAIM);
            KEYWORDS.put("defun", Kind.DEFUN);
            KEYWORDS.put("define", Kind.DEFUN);
            KEYWORDS.put("sorry", Kind.KW_SORRY);
            KEYWORDS.put("gomenasai", Kind.KW_SORRY);
            KEYWORDS.put("QaQ", Kind.KW_SORRY);
        }
    }
}
