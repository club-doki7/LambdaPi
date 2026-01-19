package club.doki7.lambdapi.common;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class DeBruijnIndex {
    public static int findInContext(@NotNull String name, @NotNull List<String> ctx) {
        for (int i = ctx.size() - 1; i >= 0; i--) {
            if (ctx.get(i).equals(name)) {
                return ctx.size() - 1 - i;
            }
        }
        return -1;
    }

    public static String superscriptNum(char prefix, int index) {
        if (index < 0) {
            throw new IllegalArgumentException("Index must be non-negative");
        }

        String indexString = Integer.toString(index);
        StringBuilder sb = new StringBuilder();
        sb.append(prefix);

        for (char c : indexString.toCharArray()) {
            sb.append(switch (c) {
                case '0' -> '⁰';
                case '1' -> '¹';
                case '2' -> '²';
                case '3' -> '³';
                case '4' -> '⁴';
                case '5' -> '⁵';
                case '6' -> '⁶';
                case '7' -> '⁷';
                case '8' -> '⁸';
                case '9' -> '⁹';
                case '-' -> '⁻';
                case '+' -> '⁺';
                default -> throw new IllegalStateException("Invalid character in index: " + c);
            });
        }
        return sb.toString();
    }
}
