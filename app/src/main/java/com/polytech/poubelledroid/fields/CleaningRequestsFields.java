package com.polytech.poubelledroid.fields;

/** Firebase document fields for CleaningRequests collection */
public class CleaningRequestsFields {

    private CleaningRequestsFields() {
        // Private constructor to hide the implicit public one
    }

    public static final String CLEANED_PHOTO_URL = "cleanedPhotoUrl";
    public static final String CLEANER_ID = "cleanerId";
    public static final String DATE = "date";
    public static final String ID = "id";
    public static final String MESSAGE = "message";
    public static final String REPORTER_ID = "reporterId";
    public static final String STATUS = "status";
    public static final String TRASH_ID = "trashId";
    public static final String TRASH_IMG_URL = "trashImgUrl";
    public static final String DESCRIPTION = "description"; // Not in the Firestore document
    public static final String IMAGE_URL = "imageUrl"; // Not in the Firestore document

    public static final String COLLECTION_NAME = "cleaningRequests";
}
