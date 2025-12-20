package club.doki7.lambdapi.syntax;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public sealed interface PNode {
    record Axiom(@NotNull String name, @NotNull Node type) implements PNode {
        @Override
        public @NotNull String toString() {
            return "axiom " + name + " : " + type;
        }
    }

    record Claim(@NotNull String name, @NotNull Node type) implements PNode {
        @Override
        public @NotNull String toString() {
            return "claim " + name + " : " + type;
        }
    }

    record Defun(@NotNull String name, @NotNull Node type, @Nullable Node value) implements PNode {
        @Override
        public @NotNull String toString() {
            if (value != null) {
                return "defun " + name + " : " + type + " = " + value;
            } else {
                return "defun " + name + " : " + type + " = sorry";
            }
        }
    }

    record Expr(@NotNull Node term) implements PNode {
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
