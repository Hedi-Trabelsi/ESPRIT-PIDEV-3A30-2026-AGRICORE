package services.ai;

import models.AnomalyResult;
import models.Depense;

import java.util.List;

public class DefaultAnomalyDetectionService implements AnomalyDetectionService {
    @Override
    public AnomalyResult analyzeDepense(int userId, List<Depense> history, Depense candidate) {
        double[] values = history.stream().mapToDouble(Depense::getMontant).toArray();
        double mean = 0.0;
        for (double v : values) mean += v;
        mean = values.length > 0 ? mean / values.length : 0.0;
        double var = 0.0;
        for (double v : values) var += (v - mean) * (v - mean);
        double std = values.length > 1 ? Math.sqrt(var / (values.length - 1)) : 0.0;
        double z = std > 0 ? Math.abs((candidate.getMontant() - mean) / std) : 0.0;
        boolean anomaly = values.length >= 3 && z > 2.0 || (std == 0 && values.length > 0 && candidate.getMontant() > mean * 1.5);
        AnomalyResult res = new AnomalyResult();
        res.setAnomaly(anomaly);
        res.setScore(z);
        res.setLowerBound(std > 0 ? mean - 2 * std : null);
        res.setUpperBound(std > 0 ? mean + 2 * std : null);
        res.setMessage(anomaly ? "Dépense détectée comme anormale" : "Dépense normale");
        return res;
    }
}
