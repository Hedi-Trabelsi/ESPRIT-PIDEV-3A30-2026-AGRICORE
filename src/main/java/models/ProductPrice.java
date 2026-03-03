package models;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class ProductPrice {

    private StringProperty product;
    private StringProperty marketPrice;

    public ProductPrice(String product, String marketPrice) {
        this.product = new SimpleStringProperty(product);
        this.marketPrice = new SimpleStringProperty(marketPrice);
    }

    public String getProduct() {
        return product.get();
    }

    public void setProduct(String product) {
        this.product.set(product);
    }

    public StringProperty productProperty() {
        return product;
    }

    public String getMarketPrice() {
        return marketPrice.get();
    }

    public void setMarketPrice(String marketPrice) {
        this.marketPrice.set(marketPrice);
    }

    public StringProperty marketPriceProperty() {
        return marketPrice;
    }
}