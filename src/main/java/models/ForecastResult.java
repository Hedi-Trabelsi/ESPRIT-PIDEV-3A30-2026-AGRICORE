package models;

import java.util.ArrayList;
import java.util.List;

public class ForecastResult {
    private List<ForecastPoint> history = new ArrayList<>();
    private List<ForecastPoint> forecast = new ArrayList<>();
    private List<String> alerts = new ArrayList<>();

    public List<ForecastPoint> getHistory() {
        return history;
    }

    public void setHistory(List<ForecastPoint> history) {
        this.history = history;
    }

    public List<ForecastPoint> getForecast() {
        return forecast;
    }

    public void setForecast(List<ForecastPoint> forecast) {
        this.forecast = forecast;
    }

    public List<String> getAlerts() {
        return alerts;
    }

    public void setAlerts(List<String> alerts) {
        this.alerts = alerts;
    }
}
