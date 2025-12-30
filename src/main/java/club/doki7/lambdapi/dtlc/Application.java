package club.doki7.lambdapi.dtlc;

import club.doki7.lambdapi.common.AsciiColor;
import club.doki7.lambdapi.common.Name;
import club.doki7.lambdapi.exc.ElabException;
import club.doki7.lambdapi.exc.LPiException;
import club.doki7.lambdapi.exc.ParseException;
import club.doki7.lambdapi.exc.TypeCheckException;
import club.doki7.lambdapi.syntax.Node;
import club.doki7.lambdapi.syntax.PNode;
import club.doki7.lambdapi.syntax.Parse;
import club.doki7.lambdapi.syntax.Token;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public final class Application implements AsciiColor {
    static void main() {
        Globals globals = Globals.empty();

        System.out.println("=== Dependent Typed Lambda Calculus ===");
        System.out.println("Commands:");
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
                    globals.clear();
                    System.out.println(
                            ANSI_GREEN
                            + "You got to put the past behind you before you can move on."
                            + ANSI_RESET
                    );
                    continue;
                case ":env":
                    if (!globals.values().isEmpty()) {
                        for (Map.Entry<String, Value> entry : globals.values().entrySet()) {
                            String name = entry.getKey();
                            Value value = entry.getValue();
                            Type type = globals.types().get(name);
                            boolean isAxiom = value instanceof Value.NFree;
                            System.out.println(
                                    (isAxiom ? ANSI_ITALIC + ANSI_CYAN : ANSI_GREEN)
                                    + "  "
                                    + name
                                    + " : "
                                    + Eval.reify(type.value())
                                    + ANSI_RESET
                            );
                        }
                    } else {
                        System.out.println(ANSI_GREEN + "Environment is empty." + ANSI_RESET);
                    }
                    continue;
            }

            try {
                processInput(line, globals);
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
            Globals globals
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
            checkAndEval(expr, globals);
        } else {
            PNode program = Parse.parseProgram(tokens);
            if (program instanceof PNode.Program(var items)) {
                for (PNode item : items) {
                    processDeclaration(item, globals);
                }
            }
        }
    }

    private static void processDeclaration(
            PNode decl,
            Globals globals
    ) throws ElabException, TypeCheckException {
        switch (decl) {
            case PNode.Axiom(List<Token> names, Node typeNode) -> {
                String namesStr = String.join(
                        ", ",
                        names.stream().map(t -> t.lexeme).toList()
                );

                Term typeTerm = Elab.elab(typeNode);
                InferCheck.infer((Term.Inferable) typeTerm, globals);
                Type type = Type.of(Eval.eval(typeTerm, globals.values()));
                for (Token name : names) {
                    globals.values().put(name.lexeme, Value.vFree(typeNode, new Name.Global(name.lexeme)));
                    globals.types().put(name.lexeme, type);
                }

                System.out.println(ANSI_CYAN + ANSI_ITALIC
                                   + "postulated "
                                   + namesStr + " : " + Eval.reify(type.value())
                                   + ANSI_RESET);
            }
            case PNode.Defun(Token name, Node valueNode) -> {
                Term term = Elab.elab(valueNode);
                Type type = InferCheck.infer((Term.Inferable) term, globals);
                Value value = Eval.eval(term, globals.values());

                globals.values().put(name.lexeme, value);
                globals.types().put(name.lexeme, type);

                Term normalForm = Eval.reify(value);
                System.out.println(ANSI_GREEN
                                   + "defined "
                                   + name.lexeme
                                   + " = " + normalForm
                                   + " : " + Eval.reify(type.value())
                                   + ANSI_RESET);
            }
            case PNode.Check(Node termNode) -> checkAndEval(termNode, globals);
            case PNode.Program(var items) -> {
                for (PNode item : items) {
                    processDeclaration(item, globals);
                }
            }
        }
    }

    private static void checkAndEval(
            Node expr,
            Globals globals
    ) throws ElabException, TypeCheckException {
        Term term = Elab.elab(expr);
        Type type = InferCheck.infer((Term.Inferable) term, globals);
        Value value = Eval.eval(term, globals.values());
        Term normalForm = Eval.reify(value);

        System.out.println(ANSI_GREEN + normalForm + " : " + Eval.reify(type.value()) + ANSI_RESET);
    }
}
