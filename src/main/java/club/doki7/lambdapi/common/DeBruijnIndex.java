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
}
