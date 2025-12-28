package club.doki7.lambdapi.syntax;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.TestOnly;

import java.util.List;

public sealed interface PNode {
    record Axiom(@NotNull List<@NotNull Token> names, @NotNull Node type) implements PNode {
        @TestOnly
        public Axiom(@NotNull String name, @NotNull Node type) {
            this(List.of(Token.ident(name)), type);
        }

        @Override
        public @NotNull String toString() {
            StringBuilder sb = new StringBuilder("axiom ");
            for (int i = 0; i < names.size(); i++) {
                sb.append(names.get(i));
                if (i < names.size() - 1) {
                    sb.append(", ");
                }
            }
            sb.append(" : ").append(type);
            return sb.toString();
        }
    }

    record Defun(@NotNull Token name, @NotNull Node value) implements PNode {
        @TestOnly
        public Defun(@NotNull String name, @NotNull Node value) {
            this(Token.ident(name), value);
        }

        @Override
        public @NotNull String toString() {
            return "defun " + name + " = " + value;
        }
    }

    record Check(@NotNull Node term) implements PNode {
        @Override
        public @NotNull String toString() {
            return term.toString();
        }
    }

    record Program(@NotNull List<@NotNull PNode> items) implements PNode {
        @Override
        public @NotNull String toString() {
            StringBuilder sb = new StringBuilder();
            for (PNode item : items) {
                sb.append(item).append("\n");
            }
            return sb.toString();
        }
    }
}
