package services.ai;

import Model.AnomalyResult;
import Model.Depense;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RobustAnomalyDetectionService implements AnomalyDetectionService {
    private final double threshold;

    public RobustAnomalyDetectionService() {
        this.threshold = 3.5;
    }

    public RobustAnomalyDetectionService(double threshold) {
        this.threshold = threshold;
    }
    @Override
    public AnomalyResult analyzeDepense(int userId, List<Depense> history, Depense candidate) {
        List<Double> values = new ArrayList<>();
        for (Depense d : history) {
            if (d != candidate) values.add(d.getMontant());
        }
        Collections.sort(values);
        AnomalyResult res = new AnomalyResult();
        if (values.size() < 3) {
            res.setAnomaly(false);
            res.setScore(0.0);
            res.setMessage("Échantillon insuffisant");
            return res;
        }
        double median = median(values);
        List<Double> absDev = new ArrayList<>(values.size());
        for (double v : values) absDev.add(Math.abs(v - median));
        double mad = median(absDev);

        double score;
        boolean anomaly = false;
        Double lower = null, upper = null;

        if (mad > 0) {
            score = 0.6745 * (candidate.getMontant() - median) / mad;
            double thr = this.threshold;
            anomaly = Math.abs(score) > thr;
            lower = median - thr * mad / 0.6745;
            upper = median + thr * mad / 0.6745;
        } else {
            // Fallback basé sur l'IQR
            double q1 = percentile(values, 25);
            double q3 = percentile(values, 75);
            double iqr = q3 - q1;
            score = 0.0;
            if (iqr > 0) {
                lower = q1 - 1.5 * iqr;
                upper = q3 + 1.5 * iqr;
                anomaly = candidate.getMontant() < lower || candidate.getMontant() > upper;
            } else {
                double mean = values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
                anomaly = mean > 0 && candidate.getMontant() > mean * 1.5;
                lower = null;
                upper = mean * 1.5;
            }
        }

        res.setAnomaly(anomaly);
        res.setScore(Math.abs(score));
        res.setLowerBound(lower);
        res.setUpperBound(upper);
        res.setMessage(anomaly ? "Anomalie (MAD/IQR)" : "Normal");
        return res;
    }

    private static double median(List<Double> sorted) {
        int n = sorted.size();
        if (n == 0) return 0.0;
        if (n % 2 == 1) return sorted.get(n / 2);
        return 0.5 * (sorted.get(n / 2 - 1) + sorted.get(n / 2));
    }

    private static double percentile(List<Double> sorted, double p) {
        if (sorted.isEmpty()) return 0.0;
        double idx = p / 100.0 * (sorted.size() - 1);
        int i = (int) Math.floor(idx);
        int j = (int) Math.ceil(idx);
        if (i == j) return sorted.get(i);
        double w = idx - i;
        return sorted.get(i) * (1 - w) + sorted.get(j) * w;
    }
}
