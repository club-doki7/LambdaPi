package club.doki7.lambdapi.stlc;

import club.doki7.lambdapi.common.Name;
import club.doki7.lambdapi.exc.ElabException;
import club.doki7.lambdapi.exc.LPiException;
import club.doki7.lambdapi.exc.ParseException;
import club.doki7.lambdapi.exc.TypeCheckException;
import club.doki7.lambdapi.syntax.Node;
import club.doki7.lambdapi.syntax.PNode;
import club.doki7.lambdapi.syntax.Parse;
import club.doki7.lambdapi.syntax.Token;

import java.util.*;

public final class Application {
    private static final String ANSI_RESET = "\u001B[0m";
    private static final String ANSI_RED = "\u001B[31m";
    private static final String ANSI_GREEN = "\u001B[32m";
    private static final String ANSI_YELLOW = "\u001B[33m";
    private static final String ANSI_BLUE = "\u001B[34m";
    private static final String ANSI_PURPLE = "\u001B[35m";
    private static final String ANSI_CYAN = "\u001B[36m";
    private static final String ANSI_BOLD = "\u001B[1m";
    private static final String ANSI_ITALIC = "\u001B[3m";

    static void main() {
        HashMap<String, Value> env = new HashMap<>();
        HashMap<String, InferCheck.Kind> typeContext = new HashMap<>();

        System.out.println("=== Simply Typed Lambda Calculus ===");
        System.out.println("Commands:");
        System.out.println("  axiom <type> : *         - Introduce type variable");
        System.out.println("  axiom <name> : <type>    - Postulate axiom of type");
        System.out.println("  defun <name> = <expr>    - Define function");
        System.out.println("  check <expr>             - Type check and evaluate expression");
        System.out.println("  <expr>                   - Type check and evaluate expression");
        System.out.println("  :env                     - Show current environment and type context");
        System.out.println("  :clear, :cls             - Clear environment and type context");
        System.out.println("  :quit, :q                - Exit REPL");
        System.out.println();

        Scanner scanner = new Scanner(System.in);

        loop: while (true) {
            System.out.print(ANSI_BLUE + ANSI_BOLD + "» " + ANSI_RESET);
            if (!scanner.hasNextLine()) {
                break;
            }

            String line = scanner.nextLine().trim();

            switch (line) {
                case "":
                    continue;
                case ":quit":
                case ":q":
                    break loop;
                case ":clear":
                case ":cls":
                    env.clear();
                    typeContext.clear();
                    System.out.println(
                            ANSI_GREEN
                            + "You got to put the past behind you before you can move on."
                            + ANSI_RESET
                    );
                    continue;
                case ":env":
                    if (!env.isEmpty()) {
                        for (Map.Entry<String, Value> entry : env.entrySet()) {
                            String name = entry.getKey();
                            Value value = entry.getValue();
                            InferCheck.Kind kind = typeContext.get(name);
                            boolean isAxiom = value instanceof Value.NFree;
                            System.out.println(
                                    (isAxiom ? ANSI_ITALIC + ANSI_CYAN : ANSI_GREEN)
                                    + "  "
                                    + name
                                    + " "
                                    + kind
                                    + ANSI_RESET
                            );
                        }
                    } else {
                        System.out.println(ANSI_GREEN + "Environment is empty." + ANSI_RESET);
                    }
                    continue;
            }

            try {
                processInput(line, env, typeContext);
            } catch (LPiException e) {
                System.out.println(ANSI_RED + "Error: " + e.getMessage() + ANSI_RESET);
            } catch (Exception e) {
                System.out.println(ANSI_RED + "Unexpected error: " + e.getMessage() + ANSI_RESET);
                e.printStackTrace();
            }
        }

        scanner.close();
        System.out.println(ANSI_YELLOW
                           + "Man! Hahaha... what can I say? Mamba out."
                           + ANSI_RESET);
        System.out.println(ANSI_PURPLE
                           + "I'll tell you all about it when I see you again."
                           + ANSI_RESET);
    }

    private static void processInput(
            String input,
            Map<String, Value> env,
            Map<String, InferCheck.Kind> typeContext
    ) throws ParseException, ElabException, TypeCheckException {
        ArrayList<Token> tokens = Token.tokenize(input);
        if (tokens.isEmpty()) {
            return;
        }

        Token.Kind firstTokenKind = tokens.getFirst().kind;
        if (firstTokenKind != Token.Kind.KW_AXIOM
            && firstTokenKind != Token.Kind.KW_DEFUN
            && firstTokenKind != Token.Kind.KW_CHECK) {
            Node expr = Parse.parseExpr(tokens);
            checkAndEval(expr, env, typeContext);
        } else {
            PNode program = Parse.parseProgram(tokens);
            if (program instanceof PNode.Program(var items)) {
                for (PNode item : items) {
                    processDeclaration(item, env, typeContext);
                }
            }
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
                    }
                    String namesString = String.join(
                            ", ",
                            names.stream().map(t -> t.lexeme).toList()
                    );
                    System.out.println(ANSI_CYAN + ANSI_ITALIC
                                       + "introduced "
                                       + namesString + " : *"
                                       + ANSI_RESET);
                } else {
                    Type type = Elab.elabType(typeNode);
                    InferCheck.checkKind(typeNode.location(), typeContext, type);
                    for (Token name : names) {
                        env.put(name.lexeme, Value.vFree(new Name.Global(name.lexeme)));
                        typeContext.put(name.lexeme, new InferCheck.HasType(type));
                        System.out.println(ANSI_CYAN + ANSI_ITALIC
                                           + "postulated "
                                           + name.lexeme + " : " + type
                                           + ANSI_RESET);
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
                System.out.println(ANSI_GREEN
                                   + "defined "
                                   + name.lexeme + " : " + type + " = " + normalForm
                                   + ANSI_RESET);
            }
            case PNode.Check(Node termNode) -> checkAndEval(termNode, env, typeContext);
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

        System.out.println(ANSI_GREEN + normalForm + " : " + type + ANSI_RESET);
    }
}
