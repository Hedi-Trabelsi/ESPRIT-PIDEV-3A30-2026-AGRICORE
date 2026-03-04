package services;

import java.awt.image.BufferedImage;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.json.JSONObject;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;

import javax.imageio.ImageIO;

public class FacePPService {

    private static final String API_KEY = "mjrTCwlQbYxXyPplnHam67bj0qw7ZpOQ";
    private static final String API_SECRET = "9V2cVKre1AJowTVGIgkIonwuYHMMBQce";
    private static final String API_URL = "https://api-us.faceplusplus.com/facepp/v3/compare";

    // ======================================================
    // REAL Face++ Compare Method
    // ======================================================
    public static double compareFaces(byte[] image1, byte[] image2) throws Exception {

        String img1Base64 = URLEncoder.encode(
                Base64.getEncoder().encodeToString(image1),
                StandardCharsets.UTF_8
        );

        String img2Base64 = URLEncoder.encode(
                Base64.getEncoder().encodeToString(image2),
                StandardCharsets.UTF_8
        );

        String urlParameters =
                "api_key=" + API_KEY +
                        "&api_secret=" + API_SECRET +
                        "&image_base64_1=" + img1Base64 +
                        "&image_base64_2=" + img2Base64;

        byte[] postData = urlParameters.getBytes(StandardCharsets.UTF_8);

        URL url = new URL(API_URL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        conn.setDoOutput(true);

        try (DataOutputStream wr = new DataOutputStream(conn.getOutputStream())) {
            wr.write(postData);
        }

        int responseCode = conn.getResponseCode();

        if (responseCode != HttpURLConnection.HTTP_OK) {
            throw new IOException("Face++ API returned HTTP " + responseCode);
        }

        StringBuilder response = new StringBuilder();
        try (BufferedReader in = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {

            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
        }

        JSONObject json = new JSONObject(response.toString());

        if (!json.has("confidence")) {
            throw new IOException("Face++ response does not contain confidence: " + json);
        }

        return json.getDouble("confidence");
    }

    // ======================================================
    // Convert OpenCV Mat → BufferedImage
    // ======================================================
    public static BufferedImage matToBufferedImage(Mat mat) throws Exception {
        MatOfByte mob = new MatOfByte();
        Imgcodecs.imencode(".jpg", mat, mob);
        byte[] byteArray = mob.toArray();
        return ImageIO.read(new ByteArrayInputStream(byteArray));
    }
}