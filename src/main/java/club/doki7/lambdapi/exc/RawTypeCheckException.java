package club.doki7.lambdapi.exc;

public final class RawTypeCheckException extends Exception {
    public final String message;

    public RawTypeCheckException(String message) {
        super(message);
        this.message = message;
    }
}
