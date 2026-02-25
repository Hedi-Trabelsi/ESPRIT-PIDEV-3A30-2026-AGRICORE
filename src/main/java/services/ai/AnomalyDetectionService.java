package services.ai;

import models.AnomalyResult;
import models.Depense;

import java.util.List;

public interface AnomalyDetectionService {
    AnomalyResult analyzeDepense(int userId, List<Depense> history, Depense candidate);
}
