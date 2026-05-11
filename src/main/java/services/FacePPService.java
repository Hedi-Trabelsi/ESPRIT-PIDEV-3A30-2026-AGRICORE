package services;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;

import javax.imageio.ImageIO;

public class FacePPService {

    private static final String API_KEY = "mjrTCwlQbYxXyPplnHam67bj0qw7ZpOQ";
    private static final String API_SECRET = "9V2cVKre1AJowTVGIgkIonwuYHMMBQce";
    private static final String API_BASE = "https://api-us.faceplusplus.com";
    private static final int MAX_DIMENSION = 800;

    // ======================================================
    // Legacy 1-vs-1 compare (kept for backward compatibility;
    // prefer the FaceSet+Search flow for login).
    // ======================================================
    public static double compareFaces(byte[] image1, byte[] image2) throws IOException {
        byte[] prepared1 = prepareImage(image1);
        byte[] prepared2 = prepareImage(image2);

        Map<String, String> params = new LinkedHashMap<>();
        params.put("image_base64_1", Base64.getEncoder().encodeToString(prepared1));
        params.put("image_base64_2", Base64.getEncoder().encodeToString(prepared2));

        JSONObject json = postForm("/facepp/v3/compare", params);

        if (!json.has("confidence")) {
            throw new IOException("Face++ response does not contain confidence: " + json);
        }
        return json.getDouble("confidence");
    }

    // ======================================================
    // Detect: returns the first face_token found in the image
    // ======================================================
    public static String detectFace(byte[] imageBytes) throws IOException {
        byte[] prepared = prepareImage(imageBytes);
        Map<String, String> params = new LinkedHashMap<>();
        params.put("image_base64", Base64.getEncoder().encodeToString(prepared));

        JSONObject resp = postForm("/facepp/v3/detect", params);
        JSONArray faces = resp.optJSONArray("faces");
        if (faces == null || faces.length() == 0) {
            throw new IOException("No face detected in image");
        }
        return faces.getJSONObject(0).getString("face_token");
    }

    public static void setUserId(String faceToken, String userId) throws IOException {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("face_token", faceToken);
        params.put("user_id", userId);
        postForm("/facepp/v3/face/setuserid", params);
    }

    /**
     * Creates a FaceSet with the given outer_id. Idempotent: if the FaceSet
     * already exists Face++ returns a 4xx with {@code FACESET_EXIST} or
     * {@code OUTER_ID_EXIST} which we treat as success.
     */
    public static void ensureFaceSet(String outerId) throws IOException {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("outer_id", outerId);
        params.put("display_name", "AGRICORE users");
        try {
            postForm("/facepp/v3/faceset/create", params);
        } catch (IOException e) {
            String msg = e.getMessage() == null ? "" : e.getMessage();
            if (msg.contains("FACESET_EXIST") || msg.contains("OUTER_ID_EXIST")) {
                return;
            }
            throw e;
        }
    }

    public static int addFaceToSet(String outerId, String faceToken) throws IOException {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("outer_id", outerId);
        params.put("face_tokens", faceToken);
        JSONObject resp = postForm("/facepp/v3/faceset/addface", params);
        return resp.optInt("face_added", 0);
    }

    public static void removeFaceFromSet(String outerId, String faceToken) throws IOException {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("outer_id", outerId);
        params.put("face_tokens", faceToken);
        postForm("/facepp/v3/faceset/removeface", params);
    }

    public static SearchResult searchInSet(byte[] liveImage, String outerId) throws IOException {
        byte[] prepared = prepareImage(liveImage);
        Map<String, String> params = new LinkedHashMap<>();
        params.put("image_base64", Base64.getEncoder().encodeToString(prepared));
        params.put("outer_id", outerId);
        params.put("return_result_count", "1");

        JSONObject resp = postForm("/facepp/v3/search", params);
        JSONArray results = resp.optJSONArray("results");
        if (results == null || results.length() == 0) return null;

        JSONObject best = results.getJSONObject(0);
        String userId = best.optString("user_id", "");
        double confidence = best.optDouble("confidence", 0.0);
        String faceToken = best.optString("face_token", "");
        return new SearchResult(userId, faceToken, confidence);
    }

    public static final class SearchResult {
        public final String userId;
        public final String faceToken;
        public final double confidence;
        public SearchResult(String userId, String faceToken, double confidence) {
            this.userId = userId;
            this.faceToken = faceToken;
            this.confidence = confidence;
        }
    }

    // ======================================================
    // Shared HTTP helper — retries on CONCURRENCY_LIMIT_EXCEEDED
    // (Face++ free tier allows ~1 in-flight request at a time, so transient
    // 403s are expected whenever two operations land within the same second).
    // ======================================================
    private static JSONObject postForm(String endpoint, Map<String, String> params) throws IOException {
        final int maxAttempts = 5;
        long backoffMs = 1200;
        IOException last = null;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return postFormOnce(endpoint, params);
            } catch (IOException e) {
                last = e;
                String msg = e.getMessage() == null ? "" : e.getMessage();
                if (!msg.contains("CONCURRENCY_LIMIT_EXCEEDED") || attempt == maxAttempts) {
                    throw e;
                }
                try {
                    Thread.sleep(backoffMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Interrupted while waiting to retry " + endpoint, ie);
                }
                backoffMs = Math.min(backoffMs * 2, 8000);
            }
        }
        throw last; // unreachable
    }

    private static JSONObject postFormOnce(String endpoint, Map<String, String> params) throws IOException {
        StringBuilder body = new StringBuilder();
        body.append("api_key=").append(URLEncoder.encode(API_KEY, StandardCharsets.UTF_8));
        body.append("&api_secret=").append(URLEncoder.encode(API_SECRET, StandardCharsets.UTF_8));
        for (Map.Entry<String, String> e : params.entrySet()) {
            body.append('&').append(URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8));
            body.append('=').append(URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8));
        }

        byte[] postData = body.toString().getBytes(StandardCharsets.UTF_8);

        URL url = new URL(API_BASE + endpoint);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        conn.setDoOutput(true);
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(30000);

        try (DataOutputStream wr = new DataOutputStream(conn.getOutputStream())) {
            wr.write(postData);
        }

        int responseCode = conn.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            String errorBody = readStream(conn.getErrorStream());
            throw new IOException("Face++ " + endpoint + " returned HTTP " + responseCode
                    + (errorBody.isEmpty() ? "" : " - " + errorBody));
        }

        return new JSONObject(readStream(conn.getInputStream()));
    }

    // Downscale + re-encode as baseline JPEG so requests stay well under
    // Face++'s 2MB-per-image limit and avoid alpha-channel decode errors.
    private static byte[] prepareImage(byte[] imageBytes) throws IOException {
        if (imageBytes == null || imageBytes.length == 0) {
            throw new IOException("Image bytes are empty");
        }
        BufferedImage img = ImageIO.read(new ByteArrayInputStream(imageBytes));
        if (img == null) {
            throw new IOException("Image bytes could not be decoded as an image");
        }

        int width = img.getWidth();
        int height = img.getHeight();
        int newW = width;
        int newH = height;
        if (width > MAX_DIMENSION || height > MAX_DIMENSION) {
            double scale = Math.min((double) MAX_DIMENSION / width, (double) MAX_DIMENSION / height);
            newW = Math.max(1, (int) Math.round(width * scale));
            newH = Math.max(1, (int) Math.round(height * scale));
        }

        BufferedImage rgb = new BufferedImage(newW, newH, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = rgb.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(img, 0, 0, newW, newH, null);
        g.dispose();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(rgb, "jpg", baos);
        return baos.toByteArray();
    }

    private static String readStream(InputStream in) throws IOException {
        if (in == null) return "";
        StringBuilder sb = new StringBuilder();
        try (BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = r.readLine()) != null) sb.append(line);
        }
        return sb.toString();
    }

    // ======================================================
    // Convert OpenCV Mat -> BufferedImage
    // ======================================================
    public static BufferedImage matToBufferedImage(Mat mat) throws Exception {
        MatOfByte mob = new MatOfByte();
        Imgcodecs.imencode(".jpg", mat, mob);
        byte[] byteArray = mob.toArray();
        return ImageIO.read(new ByteArrayInputStream(byteArray));
    }
}
