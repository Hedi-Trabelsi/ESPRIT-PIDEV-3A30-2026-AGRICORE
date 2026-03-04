package services;

public class GoogleSignInService {

    public static class GoogleUserInfo {
        private final String id;
        private final String email;
        private final String firstName;
        private final String lastName;
        private final String pictureUrl;
        private final boolean emailVerified;

        public GoogleUserInfo(String id, String email, String firstName, String lastName,
                              String pictureUrl, boolean emailVerified) {
            this.id = id;
            this.email = email;
            this.firstName = firstName != null ? firstName : "";
            this.lastName = lastName != null ? lastName : "";
            this.pictureUrl = pictureUrl;
            this.emailVerified = emailVerified;
        }

        public String getId() { return id; }
        public String getEmail() { return email; }
        public String getFirstName() { return firstName; }
        public String getLastName() { return lastName; }
        public String getPictureUrl() { return pictureUrl; }
        public boolean isEmailVerified() { return emailVerified; }
        public String getFullName() {
            String fullName = (firstName + " " + lastName).trim();
            return fullName.isEmpty() ? "Google User" : fullName;
        }
    }
}