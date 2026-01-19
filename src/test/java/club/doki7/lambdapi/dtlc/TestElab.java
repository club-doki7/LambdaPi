package club.doki7.lambdapi.dtlc;

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
        Term result = new Elab().elab(node);
        Term.Inferable expected = new Term.Free(node, new Name.Global("x"));
        assertEquals(expected, result);
    }

    // =================== 类型宇宙测试 ===================

    @Test
    void testElabStar() throws ElabException {
        // *
        Node node = new Node.Aster();
        Term result = new Elab().elab(node);
        Term.Inferable expected = new Term.Star(node);
        assertEquals(expected, result);
    }

    @Test
    void testElabAnnotatedTypeVariable() throws ElabException {
        // A : *
        Node typeA = new Node.Var("A");
        Node aster = new Node.Aster();
        Node ann = new Node.Ann(typeA, aster);

        Term result = new Elab().elab(ann);

        Term.Checkable expectedA = new Term.Inf(typeA, new Term.Free(typeA, new Name.Global("A")));
        Term.Checkable expectedAster = new Term.Inf(aster, new Term.Star(aster));
        Term.Inferable expected = new Term.Ann(ann, expectedA, expectedAster);

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

        Term result = new Elab().elab(ann);

        // 注意：lambda 体中的 x 应该变成 Bound(0)
        Term.Checkable expectedBody = new Term.Inf(lamBody, new Term.Bound(lamBody, 0));
        Term.Checkable expectedLam = new Term.Lam(lam, expectedBody);

        Term.Checkable expectedTypeA = new Term.Inf(typeA, new Term.Free(typeA, new Name.Global("A")));
        Term.Checkable expectedFunType = new Term.Inf(funType, new Term.Pi(funType, expectedTypeA, expectedTypeA));

        Term.Inferable expected = new Term.Ann(ann, expectedLam, expectedFunType);

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

        Term result = new Elab().elab(ann);

        // x 在 innerLam 体中是 Bound(1)，因为 y 是 Bound(0)
        Term.Checkable expectedXVar = new Term.Inf(xVar, new Term.Bound(xVar, 1));
        Term.Checkable expectedInnerLam = new Term.Lam(innerLam, expectedXVar);
        Term.Checkable expectedLam = new Term.Lam(lam, expectedInnerLam);

        // 构建类型
        Term.Checkable expectedTypeA = new Term.Inf(typeA, new Term.Free(typeA, new Name.Global("A")));
        Term.Checkable expectedTypeB = new Term.Inf(typeB, new Term.Free(typeB, new Name.Global("B")));
        Node innerFunType = new Node.Pi((String) null, typeB, typeA);
        Term.Checkable expectedInnerFunType = new Term.Inf(innerFunType, new Term.Pi(innerFunType, expectedTypeB, expectedTypeA));
        Term.Checkable expectedFunType = new Term.Inf(funType, new Term.Pi(funType, expectedTypeA, expectedInnerFunType));

        Term.Inferable expected = new Term.Ann(ann, expectedLam, expectedFunType);

        assertEquals(expected, result);
    }

    // =================== 应用表达式测试 ===================

    @Test
    void testElabApplication() throws ElabException {
        // f x
        Node fVar = new Node.Var("f");
        Node xVar = new Node.Var("x");
        Node app = new Node.App(fVar, xVar);

        Term result = new Elab().elab(app);

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

        Term result = new Elab().elab(app);

        Term.Inferable expectedF = new Term.Free(fVar, new Name.Global("f"));
        Term.Checkable expectedX = new Term.Inf(xVar, new Term.Free(xVar, new Name.Global("x")));
        Term.Inferable expectedInnerApp = new Term.App(innerApp, expectedF, expectedX);
        Term.Checkable expectedY = new Term.Inf(yVar, new Term.Free(yVar, new Name.Global("y")));
        Term.Inferable expected = new Term.App(app, expectedInnerApp, expectedY);

        assertEquals(expected, result);
    }

    // =================== 依值函数类型（Pi 类型）测试 ===================

    @Test
    void testElabSimplePiType() throws ElabException {
        // ∀(x : A) → B  (简单箭头类型 A → B)
        Node typeA = new Node.Var("A");
        Node typeB = new Node.Var("B");
        Node pi = new Node.Pi((String) null, typeA, typeB);

        Term result = new Elab().elab(pi);

        Term.Checkable expectedA = new Term.Inf(typeA, new Term.Free(typeA, new Name.Global("A")));
        Term.Checkable expectedB = new Term.Inf(typeB, new Term.Free(typeB, new Name.Global("B")));
        Term.Inferable expected = new Term.Pi(pi, expectedA, expectedB);

        assertEquals(expected, result);
    }

    @Test
    void testElabDependentPiType() throws ElabException {
        // ∀(x : A) → B，其中 B 中可能使用 x（虽然此例中没有使用）
        Node typeA = new Node.Var("A");
        Node typeB = new Node.Var("B");
        Node pi = new Node.Pi("x", typeA, typeB);

        Term result = new Elab().elab(pi);

        Term.Checkable expectedA = new Term.Inf(typeA, new Term.Free(typeA, new Name.Global("A")));
        // B 是自由变量（在这个例子中），因为我们没有引用 x
        Term.Checkable expectedB = new Term.Inf(typeB, new Term.Free(typeB, new Name.Global("B")));
        Term.Inferable expected = new Term.Pi(pi, expectedA, expectedB);

        assertEquals(expected, result);
    }

    @Test
    void testElabDependentPiTypeWithBoundVar() throws ElabException {
        // ∀(x : A) → x，其中 x 被引用
        Node typeA = new Node.Var("A");
        Node xVar = new Node.Var("x");
        Node pi = new Node.Pi("x", typeA, xVar);

        Term result = new Elab().elab(pi);

        Term.Checkable expectedA = new Term.Inf(typeA, new Term.Free(typeA, new Name.Global("A")));
        // x 应该变成 Bound(0)，因为它被 Pi 绑定
        Term.Checkable expectedX = new Term.Inf(xVar, new Term.Bound(xVar, 0));
        Term.Inferable expected = new Term.Pi(pi, expectedA, expectedX);

        assertEquals(expected, result);
    }

    @Test
    void testElabNestedPiType() throws ElabException {
        // ∀(A : *) → ∀(x : A) → A
        Node aster = new Node.Aster();
        Node typeA2 = new Node.Var("A");
        Node typeA3 = new Node.Var("A");
        Node innerPi = new Node.Pi("x", typeA2, typeA3);
        Node outerPi = new Node.Pi("A", aster, innerPi);

        Term result = new Elab().elab(outerPi);

        Term.Checkable expectedAster = new Term.Inf(aster, new Term.Star(aster));
        // 内层 Pi：∀(x : A) → A
        // 其中第一个 A 是 Bound(0)，第二个 A 是 Bound(1)，都指向最外层的 Pi 引入的 A
        Term.Checkable expectedA2 = new Term.Inf(typeA2, new Term.Bound(typeA2, 0));
        Term.Checkable expectedA3 = new Term.Inf(typeA3, new Term.Bound(typeA3, 1));
        Term.Checkable expectedInnerPi = new Term.Inf(innerPi, new Term.Pi(innerPi, expectedA2, expectedA3));
        Term.Inferable expected = new Term.Pi(outerPi, expectedAster, expectedInnerPi);

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

        Term result = new Elab().elab(ann);

        // 验证体中的变量是 Bound(0)
        Term.Ann annTerm = (Term.Ann) result;
        Term.Lam lamTerm = (Term.Lam) annTerm.term();
        Term.Inf infTerm = (Term.Inf) lamTerm.body();
        Term.Bound boundTerm = (Term.Bound) infTerm.inferable();
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

        Term result = new Elab().elab(ann);

        Term.Inferable expectedF = new Term.Free(fVar, new Name.Global("f"));
        Term.Checkable expectedX = new Term.Inf(xVar, new Term.Bound(xVar, 0));
        Term.Inferable expectedAppBody = new Term.App(appBody, expectedF, expectedX);
        Term.Checkable expectedApp = new Term.Inf(appBody, expectedAppBody);
        Term.Checkable expectedLam = new Term.Lam(lam, expectedApp);

        Term.Checkable expectedTypeA = new Term.Inf(typeA, new Term.Free(typeA, new Name.Global("A")));
        Term.Checkable expectedTypeB = new Term.Inf(typeB, new Term.Free(typeB, new Name.Global("B")));
        Term.Checkable expectedFunType = new Term.Inf(funType, new Term.Pi(funType, expectedTypeA, expectedTypeB));

        Term.Inferable expected = new Term.Ann(ann, expectedLam, expectedFunType);

        assertEquals(expected, result);
    }

    @Test
    void testElabDeBruijnInPiType() throws ElabException {
        // ∀(x : A) → ∀(y : x) → x
        // 第二个 x 应该是 Bound(0)（指向第一个 x）
        // 第三个 x 应该是 Bound(1)（指向第一个 x，因为 y 占据了 Bound(0)）
        Node typeA = new Node.Var("A");
        Node x1 = new Node.Var("x");
        Node x2 = new Node.Var("x");
        Node innerPi = new Node.Pi("y", x1, x2);
        Node outerPi = new Node.Pi("x", typeA, innerPi);

        Term result = new Elab().elab(outerPi);

        Term.Checkable expectedA = new Term.Inf(typeA, new Term.Free(typeA, new Name.Global("A")));
        // 内层 Pi 体中：x1 是 Bound(0)，x2 是 Bound(1)
        Term.Checkable expectedX1 = new Term.Inf(x1, new Term.Bound(x1, 0));
        Term.Checkable expectedX2 = new Term.Inf(x2, new Term.Bound(x2, 1));
        Term.Checkable expectedInnerPi = new Term.Inf(innerPi, new Term.Pi(innerPi, expectedX1, expectedX2));
        Term.Inferable expected = new Term.Pi(outerPi, expectedA, expectedInnerPi);

        assertEquals(expected, result);
    }

    // =================== 复杂表达式测试 ===================

    @Test
    void testElabPolymorphicIdentity() throws ElabException {
        // (λA. λx. x) : ∀(A : *) → ∀(x : A) → A
        Node xVar = new Node.Var("x");
        Node innerLam = new Node.Lam("x", xVar);
        Node outerLam = new Node.Lam("A", innerLam);

        // 类型：∀(A : *) → ∀(x : A) → A
        Node aster = new Node.Aster();
        Node typeA3 = new Node.Var("A");
        Node typeA4 = new Node.Var("A");
        Node innerPi = new Node.Pi("x", typeA3, typeA4);
        Node outerPi = new Node.Pi("A", aster, innerPi);

        Node ann = new Node.Ann(outerLam, outerPi);

        Term result = new Elab().elab(ann);

        // 外层 lambda 体：λx. x，其中 x 是 Bound(0)
        Term.Checkable expectedXBody = new Term.Inf(xVar, new Term.Bound(xVar, 0));
        Term.Checkable expectedInnerLam = new Term.Lam(innerLam, expectedXBody);
        Term.Checkable expectedOuterLam = new Term.Lam(outerLam, expectedInnerLam);

        // 类型：∀(A : *) → ∀(x : A) → A
        Term.Checkable expectedAster = new Term.Inf(aster, new Term.Star(aster));
        Term.Checkable expectedA3 = new Term.Inf(typeA3, new Term.Bound(typeA3, 0));
        Term.Checkable expectedA4 = new Term.Inf(typeA4, new Term.Bound(typeA4, 1));
        Term.Checkable expectedInnerPi = new Term.Inf(innerPi, new Term.Pi(innerPi, expectedA3, expectedA4));
        Term.Checkable expectedOuterPi = new Term.Inf(outerPi, new Term.Pi(outerPi, expectedAster, expectedInnerPi));

        Term.Inferable expected = new Term.Ann(ann, expectedOuterLam, expectedOuterPi);

        assertEquals(expected, result);
    }

    @Test
    void testElabTypeConstructor() throws ElabException {
        // (λA. A → A) : * → *
        Node typeA1 = new Node.Var("A");
        Node typeA2 = new Node.Var("A");
        Node arrow = new Node.Pi((String) null, typeA1, typeA2);
        Node lam = new Node.Lam("A", arrow);

        Node aster1 = new Node.Aster();
        Node aster2 = new Node.Aster();
        Node funType = new Node.Pi((String) null, aster1, aster2);
        Node ann = new Node.Ann(lam, funType);

        Term result = new Elab().elab(ann);

        // Lambda 体：A → A，实质上是 ∀(_ : A) → A。因为 ∀ 引入了额外的一层绑定，所以最内层的 A 变成 Bound(1)
        Term.Checkable expectedA1 = new Term.Inf(typeA1, new Term.Bound(typeA1, 0));
        Term.Checkable expectedA2 = new Term.Inf(typeA2, new Term.Bound(typeA2, 1));
        Term.Checkable expectedArrow = new Term.Inf(arrow, new Term.Pi(arrow, expectedA1, expectedA2));
        Term.Checkable expectedLam = new Term.Lam(lam, expectedArrow);

        // 类型：* → *
        Term.Checkable expectedAster1 = new Term.Inf(aster1, new Term.Star(aster1));
        Term.Checkable expectedAster2 = new Term.Inf(aster2, new Term.Star(aster2));
        Term.Checkable expectedFunType = new Term.Inf(funType, new Term.Pi(funType, expectedAster1, expectedAster2));

        Term.Inferable expected = new Term.Ann(ann, expectedLam, expectedFunType);

        assertEquals(expected, result);
    }

    // =================== 匿名 Pi 类型测试 ===================

    @Test
    void testElabAnonymousPi() throws ElabException {
        // ∀(_ : A) → B，参数为 null（匿名）
        Node typeA = new Node.Var("A");
        Node typeB = new Node.Var("B");
        Node pi = new Node.Pi((String) null, typeA, typeB);

        Term result = new Elab().elab(pi);

        Term.Checkable expectedA = new Term.Inf(typeA, new Term.Free(typeA, new Name.Global("A")));
        Term.Checkable expectedB = new Term.Inf(typeB, new Term.Free(typeB, new Name.Global("B")));
        Term.Inferable expected = new Term.Pi(pi, expectedA, expectedB);

        assertEquals(expected, result);
    }

    // =================== 错误处理测试 ===================

    @Test
    void testElabUnannotatedLambdaThrows() {
        // λx. x 没有类型注解，应该抛出异常
        Node lam = new Node.Lam("x", new Node.Var("x"));

        ElabException ex = assertThrows(ElabException.class, () -> new Elab().elab(lam));
        assertTrue(ex.getMessage().contains("lambda expression must be annotated"));
    }

    @Test
    void testElabUnannotatedNestedLambdaThrows() {
        // (λx. λy. x) 外层没有类型注解，应该抛出异常
        Node lam = new Node.Lam("x", new Node.Lam("y", new Node.Var("x")));

        ElabException ex = assertThrows(ElabException.class, () -> new Elab().elab(lam));
        assertTrue(ex.getMessage().contains("lambda expression must be annotated"));
    }
}

