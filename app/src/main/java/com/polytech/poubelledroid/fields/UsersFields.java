package com.polytech.poubelledroid.fields;

/** Firebase document fields for Users collection */
public class UsersFields {

    private UsersFields() {
        // Private constructor to hide the implicit public one
    }

    public static final String EMAIL = "email";
    public static final String ID = "id";
    public static final String USERNAME = "username";
    public static final String FCM_TOKEN = "fcmToken";
    public static final String PASSWORD = "password";

    public static final String COLLECTION_NAME = "users";
}
