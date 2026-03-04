package Controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

import Model.ProductPrice;

import java.net.HttpURLConnection;
import java.net.URL;
import java.io.BufferedReader;
import java.io.InputStreamReader;

import com.google.gson.*;

public class MarketController {

    @FXML
    private TableView<ProductPrice> priceTable;

    @FXML
    private TableColumn<ProductPrice,String> productCol;

    @FXML
    private TableColumn<ProductPrice,String> priceCol;


    public void initialize(){

        productCol.setCellValueFactory(new PropertyValueFactory<>("product"));
        priceCol.setCellValueFactory(new PropertyValueFactory<>("marketPrice"));
        priceTable.getStylesheets().add(
                getClass().getResource("/fxml/style.css").toExternalForm() );

    }


    public String callAPI(){

        StringBuilder response = new StringBuilder();

        try{

            URL url = new URL("https://api.open-meteo.com/v1/forecast?latitude=36.80&longitude=10.18&current_weather=true");

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            conn.setRequestMethod("GET");

            int status = conn.getResponseCode();

            if(status != 200){
                System.out.println("API ERROR CODE: " + status);
                return null;
            }

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream())
            );

            String line;

            while((line = reader.readLine()) != null){
                response.append(line);
            }

            reader.close();

        }catch(Exception e){
            e.printStackTrace();
        }

        return response.toString();
    }


    @FXML
    public void loadPrices(){

        String json = callAPI();

        if(json == null){
            System.out.println("API returned no data");
            return;
        }

        JsonObject object = JsonParser.parseString(json).getAsJsonObject();

        JsonObject weather = object.getAsJsonObject("current_weather");

        double temperature = weather.get("temperature").getAsDouble();

        ObservableList<ProductPrice> list = FXCollections.observableArrayList(

                new ProductPrice("Tomatoes", String.format("%.2f TND/kg", temperature * 0.12)),
                new ProductPrice("Potatoes", String.format("%.2f TND/kg", temperature * 0.08)),
                new ProductPrice("Onions", String.format("%.2f TND/kg", temperature * 0.05)),
                new ProductPrice("Carrots", String.format("%.2f TND/kg", temperature * 0.07))

        );

        priceTable.setItems(list);
    }

}
