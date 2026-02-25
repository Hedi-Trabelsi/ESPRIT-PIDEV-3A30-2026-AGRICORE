package services.ai;

import models.ForecastPoint;
import models.ForecastResult;
import models.Vente;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class DefaultForecastingServiceTest {
    private List<Vente> ventes(double... monthlyValues) {
        List<Vente> list = new ArrayList<>();
        YearMonth start = YearMonth.of(2025, 1);
        for (int i = 0; i < monthlyValues.length; i++) {
            YearMonth ym = start.plusMonths(i);
            LocalDate date = ym.atDay(1);
            double ca = monthlyValues[i];
            Vente v = new Vente(0, 1, null, 0.0, 0.0, ca, date, "X", null);
            list.add(v);
        }
        return list;
    }

    @Test
    void horizonAndHistoryPresence() {
        DefaultForecastingService svc = new DefaultForecastingService();
        List<Vente> hist = ventes(100, 120, 110, 140, 160);
        ForecastResult fr = svc.forecastUserSales(1, hist, 6);
        assertEquals(5, fr.getHistory().size());
        assertEquals(6, fr.getForecast().size());
    }

    @Test
    void confidenceIntervalsCoherence() {
        DefaultForecastingService svc = new DefaultForecastingService();
        List<Vente> hist = ventes(100, 120, 110, 140, 160);
        ForecastResult fr = svc.forecastUserSales(1, hist, 6);
        for (ForecastPoint p : fr.getForecast()) {
            assertNotNull(p.getLower());
            assertNotNull(p.getUpper());
            assertTrue(p.getLower() <= p.getUpper());
            assertTrue(p.getLower() <= p.getValue());
            assertTrue(p.getValue() <= p.getUpper());
        }
    }

    @Test
    void forecastMonthsAreConsecutive() {
        DefaultForecastingService svc = new DefaultForecastingService();
        List<Vente> hist = ventes(100, 120, 110, 140, 160);
        ForecastResult fr = svc.forecastUserSales(1, hist, 6);
        YearMonth lastHist = fr.getHistory().get(fr.getHistory().size() - 1).getPeriod();
        YearMonth expected = lastHist.plusMonths(1);
        for (ForecastPoint p : fr.getForecast()) {
            assertEquals(expected, p.getPeriod());
            expected = expected.plusMonths(1);
        }
    }

    @Test
    void worksWithEmptyHistory() {
        DefaultForecastingService svc = new DefaultForecastingService();
        ForecastResult fr = svc.forecastUserSales(1, new ArrayList<>(), 4);
        assertEquals(0, fr.getHistory().size());
        assertEquals(4, fr.getForecast().size());
        for (ForecastPoint p : fr.getForecast()) {
            assertNotNull(p.getLower());
            assertNotNull(p.getUpper());
            assertTrue(p.getLower() <= p.getUpper());
        }
    }
}
