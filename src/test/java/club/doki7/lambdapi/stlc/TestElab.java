package club.doki7.lambdapi.stlc;

import club.doki7.lambdapi.common.Name;
import club.doki7.lambdapi.exc.ElabException;
import club.doki7.lambdapi.syntax.Node;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestElab {
    // =================== 变量测试 ===================

    @Test
    void testElabFreeVariable() throws ElabException {
        // 自由变量 x
        Node node = new Node.Var("x");
        Term result = Elab.elab(node);
        Term.Inferable expected = new Term.Free(node, new Name.Global("x"));
        assertEquals(expected, result);
    }

    // =================== 带类型注解的 Lambda 表达式测试 ===================

    @Test
    void testElabAnnotatedLambda() throws ElabException {
        // (λx. x) : A → A
        Node lamBody = new Node.Var("x");
        Node lam = new Node.Lam("x", lamBody);
        Node typeA = new Node.Var("A");
        Node funType = new Node.Pi((String) null, typeA, typeA);
        Node ann = new Node.Ann(lam, funType);

        Term result = Elab.elab(ann);

        Type expectedType = new Type.Fun(
                new Type.Free(new Name.Global("A")),
                new Type.Free(new Name.Global("A"))
        );
        // 注意：lambda 体中的 x 应该变成 Bound(0)
        Term.Checkable expectedBody = new Term.Inf(lamBody, new Term.Bound(lamBody, 0));
        Term.Checkable expectedLam = new Term.Lam(lam, expectedBody);
        Term.Inferable expected = new Term.Ann(ann, expectedLam, expectedType);

        assertEquals(expected, result);
    }

    @Test
    void testElabNestedAnnotatedLambda() throws ElabException {
        // (λx. λy. x) : A → B → A
        Node xVar = new Node.Var("x");
        Node innerLam = new Node.Lam("y", xVar);
        Node lam = new Node.Lam("x", innerLam);
        Node typeA = new Node.Var("A");
        Node typeB = new Node.Var("B");
        Node funType = new Node.Pi((String) null, typeA, new Node.Pi((String) null, typeB, typeA));
        Node ann = new Node.Ann(lam, funType);

        Term result = Elab.elab(ann);

        Type expectedType = new Type.Fun(
                new Type.Free(new Name.Global("A")),
                new Type.Fun(
                        new Type.Free(new Name.Global("B")),
                        new Type.Free(new Name.Global("A"))
                )
        );
        // x 在 innerLam 体中是 Bound(1)，因为 y 是 Bound(0)
        Term.Checkable expectedXVar = new Term.Inf(xVar, new Term.Bound(xVar, 1));
        Term.Checkable expectedInnerLam = new Term.Lam(innerLam, expectedXVar);
        Term.Checkable expectedLam = new Term.Lam(lam, expectedInnerLam);
        Term.Inferable expected = new Term.Ann(ann, expectedLam, expectedType);

        assertEquals(expected, result);
    }

    // =================== 应用表达式测试 ===================

    @Test
    void testElabApplication() throws ElabException {
        // f x
        Node fVar = new Node.Var("f");
        Node xVar = new Node.Var("x");
        Node app = new Node.App(fVar, xVar);

        Term result = Elab.elab(app);

        Term.Inferable expectedF = new Term.Free(fVar, new Name.Global("f"));
        Term.Checkable expectedX = new Term.Inf(xVar, new Term.Free(xVar, new Name.Global("x")));
        Term.Inferable expected = new Term.App(app, expectedF, expectedX);

        assertEquals(expected, result);
    }

    @Test
    void testElabNestedApplication() throws ElabException {
        // f x y = (f x) y
        Node fVar = new Node.Var("f");
        Node xVar = new Node.Var("x");
        Node yVar = new Node.Var("y");
        Node innerApp = new Node.App(fVar, xVar);
        Node app = new Node.App(innerApp, yVar);

        Term result = Elab.elab(app);

        Term.Inferable expectedF = new Term.Free(fVar, new Name.Global("f"));
        Term.Checkable expectedX = new Term.Inf(xVar, new Term.Free(xVar, new Name.Global("x")));
        Term.Inferable expectedInnerApp = new Term.App(innerApp, expectedF, expectedX);
        Term.Checkable expectedY = new Term.Inf(yVar, new Term.Free(yVar, new Name.Global("y")));
        Term.Inferable expected = new Term.App(app, expectedInnerApp, expectedY);

        assertEquals(expected, result);
    }

    // =================== De Bruijn 索引测试 ===================

    @Test
    void testElabBoundVariable() throws ElabException {
        // (λx. x) : A → A
        // x 应该变成 Bound(0)
        Node xVar = new Node.Var("x");
        Node lam = new Node.Lam("x", xVar);
        Node typeA = new Node.Var("A");
        Node funType = new Node.Pi((String) null, typeA, typeA);
        Node ann = new Node.Ann(lam, funType);

        Term result = Elab.elab(ann);

        // 验证体中的变量是 Bound(0)
        Term.Ann annTerm = (Term.Ann) result;
        Term.Lam lamTerm = (Term.Lam) annTerm.term();
        Term.Inf infTerm = (Term.Inf) lamTerm.body();
        Term.Bound boundTerm = (Term.Bound) infTerm.term();
        assertEquals(0, boundTerm.index());
    }

    @Test
    void testElabMixedBoundAndFree() throws ElabException {
        // (λx. f x) : A → B
        // f 是自由变量，x 是约束变量 Bound(0)
        Node fVar = new Node.Var("f");
        Node xVar = new Node.Var("x");
        Node appBody = new Node.App(fVar, xVar);
        Node lam = new Node.Lam("x", appBody);
        Node typeA = new Node.Var("A");
        Node typeB = new Node.Var("B");
        Node funType = new Node.Pi((String) null, typeA, typeB);
        Node ann = new Node.Ann(lam, funType);

        Term result = Elab.elab(ann);

        Type expectedType = new Type.Fun(
                new Type.Free(new Name.Global("A")),
                new Type.Free(new Name.Global("B"))
        );
        Term.Inferable expectedF = new Term.Free(fVar, new Name.Global("f"));
        Term.Checkable expectedX = new Term.Inf(xVar, new Term.Bound(xVar, 0));
        Term.Inferable expectedAppBody = new Term.App(appBody, expectedF, expectedX);
        Term.Checkable expectedApp = new Term.Inf(appBody, expectedAppBody);
        Term.Checkable expectedLam = new Term.Lam(lam, expectedApp);
        Term.Inferable expected = new Term.Ann(ann, expectedLam, expectedType);

        assertEquals(expected, result);
    }

    // =================== 类型精化测试 ===================

    @Test
    void testElabNestedFunctionType() throws ElabException {
        // x : (A → B) → C
        Node xVar = new Node.Var("x");
        Node typeA = new Node.Var("A");
        Node typeB = new Node.Var("B");
        Node typeC = new Node.Var("C");
        Node abType = new Node.Pi((String) null, typeA, typeB);
        Node fullType = new Node.Pi((String) null, abType, typeC);
        Node ann = new Node.Ann(xVar, fullType);

        Term result = Elab.elab(ann);

        Type expectedType = new Type.Fun(
                new Type.Fun(
                        new Type.Free(new Name.Global("A")),
                        new Type.Free(new Name.Global("B"))
                ),
                new Type.Free(new Name.Global("C"))
        );
        Term.Checkable expectedX = new Term.Inf(xVar, new Term.Free(xVar, new Name.Global("x")));
        Term.Inferable expected = new Term.Ann(ann, expectedX, expectedType);

        assertEquals(expected, result);
    }

    // =================== 错误处理测试 ===================

    @Test
    void testElabUnannotatedLambdaThrows() {
        // λx. x 没有类型注解，应该抛出异常
        Node lam = new Node.Lam("x", new Node.Var("x"));

        ElabException ex = assertThrows(ElabException.class, () -> Elab.elab(lam));
        assertTrue(ex.getMessage().contains("lambda expression must be annotated"));
    }

    @Test
    void testElabDependentPiThrows() {
        // Πx : A . B 是依值类型，STLC 不支持
        Node typeA = new Node.Var("A");
        Node typeB = new Node.Var("B");
        Node pi = new Node.Pi("x", typeA, typeB);

        ElabException ex = assertThrows(ElabException.class, () -> Elab.elab(pi));
        assertTrue(ex.getMessage().contains("dependent function types"));
    }

    @Test
    void testElabArrowAtTermLevelThrows() {
        // A → B 在词项层级应该抛出异常
        Node typeA = new Node.Var("A");
        Node typeB = new Node.Var("B");
        Node arrow = new Node.Pi((String) null, typeA, typeB);

        ElabException ex = assertThrows(ElabException.class, () -> Elab.elab(arrow));
        assertTrue(ex.getMessage().contains("function arrow is not allowed at term level"));
    }

    @Test
    void testElabAsterThrows() {
        // * 在 STLC 中不支持
        Node aster = new Node.Aster();

        ElabException ex = assertThrows(ElabException.class, () -> Elab.elab(aster));
        assertTrue(ex.getMessage().contains("type universes"));
    }

    @Test
    void testElabDependentPiInTypeThrows() {
        // x : (Πy : A . B)，类型中使用依值类型应该抛出异常
        Node xVar = new Node.Var("x");
        Node typeA = new Node.Var("A");
        Node typeB = new Node.Var("B");
        Node depPi = new Node.Pi("y", typeA, typeB);
        Node ann = new Node.Ann(xVar, depPi);

        ElabException ex = assertThrows(ElabException.class, () -> Elab.elab(ann));
        assertTrue(ex.getMessage().contains("dependent function types"));
    }

    @Test
    void testElabLambdaInTypeThrows() {
        // x : (λy. A)，类型中使用 lambda 应该抛出异常
        Node xVar = new Node.Var("x");
        Node typeA = new Node.Var("A");
        Node lamInType = new Node.Lam("y", typeA);
        Node ann = new Node.Ann(xVar, lamInType);

        ElabException ex = assertThrows(ElabException.class, () -> Elab.elab(ann));
        assertTrue(ex.getMessage().contains("lambda expression is not allowed at type/kind level"));
    }

    @Test
    void testElabAppInTypeThrows() {
        // x : (f A)，类型中使用应用应该抛出异常
        Node xVar = new Node.Var("x");
        Node fVar = new Node.Var("f");
        Node typeA = new Node.Var("A");
        Node appInType = new Node.App(fVar, typeA);
        Node ann = new Node.Ann(xVar, appInType);

        ElabException ex = assertThrows(ElabException.class, () -> Elab.elab(ann));
        assertTrue(ex.getMessage().contains("function application is not allowed at type/kind level"));
    }

    @Test
    void testElabAnnInTypeThrows() {
        // x : (A : *)，类型中使用类型注解应该抛出异常
        Node xVar = new Node.Var("x");
        Node typeA = new Node.Var("A");
        Node aster = new Node.Aster();
        Node annInType = new Node.Ann(typeA, aster);
        Node ann = new Node.Ann(xVar, annInType);

        ElabException ex = assertThrows(ElabException.class, () -> Elab.elab(ann));
        assertTrue(ex.getMessage().contains("type annotation is not allowed at type/kind level"));
    }

    @Test
    void testElabAsterInTypeThrows() {
        // x : *，STLC 不支持类型宇宙
        Node xVar = new Node.Var("x");
        Node aster = new Node.Aster();
        Node ann = new Node.Ann(xVar, aster);

        ElabException ex = assertThrows(ElabException.class, () -> Elab.elab(ann));
        assertTrue(ex.getMessage().contains("type universes"));
    }
}

