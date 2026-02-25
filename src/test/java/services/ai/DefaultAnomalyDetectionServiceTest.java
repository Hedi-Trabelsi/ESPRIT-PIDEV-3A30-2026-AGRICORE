package services.ai;

import models.AnomalyResult;
import models.Depense;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class DefaultAnomalyDetectionServiceTest {

    private List<Depense> history(double... values) {
        List<Depense> list = new ArrayList<>();
        LocalDate base = LocalDate.now().minusDays(30);
        for (int i = 0; i < values.length; i++) {
            Depense d = new Depense(0, 1, values[i], null, base.plusDays(i));
            list.add(d);
        }
        return list;
    }

    @Test
    void detectsAnomalyWhenZScoreHigh() {
        DefaultAnomalyDetectionService svc = new DefaultAnomalyDetectionService();
        List<Depense> hist = history(100, 110, 95, 105, 98, 103);
        Depense candidate = new Depense(0, 1, 250.0, null, LocalDate.now());
        AnomalyResult res = svc.analyzeDepense(1, hist, candidate);
        assertTrue(res.isAnomaly(), "Should flag a large spike as anomaly");
        assertTrue(res.getScore() > 2.0, "Z-score should be > 2");
        assertNotNull(res.getLowerBound());
        assertNotNull(res.getUpperBound());
    }

    @Test
    void notAnomalyForTypicalValue() {
        DefaultAnomalyDetectionService svc = new DefaultAnomalyDetectionService();
        List<Depense> hist = history(100, 110, 95, 105, 98, 103);
        Depense candidate = new Depense(0, 1, 108.0, null, LocalDate.now());
        AnomalyResult res = svc.analyzeDepense(1, hist, candidate);
        assertFalse(res.isAnomaly(), "Value near mean should not be anomalous");
        assertTrue(res.getScore() >= 0.0);
    }

    @Test
    void fallbackWhenStdZeroUsesFactorRule() {
        DefaultAnomalyDetectionService svc = new DefaultAnomalyDetectionService();
        List<Depense> hist = history(100, 100, 100, 100);
        Depense candidateNormal = new Depense(0, 1, 120.0, null, LocalDate.now());
        Depense candidateAnomaly = new Depense(0, 1, 160.0, null, LocalDate.now());
        AnomalyResult res1 = svc.analyzeDepense(1, hist, candidateNormal);
        AnomalyResult res2 = svc.analyzeDepense(1, hist, candidateAnomaly);
        assertFalse(res1.isAnomaly(), "20% over mean when std=0 should be ok");
        assertTrue(res2.isAnomaly(), "60% over mean triggers fallback rule");
        assertNull(res1.getLowerBound(), "Bounds are null when std=0");
        assertNull(res1.getUpperBound());
    }
}
