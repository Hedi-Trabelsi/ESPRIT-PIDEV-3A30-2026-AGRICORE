package services;

import com.google.api.client.googleapis.auth.oauth2.*;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;

public class GoogleSignInService {

    // ============================================
    // REPLACE THESE WITH YOUR GOOGLE API CREDENTIALS
    // ============================================
    private static final String CLIENT_ID = "1093408491388-26ia28ggqehk4o8a2bc9bgnfppd7qevg.apps.googleusercontent.com";
    private static final String CLIENT_SECRET = "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";

    private static final JacksonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    private static final NetHttpTransport HTTP_TRANSPORT = new NetHttpTransport();

    /**
     * Verify and get user info from Google ID token
     */
    public static GoogleUserInfo verifyGoogleToken(String idTokenString) {
        try {
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(HTTP_TRANSPORT, JSON_FACTORY)
                    .setAudience(Collections.singletonList(CLIENT_ID))
                    .build();

            GoogleIdToken idToken = verifier.verify(idTokenString);
            if (idToken != null) {
                GoogleIdToken.Payload payload = idToken.getPayload();

                return new GoogleUserInfo(
                        payload.getSubject(),
                        payload.getEmail(),
                        (String) payload.get("given_name"),
                        (String) payload.get("family_name"),
                        (String) payload.get("picture"),
                        payload.getEmailVerified()
                );
            }
        } catch (GeneralSecurityException | IOException e) {
            e.printStackTrace();
        }
        return null;
    }

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

        // Getters
        public String getId() { return id; }
        public String getEmail() { return email; }
        public String getFirstName() { return firstName; }
        public String getLastName() { return lastName; }
        public String getPictureUrl() { return pictureUrl; }
        public boolean isEmailVerified() { return emailVerified; }
        public String getFullName() {
            return (firstName + " " + lastName).trim();
        }
    }
}