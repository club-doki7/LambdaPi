package club.doki7.lambdapi.syntax;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

public class TestParse {
    // =================== 简单表达式测试 ===================

    @Test
    void testParseVariable() throws Parse.ParseException {
        Node result = parseExpr("x");
        Node expected = new Node.Var("x");
        Assertions.assertEquals(expected, result);
    }

    @Test
    void testParseAster() throws Parse.ParseException {
        Node result = parseExpr("*");
        Node expected = new Node.Aster();
        Assertions.assertEquals(expected, result);
    }

    @Test
    void testParseParenthesized() throws Parse.ParseException {
        Node result = parseExpr("(x)");
        Node expected = new Node.Var("x");
        Assertions.assertEquals(expected, result);
    }

    // =================== Lambda 表达式测试 ===================

    @Test
    void testParseLambdaSimple() throws Parse.ParseException {
        Node result = parseExpr("λx.x");
        Node expected = new Node.Lam("x", new Node.Var("x"));
        Assertions.assertEquals(expected, result);
    }

    @Test
    void testParseLambdaArrow() throws Parse.ParseException {
        Node result = parseExpr("λx -> x");
        Node expected = new Node.Lam("x", new Node.Var("x"));
        Assertions.assertEquals(expected, result);
    }

    @Test
    void testParseLambdaBackslash() throws Parse.ParseException {
        Node result = parseExpr("\\x.x");
        Node expected = new Node.Lam("x", new Node.Var("x"));
        Assertions.assertEquals(expected, result);
    }

    @Test
    void testParseLambdaNested() throws Parse.ParseException {
        Node result = parseExpr("λx.λy.x");
        Node expected = new Node.Lam("x", new Node.Lam("y", new Node.Var("x")));
        Assertions.assertEquals(expected, result);
    }

    // =================== 应用表达式测试 ===================

    @Test
    void testParseApplication() throws Parse.ParseException {
        Node result = parseExpr("f x");
        Node expected = new Node.App(new Node.Var("f"), new Node.Var("x"));
        Assertions.assertEquals(expected, result);
    }

    @Test
    void testParseApplicationLeftAssociative() throws Parse.ParseException {
        // f x y 应该解析为 (f x) y
        Node result = parseExpr("f x y");
        Node expected = new Node.App(
                new Node.App(new Node.Var("f"), new Node.Var("x")),
                new Node.Var("y")
        );
        Assertions.assertEquals(expected, result);
    }

    @Test
    void testParseApplicationWithParen() throws Parse.ParseException {
        Node result = parseExpr("f (g x)");
        Node expected = new Node.App(
                new Node.Var("f"),
                new Node.App(new Node.Var("g"), new Node.Var("x"))
        );
        Assertions.assertEquals(expected, result);
    }

    // =================== 函数类型测试 ===================

    @Test
    void testParseArrowType() throws Parse.ParseException {
        Node result = parseExpr("A -> B");
        Node expected = new Node.Pi((String) null, new Node.Var("A"), new Node.Var("B"));
        Assertions.assertEquals(expected, result);
    }

    @Test
    void testParseArrowTypeRightAssociative() throws Parse.ParseException {
        // A -> B -> C 应该解析为 A -> (B -> C)
        Node result = parseExpr("A -> B -> C");
        Node expected = new Node.Pi(
                (String) null,
                new Node.Var("A"),
                new Node.Pi((String) null, new Node.Var("B"), new Node.Var("C"))
        );
        Assertions.assertEquals(expected, result);
    }

    @Test
    void testParseArrowTypeUnicode() throws Parse.ParseException {
        Node result = parseExpr("A → B");
        Node expected = new Node.Pi((String) null, new Node.Var("A"), new Node.Var("B"));
        Assertions.assertEquals(expected, result);
    }

    // =================== Pi 类型测试 ===================

    @Test
    void testParsePiSimple() throws Parse.ParseException {
        Node result = parseExpr("Πx:*.x");
        Node expected = new Node.Pi("x", new Node.Aster(), new Node.Var("x"));
        Assertions.assertEquals(expected, result);
    }

    @Test
    void testParsePiForall() throws Parse.ParseException {
        Node result = parseExpr("forall x:A.B");
        Node expected = new Node.Pi("x", new Node.Var("A"), new Node.Var("B"));
        Assertions.assertEquals(expected, result);
    }

    @Test
    void testParsePiUnicode() throws Parse.ParseException {
        Node result = parseExpr("∀x:A→B");
        Node expected = new Node.Pi("x", new Node.Var("A"), new Node.Var("B"));
        Assertions.assertEquals(expected, result);
    }

    @Test
    void testParsePiWithParentheses() throws Parse.ParseException {
        // Π(x, y : A) -> B
        Node result = parseExpr("Π(x, y : A) -> B");
        Node expected = new Node.Pi(
                "x",
                new Node.Var("A"),
                new Node.Pi("y", new Node.Var("A"), new Node.Var("B"))
        );
        Assertions.assertEquals(expected, result);
    }

    // =================== 类型注解测试 ===================

    @Test
    void testParseAnnotation() throws Parse.ParseException {
        Node result = parseExpr("x : A");
        Node expected = new Node.Ann(new Node.Var("x"), new Node.Var("A"));
        Assertions.assertEquals(expected, result);
    }

    @Test
    void testParseAnnotationWithLambda() throws Parse.ParseException {
        Node result = parseExpr("λx.x : A -> A");
        Node expected = new Node.Ann(
                new Node.Lam("x", new Node.Var("x")),
                new Node.Pi((String) null, new Node.Var("A"), new Node.Var("A"))
        );
        Assertions.assertEquals(expected, result);
    }

    // =================== 复杂表达式测试 ===================

    @Test
    void testParseComplex1() throws Parse.ParseException {
        // (λx.x) y
        Node result = parseExpr("(λx.x) y");
        Node expected = new Node.App(
                new Node.Lam("x", new Node.Var("x")),
                new Node.Var("y")
        );
        Assertions.assertEquals(expected, result);
    }

    @Test
    void testParseComplex2() throws Parse.ParseException {
        // λf.λx.f (f x)
        Node result = parseExpr("λf.λx.f (f x)");
        Node expected = new Node.Lam("f",
                new Node.Lam("x",
                        new Node.App(
                                new Node.Var("f"),
                                new Node.App(new Node.Var("f"), new Node.Var("x"))
                        )
                )
        );
        Assertions.assertEquals(expected, result);
    }

    // =================== Program 解析测试 ===================

    @Test
    void testParseProgramAxiom() throws Parse.ParseException {
        PNode result = parseProgram("axiom Nat : *");
        PNode expected = new PNode.Program(List.of(
                new PNode.Axiom("Nat", new Node.Aster())
        ));
        Assertions.assertEquals(expected, result);
    }

    @Test
    void testParseProgramDefun() throws Parse.ParseException {
        PNode result = parseProgram("defun id = λx.x");
        PNode expected = new PNode.Program(List.of(
                new PNode.Defun("id", new Node.Lam("x", new Node.Var("x")))
        ));
        Assertions.assertEquals(expected, result);
    }

    @Test
    void testParseProgramExpr() throws Parse.ParseException {
        PNode result = parseProgram("check f x");
        PNode expected = new PNode.Program(List.of(
                new PNode.Check(new Node.App(new Node.Var("f"), new Node.Var("x")))
        ));
        Assertions.assertEquals(expected, result);
    }

    @Test
    void testParseProgramMultiple() throws Parse.ParseException {
        PNode result = parseProgram("axiom A : *\ndefun id = λx.x\ncheck id A");
        PNode expected = new PNode.Program(List.of(
                new PNode.Axiom("A", new Node.Aster()),
                new PNode.Defun("id", new Node.Lam("x", new Node.Var("x"))),
                new PNode.Check(new Node.App(new Node.Var("id"), new Node.Var("A")))
        ));
        Assertions.assertEquals(expected, result);
    }

    // =================== 错误处理测试 ===================

    @Test
    void testParseErrorUnexpectedEnd() {
        Assertions.assertThrows(Parse.ParseException.class, () -> parseExpr("λx."));
    }

    @Test
    void testParseErrorMissingRParen() {
        Assertions.assertThrows(Parse.ParseException.class, () -> parseExpr("(x"));
    }

    @Test
    void testParseErrorUnexpectedToken() {
        Assertions.assertThrows(Parse.ParseException.class, () -> parseExpr(")"));
    }

    // =================== 辅助方法 ===================

    private static Node parseExpr(String source) throws Parse.ParseException {
        ArrayList<Token> tokens = Token.tokenize(source);
        return Parse.parseExpr(tokens);
    }

    private static PNode parseProgram(String source) throws Parse.ParseException {
        ArrayList<Token> tokens = Token.tokenize(source);
        return Parse.parseProgram(tokens);
    }
}
