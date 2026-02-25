package services.ai;

import models.ForecastResult;
import models.Vente;

import java.util.List;

public interface ForecastingService {
    ForecastResult forecastUserSales(int userId, List<Vente> ventes, int horizonMonths);
}
