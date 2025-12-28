package club.doki7.lambdapi;

import club.doki7.lambdapi.exc.ElabException;
import club.doki7.lambdapi.exc.LPiException;
import club.doki7.lambdapi.exc.ParseException;
import club.doki7.lambdapi.exc.TypeCheckException;
import club.doki7.lambdapi.stlc.*;
import club.doki7.lambdapi.syntax.Node;
import club.doki7.lambdapi.syntax.PNode;
import club.doki7.lambdapi.syntax.Parse;
import club.doki7.lambdapi.syntax.Token;

import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public final class Application {
    static void main() {
        HashMap<String, Value> env = new HashMap<>();
        HashMap<String, InferCheck.Kind> typeContext = new HashMap<>();

        System.out.println("=== Simply Typed Lambda Calculus ===");
        System.out.println("Commands:");
        System.out.println("  axiom <name> : <type>     - Declare axiom with type");
        System.out.println("  defun <name> = <expr>     - Define function");
        System.out.println("  check <expr>              - Type check and evaluate expression");
        System.out.println("  <expr>                    - Type check and evaluate expression");
        System.out.println("  :quit                     - Exit REPL");
        System.out.println();

        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.print("> ");
            if (!scanner.hasNextLine()) {
                break;
            }

            String line = scanner.nextLine().trim();

            if (line.isEmpty()) {
                continue;
            }

            if (line.equals(":quit") || line.equals(":q")) {
                System.out.println("Goodbye!");
                break;
            }

            try {
                processInput(line, env, typeContext);
            } catch (LPiException e) {
                System.err.println("Error: " + e.getMessage());
            } catch (Exception e) {
                System.err.println("Unexpected error: " + e.getMessage());
                e.printStackTrace();
            }
        }

        scanner.close();
    }

    private static void processInput(
            String input,
            Map<String, Value> env,
            Map<String, InferCheck.Kind> typeContext
    ) throws ParseException, ElabException, TypeCheckException {
        var tokens = Token.tokenize(input);

        // 尝试解析为声明
        try {
            PNode program = Parse.parseProgram(tokens);
            if (program instanceof PNode.Program(var items)) {
                for (PNode item : items) {
                    processDeclaration(item, env, typeContext);
                }
            }
        } catch (ParseException e) {
            // 如果解析为声明失败，尝试解析为表达式
            tokens = Token.tokenize(input);
            Node expr = Parse.parseExpr(tokens);
            checkAndEval(expr, env, typeContext);
        }
    }

    private static void processDeclaration(
            PNode decl,
            Map<String, Value> env,
            Map<String, InferCheck.Kind> typeContext
    ) throws ElabException, TypeCheckException {
        switch (decl) {
            case PNode.Axiom(var names, Node typeNode) -> {
                if (typeNode instanceof Node.Aster) {
                    for (Token name : names) {
                        typeContext.put(name.lexeme, new InferCheck.HasKind());
                        System.out.println(name.lexeme + " : *");
                    }
                } else {
                    Type type = Elab.elabType(typeNode);
                    InferCheck.checkKind(typeNode.location(), typeContext, type);
                    for (Token name : names) {
                        env.put(name.lexeme, Value.vFree(new Name.Global(name.lexeme)));
                        typeContext.put(name.lexeme, new InferCheck.HasType(type));
                        System.out.println(name.lexeme + " : " + type);
                    }
                }
            }
            case PNode.Defun(Token name, Node valueNode) -> {
                Term term = Elab.elab(valueNode);
                Type type = InferCheck.infer(typeContext, (Term.Inferable) term);
                Value value = Eval.eval(term, env);

                env.put(name.lexeme, value);
                typeContext.put(name.lexeme, new InferCheck.HasType(type));

                Term normalForm = Eval.reify(value);
                System.out.println(name.lexeme + " : " + type + " = " + normalForm);
            }
            case PNode.Check(Node termNode) -> {
                checkAndEval(termNode, env, typeContext);
            }
            case PNode.Program(var items) -> {
                for (PNode item : items) {
                    processDeclaration(item, env, typeContext);
                }
            }
        }
    }

    private static void checkAndEval(
            Node expr,
            Map<String, Value> env,
            Map<String, InferCheck.Kind> typeContext
    ) throws ElabException, TypeCheckException {
        Term term = Elab.elab(expr);
        Type type = InferCheck.infer(typeContext, (Term.Inferable) term);
        Value value = Eval.eval(term, env);
        Term normalForm = Eval.reify(value);

        System.out.println(normalForm + " : " + type);
    }
}
