package club.doki7.lambdapi.stlc;

import club.doki7.lambdapi.exc.RawTypeCheckException;
import club.doki7.lambdapi.util.ConsList;
import club.doki7.lambdapi.util.Pair;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public final class InferCheck {
    public sealed interface Kind permits HasKind, HasType {}

    record HasKind() implements Kind {
        @Override
        public @NotNull String toString() {
            return ":: *";
        }
    }

    record HasType(@NotNull Type type) implements Kind {
        @Override
        public @NotNull String toString() {
            return ":: " + type;
        }
    }

    public static void checkKind(
            ConsList<Pair<Name.Local, Kind>> ctx,
            Map<String, Kind> globals,
            Type type
    ) throws RawTypeCheckException {
        switch (type) {
            case Type.Free(Name.Global(String strName)) -> {
                if (!(globals.get(strName) instanceof HasKind)) {
                    throw new RawTypeCheckException(strName + "is not a type");
                }
            }
            case Type.Free(Name.Local(int index)) -> {
                Pair<Name.Local, Kind> binding = ctx.findFirst(p -> p.first().index() == index);
                if (!(binding != null && binding.second() instanceof HasKind)) {
                    throw new RawTypeCheckException("L" + index + " is not a type");
                }
            }
            case Type.Free(Name.Quote _) -> throw new IllegalStateException(
                    "Unexpected quote in type checking phase"
            );
            case Type.Fun(Type in, Type out) -> {
                checkKind(ctx, globals, in);
                checkKind(ctx, globals, out);
            }
        }
    }
}
