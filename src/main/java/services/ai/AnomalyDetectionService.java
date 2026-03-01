package services.ai;

import Model.AnomalyResult;
import Model.Depense;

import java.util.List;

public interface AnomalyDetectionService {
    AnomalyResult analyzeDepense(int userId, List<Depense> history, Depense candidate);
}
