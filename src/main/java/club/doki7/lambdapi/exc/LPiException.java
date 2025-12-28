package club.doki7.lambdapi.exc;

import club.doki7.lambdapi.syntax.Token;

public sealed class LPiException extends Exception permits
        ParseException,
        ElabException,
        TypeCheckException
{
    public final Token location;
    public final String message;

    public LPiException(Class<?> C, Token location, String message) {
        super(location.line + ":" + location.col + ": " + C.getSimpleName() + ": " + message);

        this.location = location;
        this.message = message;
    }
}
