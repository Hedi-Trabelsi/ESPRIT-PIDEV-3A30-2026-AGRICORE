package Model;

import java.time.YearMonth;

public class ForecastPoint {
    private YearMonth period;
    private double value;
    private Double lower;
    private Double upper;

    public ForecastPoint() {
    }

    public ForecastPoint(YearMonth period, double value, Double lower, Double upper) {
        this.period = period;
        this.value = value;
        this.lower = lower;
        this.upper = upper;
    }

    public YearMonth getPeriod() {
        return period;
    }

    public void setPeriod(YearMonth period) {
        this.period = period;
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }

    public Double getLower() {
        return lower;
    }

    public void setLower(Double lower) {
        this.lower = lower;
    }

    public Double getUpper() {
        return upper;
    }

    public void setUpper(Double upper) {
        this.upper = upper;
    }
}
