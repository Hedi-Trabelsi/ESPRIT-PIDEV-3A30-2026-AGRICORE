package services;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;
import org.json.JSONArray;

/**
 * Fetches Tunisian agricultural machinery & equipment indicators
 * from the World Bank API.
 *
 * World Bank API pattern:
 *   https://api.worldbank.org/v2/country/TN/indicator/{CODE}?format=json&mrv=1
 *
 * Indicators used (agricultural machinery / equipment focus for Tunisia):
 *   AG.LND.TRAC.ZS  — Tractors per 100 km² of arable land
 *   AG.LND.ARBL.ZS  — Arable land (% of land area)
 *   AG.LND.AGRI.ZS  — Agricultural land (% of land area)
 *   NV.AGR.TOTL.ZS  — Agriculture, value added (% of GDP)
 *   SL.AGR.EMPL.ZS  — Employment in agriculture (% of total employment)
 *   AG.YLD.CREL.KG  — Cereal yield (kg per hectare)
 */
public class AgricultureService {

    private static final String BASE =
            "https://api.worldbank.org/v2/country/TN/indicator/";
    private static final String PARAMS = "?format=json&mrv=1";

    /** Returns a map of  label → value string (e.g. "9.7 %")  */
    public Map<String, IndicatorResult> fetchIndicators() {
        Map<String, IndicatorResult> results = new LinkedHashMap<>();

        results.put("Tracteurs / 100 km²",
                fetch("AG.LND.TRAC.ZS", "tracteurs/100 km²", "🚜"));
        results.put("Terres arables",
                fetch("AG.LND.ARBL.ZS", "%", "🌱"));
        results.put("Terres agricoles",
                fetch("AG.LND.AGRI.ZS", "%", "🌾"));
        results.put("Agriculture / PIB",
                fetch("NV.AGR.TOTL.ZS", "%", "📊"));
        results.put("Emploi agricole",
                fetch("SL.AGR.EMPL.ZS", "%", "👨‍🌾"));
        results.put("Rendement céréalier",
                fetch("AG.YLD.CREL.KG", "kg/ha", "🌽"));

        return results;
    }

    private IndicatorResult fetch(String indicatorCode, String unit, String icon) {
        try {
            String urlStr = BASE + indicatorCode + PARAMS;
            HttpURLConnection conn = (HttpURLConnection)
                    new URL(urlStr).openConnection();
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.setRequestProperty("Accept", "application/json");

            if (conn.getResponseCode() != 200) {
                return new IndicatorResult(icon, unit, null, null);
            }

            try (InputStream is = conn.getInputStream()) {
                String json = new String(is.readAllBytes());
                JSONArray root = new JSONArray(json);
                if (root.length() < 2) return new IndicatorResult(icon, unit, null, null);

                JSONArray data = root.getJSONArray(1);
                if (data.isEmpty()) return new IndicatorResult(icon, unit, null, null);

                // Most recent non-null value
                for (int i = 0; i < data.length(); i++) {
                    var obj = data.getJSONObject(i);
                    if (!obj.isNull("value")) {
                        double val = obj.getDouble("value");
                        String year = obj.optString("date", "");
                        return new IndicatorResult(icon, unit, val, year);
                    }
                }
            }
        } catch (Exception e) {
            // Network unavailable or parse error — return null value
        }
        return new IndicatorResult(icon, unit, null, null);
    }

    // ── Result DTO ────────────────────────────────────────────────
    public static class IndicatorResult {
        public final String icon;
        public final String unit;
        public final Double value;   // null = N/A
        public final String year;

        public IndicatorResult(String icon, String unit, Double value, String year) {
            this.icon  = icon;
            this.unit  = unit;
            this.value = value;
            this.year  = year;
        }

        public String formatted() {
            if (value == null) return "N/A";
            // Show one decimal if not a whole number
            if (value == Math.floor(value)) return String.valueOf((int)(double) value);
            return String.format("%.1f", value);
        }
    }
}