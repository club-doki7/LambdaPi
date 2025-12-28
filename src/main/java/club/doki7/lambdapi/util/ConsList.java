package club.doki7.lambdapi.util;

import org.jetbrains.annotations.NotNull;

import java.util.function.Predicate;

public sealed interface ConsList<T> {
    enum Nil implements ConsList<Object> {
        INSTANCE;

        @Override
        public @NotNull String toString() {
            return "nil";
        }
    }

    record Cons<T>(T head, ConsList<T> tail) implements ConsList<T> {
        @Override
        public @NotNull String toString() {
            return "(" + head + ", " + tail + ")";
        }
    }

    static <T> @NotNull ConsList<T> nil() {
        // noinspection unchecked
        return (ConsList<T>) Nil.INSTANCE;
    }

    static <T> @NotNull ConsList<T> cons(@NotNull T head, @NotNull ConsList<T> tail) {
        return new Cons<>(head, tail);
    }

    static <T> @NotNull ConsList<T> cons(@NotNull T head) {
        return new Cons<>(head, nil());
    }

    default T get(int index) {
        ConsList<T> current = this;
        int i = index;
        while (current instanceof Cons<T>(T x, ConsList<T> xs)) {
            if (i == 0) {
                return x;
            }
            current = xs;
            i--;
        }
        throw new IndexOutOfBoundsException(index);
    }

    default T findFirst(Predicate<T> predicate) {
        ConsList<T> current = this;
        while (current instanceof Cons<T>(T x, ConsList<T> xs)) {
            if (predicate.test(x)) {
                return x;
            }
            current = xs;
        }
        return null;
    }

    default int veryExpensiveSizeDoNotUseInCriticalPathOrYouWillBeFired() {
        int size = 0;
        ConsList<T> current = this;
        while (current instanceof Cons<T>(T x, ConsList<T> xs)) {
            size++;
            current = xs;
        }
        return size;
    }
}
