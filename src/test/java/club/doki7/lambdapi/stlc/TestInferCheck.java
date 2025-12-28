package club.doki7.lambdapi.stlc;

import club.doki7.lambdapi.exc.TypeCheckException;
import club.doki7.lambdapi.syntax.Node;
import club.doki7.lambdapi.syntax.Parse;
import club.doki7.lambdapi.syntax.Token;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class TestInferCheck {
    private Map<String, InferCheck.Kind> globals;

    @BeforeEach
    void setUp() {
        globals = new HashMap<>();
        // 注册一些基本类型
        globals.put("Int", new InferCheck.HasKind());
        globals.put("Bool", new InferCheck.HasKind());
        globals.put("String", new InferCheck.HasKind());
    }

    // 辅助方法：解析表达式，elaborate，然后类型推断
    private Type infer(String code) throws Exception {
        Node node = Parse.parseExpr(Token.tokenize(code));
        Term term = Elab.elab(node);
        return InferCheck.infer(globals, (Term.Inferable) term);
    }

    // =================== 简单变量测试 ===================

    @Test
    void testInferGlobalVariable() throws Exception {
        // 注册一个全局变量 x : Int
        globals.put("x", new InferCheck.HasType(new Type.Free(new Name.Global("Int"))));

        Type result = infer("x");
        assertEquals(new Type.Free(new Name.Global("Int")), result);
    }

    @Test
    void testInferUndefinedVariable() {
        // 未定义的变量应该抛出异常
        assertThrows(TypeCheckException.class, () -> infer("undefined"));
    }

    // =================== 带类型注解的 Lambda 表达式测试 ===================

    @Test
    void testInferIdentityFunction() throws Exception {
        // (λx. x) : Int -> Int
        Type result = infer("(\\x. x) : Int -> Int");
        Type expected = new Type.Fun(
                new Type.Free(new Name.Global("Int")),
                new Type.Free(new Name.Global("Int"))
        );
        assertEquals(expected, result);
    }

    @Test
    void testInferPolymorphicIdentity() throws Exception {
        // 对于不同的类型，identity 函数都能通过类型检查
        Type resultInt = infer("(\\x. x) : Int -> Int");
        Type resultBool = infer("(\\x. x) : Bool -> Bool");

        assertEquals(
                new Type.Fun(
                        new Type.Free(new Name.Global("Int")),
                        new Type.Free(new Name.Global("Int"))
                ),
                resultInt
        );
        assertEquals(
                new Type.Fun(
                        new Type.Free(new Name.Global("Bool")),
                        new Type.Free(new Name.Global("Bool"))
                ),
                resultBool
        );
    }

    @Test
    void testInferConstFunction() throws Exception {
        // (λx. λy. x) : Int -> Bool -> Int
        Type result = infer("(\\x. \\y. x) : Int -> Bool -> Int");
        Type expected = new Type.Fun(
                new Type.Free(new Name.Global("Int")),
                new Type.Fun(
                        new Type.Free(new Name.Global("Bool")),
                        new Type.Free(new Name.Global("Int"))
                )
        );
        assertEquals(expected, result);
    }

    // =================== 函数应用测试 ===================

    @Test
    void testInferFunctionApplication() throws Exception {
        // 注册 f : Int -> Bool, x : Int
        globals.put("f", new InferCheck.HasType(
                new Type.Fun(
                        new Type.Free(new Name.Global("Int")),
                        new Type.Free(new Name.Global("Bool"))
                )
        ));
        globals.put("x", new InferCheck.HasType(new Type.Free(new Name.Global("Int"))));

        // f x 的类型应该是 Bool
        Type result = infer("f x");
        assertEquals(new Type.Free(new Name.Global("Bool")), result);
    }

    @Test
    void testInferNestedApplication() throws Exception {
        // 注册 f : Int -> Int -> Bool, x : Int, y : Int
        globals.put("f", new InferCheck.HasType(
                new Type.Fun(
                        new Type.Free(new Name.Global("Int")),
                        new Type.Fun(
                                new Type.Free(new Name.Global("Int")),
                                new Type.Free(new Name.Global("Bool"))
                        )
                )
        ));
        globals.put("x", new InferCheck.HasType(new Type.Free(new Name.Global("Int"))));
        globals.put("y", new InferCheck.HasType(new Type.Free(new Name.Global("Int"))));

        // f x y 的类型应该是 Bool
        Type result = infer("f x y");
        assertEquals(new Type.Free(new Name.Global("Bool")), result);
    }

    @Test
    void testInferApplicationWithLambda() throws Exception {
        // 注册 x : Int
        globals.put("x", new InferCheck.HasType(new Type.Free(new Name.Global("Int"))));

        // ((λf. f x) : (Int -> Bool) -> Bool) 应用一个函数
        Type result = infer("(\\f. f x) : (Int -> Bool) -> Bool");
        Type expected = new Type.Fun(
                new Type.Fun(
                        new Type.Free(new Name.Global("Int")),
                        new Type.Free(new Name.Global("Bool"))
                ),
                new Type.Free(new Name.Global("Bool"))
        );
        assertEquals(expected, result);
    }

    // =================== 类型错误测试 ===================

    @Test
    void testTypeErrorMismatch() {
        // 注册 f : Int -> Bool, x : Bool (不是 Int)
        globals.put("f", new InferCheck.HasType(
                new Type.Fun(
                        new Type.Free(new Name.Global("Int")),
                        new Type.Free(new Name.Global("Bool"))
                )
        ));
        globals.put("x", new InferCheck.HasType(new Type.Free(new Name.Global("Bool"))));

        // f x 应该失败，因为 x 是 Bool 而不是 Int
        assertThrows(TypeCheckException.class, () -> infer("f x"));
    }

    @Test
    void testTypeErrorNotFunction() {
        // 注册 x : Int
        globals.put("x", new InferCheck.HasType(new Type.Free(new Name.Global("Int"))));

        // x x 应该失败，因为 Int 不是函数类型
        assertThrows(TypeCheckException.class, () -> infer("x x"));
    }

    @Test
    void testTypeErrorUndefinedType() {
        // 使用未定义的类型 UndefinedType
        assertThrows(TypeCheckException.class, () -> infer("(\\x. x) : UndefinedType -> UndefinedType"));
    }

    @Test
    void testTypeErrorLambdaBodyMismatch() {
        // 注册 x : Bool
        globals.put("x", new InferCheck.HasType(new Type.Free(new Name.Global("Bool"))));

        // (λy. x) : Int -> Int 应该失败，因为 x 是 Bool 而不是 Int
        assertThrows(TypeCheckException.class, () -> infer("(\\y. x) : Int -> Int"));
    }

    // =================== 高阶函数测试 ===================

    @Test
    void testInferHigherOrderFunction() throws Exception {
        // (λf. λx. f x) : (Int -> Bool) -> Int -> Bool
        Type result = infer("(\\f. \\x. f x) : (Int -> Bool) -> Int -> Bool");
        Type int2Bool = new Type.Fun(
                new Type.Free(new Name.Global("Int")),
                new Type.Free(new Name.Global("Bool"))
        );
        Type expected = new Type.Fun(int2Bool, int2Bool);
        assertEquals(expected, result);
    }

    @Test
    void testInferCompose() throws Exception {
        // (λf. λg. λx. f (g x)) : (Bool -> String) -> (Int -> Bool) -> Int -> String
        Type result = infer("(\\f. \\g. \\x. f (g x)) : (Bool -> String) -> (Int -> Bool) -> Int -> String");
        Type expected = new Type.Fun(
                new Type.Fun(
                        new Type.Free(new Name.Global("Bool")),
                        new Type.Free(new Name.Global("String"))
                ),
                new Type.Fun(
                        new Type.Fun(
                                new Type.Free(new Name.Global("Int")),
                                new Type.Free(new Name.Global("Bool"))
                        ),
                        new Type.Fun(
                                new Type.Free(new Name.Global("Int")),
                                new Type.Free(new Name.Global("String"))
                        )
                )
        );
        assertEquals(expected, result);
    }

    // =================== 复杂类型注解测试 ===================

    @Test
    void testInferFlip() throws Exception {
        // (λf. λx. λy. f y x) : (Int -> Bool -> String) -> Bool -> Int -> String
        Type result = infer("(\\f. \\x. \\y. f y x) : (Int -> Bool -> String) -> Bool -> Int -> String");
        Type expected = new Type.Fun(
                new Type.Fun(
                        new Type.Free(new Name.Global("Int")),
                        new Type.Fun(
                                new Type.Free(new Name.Global("Bool")),
                                new Type.Free(new Name.Global("String"))
                        )
                ),
                new Type.Fun(
                        new Type.Free(new Name.Global("Bool")),
                        new Type.Fun(
                                new Type.Free(new Name.Global("Int")),
                                new Type.Free(new Name.Global("String"))
                        )
                )
        );
        assertEquals(expected, result);
    }
}

