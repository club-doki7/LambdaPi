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
}
