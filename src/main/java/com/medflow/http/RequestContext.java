package com.medflow.http;

public final class RequestContext {

    private static final ThreadLocal<AuthenticatedUser> CURRENT = new ThreadLocal<>();

    private RequestContext() {
    }

    public static void setCurrentUser(AuthenticatedUser user) {
        CURRENT.set(user);
    }

    public static AuthenticatedUser getCurrentUser() {
        return CURRENT.get();
    }

    public static String currentUserId() {
        AuthenticatedUser user = CURRENT.get();
        return user == null ? null : user.userId();
    }

    public static String currentUserRole() {
        AuthenticatedUser user = CURRENT.get();
        return user == null ? null : user.role();
    }

    public static void clear() {
        CURRENT.remove();
    }
}
