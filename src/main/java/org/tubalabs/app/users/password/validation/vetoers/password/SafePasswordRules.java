package org.tubalabs.app.users.password.validation.vetoers.password;

public final class SafePasswordRules {

    private static final int MINIMUM_LENGTH = 12;
    private static final int MAXIMUM_LENGTH = 128;

    private SafePasswordRules() {
    }

    public static boolean isSafe(String password) {
        if (password == null || password.isBlank()) {
            return true;
        }
        return hasValidLength(password) && containsLetter(password) && containsNumberOrSymbol(password);
    }

    private static boolean hasValidLength(String password) {
        return password.length() >= MINIMUM_LENGTH && password.length() <= MAXIMUM_LENGTH;
    }

    private static boolean containsLetter(String password) {
        return password.codePoints().anyMatch(Character::isLetter);
    }

    private static boolean containsNumberOrSymbol(String password) {
        return password.codePoints()
                .anyMatch(codePoint -> Character.isDigit(codePoint) || isSymbol(codePoint));
    }

    private static boolean isSymbol(int codePoint) {
        return !Character.isLetterOrDigit(codePoint) && !Character.isWhitespace(codePoint);
    }
}
