package services.ai;

import Model.ForecastPoint;
import Model.ForecastResult;
import Model.Vente;

import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

public class DefaultForecastingService implements ForecastingService {
    @Override
    public ForecastResult forecastUserSales(int userId, List<Vente> ventes, int horizonMonths) {
        Map<YearMonth, Double> monthSum = new HashMap<>();
        for (Vente v : ventes) {
            if (v.getDate() == null) continue;
            YearMonth ym = YearMonth.from(v.getDate());
            monthSum.put(ym, monthSum.getOrDefault(ym, 0.0) + v.getChiffreAffaires());
        }
        List<YearMonth> months = new ArrayList<>(monthSum.keySet());
        months.sort(Comparator.naturalOrder());
        List<Double> series = months.stream().map(monthSum::get).collect(Collectors.toList());
        double alpha = 0.5;
        double level = series.isEmpty() ? 0.0 : series.get(0);
        List<Double> fitted = new ArrayList<>();
        for (double y : series) {
            level = alpha * y + (1 - alpha) * level;
            fitted.add(level);
        }
        double mse = 0.0;
        for (int i = 0; i < series.size(); i++) {
            double e = series.get(i) - fitted.get(i);
            mse += e * e;
        }
        double sigma = series.size() > 0 ? Math.sqrt(mse / Math.max(1, series.size())) : 0.0;
        ForecastResult result = new ForecastResult();
        for (int i = 0; i < months.size(); i++) {
            YearMonth ym = months.get(i);
            double val = series.get(i);
            result.getHistory().add(new ForecastPoint(ym, val, null, null));
        }
        YearMonth last = months.isEmpty() ? YearMonth.now() : months.get(months.size() - 1);
        double lastLevel = fitted.isEmpty() ? 0.0 : fitted.get(fitted.size() - 1);
        for (int h = 1; h <= horizonMonths; h++) {
            last = last.plusMonths(1);
            double forecast = lastLevel;
            double lower = forecast - 1.96 * sigma;
            double upper = forecast + 1.96 * sigma;
            result.getForecast().add(new ForecastPoint(last, forecast, lower, upper));
        }
        if (!result.getForecast().isEmpty()) {
            for (ForecastPoint fp : result.getForecast()) {
                if (fp.getUpper() != null && fp.getUpper() < 0) {
                    result.getAlerts().add("Risque: ventes négatives prévues");
                    break;
                }
            }
        }
        return result;
    }
}
