package club.doki7.lambdapi.exc;

import club.doki7.lambdapi.syntax.Token;

public final class TypeCheckException extends LPiException {
    public TypeCheckException(Token location, String message) {
        super(TypeCheckException.class, location, message);
    }
}
