package club.doki7.lambdapi.util;

public final class Ref<T> {
    public T value;

    public Ref(T value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "Ref(" + value + ")";
    }
}
