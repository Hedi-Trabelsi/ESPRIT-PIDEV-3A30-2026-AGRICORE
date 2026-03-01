package services.ai;

import Model.ForecastResult;
import Model.Vente;

import java.util.List;

public interface ForecastingService {
    ForecastResult forecastUserSales(int userId, List<Vente> ventes, int horizonMonths);
}
