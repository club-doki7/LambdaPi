package club.doki7.lambdapi.syntax;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.TestOnly;

import java.util.List;

public sealed interface PNode {
    record Axiom(@NotNull Token name, @NotNull Node type) implements PNode {
        @TestOnly
        public Axiom(@NotNull String name, @NotNull Node type) {
            this(Token.ident(name), type);
        }

        @Override
        public @NotNull String toString() {
            return "axiom " + name + " : " + type;
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
