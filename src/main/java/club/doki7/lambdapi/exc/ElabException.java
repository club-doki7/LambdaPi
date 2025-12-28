package club.doki7.lambdapi.exc;

import club.doki7.lambdapi.syntax.Token;

public final class ElabException extends LPiException {
    public ElabException(Token location, String message) {
        super(ElabException.class, location, message);
    }
}
