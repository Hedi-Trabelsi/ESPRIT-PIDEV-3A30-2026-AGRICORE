package services.ai;

import models.AnomalyResult;
import models.Depense;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class RobustAnomalyDetectionServiceTest {

    private List<Depense> history(double... vals) {
        List<Depense> list = new ArrayList<>();
        LocalDate base = LocalDate.now().minusDays(10);
        for (int i = 0; i < vals.length; i++) {
            list.add(new Depense(0, 1, vals[i], null, base.plusDays(i)));
        }
        return list;
    }

    @Test
    void detectsOutlierWithMAD() {
        RobustAnomalyDetectionService svc = new RobustAnomalyDetectionService();
        List<Depense> hist = history(100, 110, 95, 105, 98, 103);
        Depense cand = new Depense(0, 1, 250.0, null, LocalDate.now());
        AnomalyResult res = svc.analyzeDepense(1, hist, cand);
        assertTrue(res.isAnomaly(), "MAD should flag large spike");
        assertNotNull(res.getUpperBound());
    }

    @Test
    void fallbackIQRWhenMADZero() {
        RobustAnomalyDetectionService svc = new RobustAnomalyDetectionService();
        List<Depense> hist = history(100, 100, 100, 100, 100);
        Depense ok = new Depense(0, 1, 120.0, null, LocalDate.now());
        Depense spike = new Depense(0, 1, 170.0, null, LocalDate.now());
        assertFalse(svc.analyzeDepense(1, hist, ok).isAnomaly());
        assertTrue(svc.analyzeDepense(1, hist, spike).isAnomaly());
    }
}
