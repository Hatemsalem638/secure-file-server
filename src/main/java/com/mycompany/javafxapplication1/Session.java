package com.mycompany.javafxapplication1;

/** Holds the signed-in user so file operations can enforce ownership. */
public final class Session {

    private static String currentUser;

    private Session() {
    }

    public static void signIn(String username) {
        currentUser = username;
    }

    public static void signOut() {
        currentUser = null;
    }

    public static String currentUser() {
        if (currentUser == null) {
            throw new IllegalStateException("No user is signed in.");
        }
        return currentUser;
    }

    public static boolean isSignedIn() {
        return currentUser != null;
    }
}
