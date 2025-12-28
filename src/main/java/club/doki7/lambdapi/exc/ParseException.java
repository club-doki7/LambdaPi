package club.doki7.lambdapi.exc;

import club.doki7.lambdapi.syntax.Token;

public final class ParseException extends LPiException {
    public ParseException(Token location, String message) {
        super(ParseException.class, location, message);
    }
}
