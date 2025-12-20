package club.doki7.lambdapi.syntax;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

public class TestTokenize {
    @Test
    void testSimple() {
        String idSource = "λx.x";
        List<Token> tokens = Token.tokenize(idSource);
        List<Token> expectedTokens = List.of(
                Token.symbol(Token.Kind.LAMBDA),
                Token.ident("x"),
                Token.symbol(Token.Kind.ARROW),
                Token.ident("x")
        );
        Assertions.assertEquals(expectedTokens, tokens);
    }

    @Test
    void testAsciiSyntax() {
        String source = "\\x -> x";
        List<Token> tokens = Token.tokenize(source);
        List<Token> expectedTokens = List.of(
                Token.symbol(Token.Kind.LAMBDA),
                Token.ident("x"),
                Token.symbol(Token.Kind.ARROW),
                Token.ident("x")
        );
        Assertions.assertEquals(expectedTokens, tokens);
    }

    @Test
    void testPiType() {
        String source = "Πx:*.x";
        List<Token> tokens = Token.tokenize(source);
        List<Token> expectedTokens = List.of(
                Token.symbol(Token.Kind.PI),
                Token.ident("x"),
                Token.symbol(Token.Kind.COLON),
                Token.symbol(Token.Kind.ASTER),
                Token.symbol(Token.Kind.ARROW),
                Token.ident("x")
        );
        Assertions.assertEquals(expectedTokens, tokens);
    }

    @Test
    void testComplex() {
        String source = "(λx:A. x) y";
        List<Token> tokens = Token.tokenize(source);
        List<Token> expectedTokens = List.of(
                Token.symbol(Token.Kind.LPAREN),
                Token.symbol(Token.Kind.LAMBDA),
                Token.ident("x"),
                Token.symbol(Token.Kind.COLON),
                Token.ident("A"),
                Token.symbol(Token.Kind.ARROW),
                Token.ident("x"),
                Token.symbol(Token.Kind.RPAREN),
                Token.ident("y")
        );
        Assertions.assertEquals(expectedTokens, tokens);
    }

    @Test
    void testAlternativeSymbols() {
        String source = "∀x . x";
        List<Token> tokens = Token.tokenize(source);
        List<Token> expectedTokens = List.of(
                Token.symbol(Token.Kind.PI),
                Token.ident("x"),
                Token.symbol(Token.Kind.ARROW),
                Token.ident("x")
        );
        Assertions.assertEquals(expectedTokens, tokens);
    }
}
