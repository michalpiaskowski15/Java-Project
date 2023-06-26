import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartFrame;
import org.jfree.chart.JFreeChart;
import org.jfree.data.category.DefaultCategoryDataset;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import java.text.SimpleDateFormat;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.*;

public class Main {
    // logowanie
    private static final String DATABASE_URL = "jdbc:mysql://localhost:3306/weatherdb";
    private static final String DATABASE_USERNAME = "root";
    private static final String DATABASE_PASSWORD = "bazydanych";

    // wpisz API
    private static final String API_KEY = "5a470a5022cab7d27acb8dd244dfb97f";

    public static void main(String[] args) {
        try {
            // Pobierz dane pogodowe ze strony
            String weatherData = getWeatherData();
            if (weatherData != null) {
                // Parsuj dane pogodowe
                JSONObject json = (JSONObject) new JSONParser().parse(weatherData);
                JSONObject mainData = (JSONObject) json.get("main");
                double temperature = ((Number) mainData.get("temp")).doubleValue();
                double pressure = ((Number) mainData.get("pressure")).doubleValue();
                double humidity = ((Number) mainData.get("humidity")).doubleValue();


                // Zapisz dane do bd
                saveWeatherData(temperature, pressure, humidity);

                // Generuj wykresy
                generateCharts();
            }
        } catch (ParseException | IOException | SQLException e) {
            e.printStackTrace();
        }
    }

    private static String getWeatherData() throws IOException {
        String city = "Warsaw";
        String apiUrl = "http://api.openweathermap.org/data/2.5/weather?q=" + city + "&appid=" + API_KEY;
        URL url = new URL(apiUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");

        int responseCode = connection.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();
            return response.toString();
        } else {
            System.out.println("Błąd podczas pobierania danych z API. Kod odpowiedzi: " + responseCode);
        }

        return null;
    }

    private static void saveWeatherData(double temperature, double pressure, double humidity) throws SQLException {
        Connection connection = DriverManager.getConnection(DATABASE_URL, DATABASE_USERNAME, DATABASE_PASSWORD);
        String query = "INSERT INTO weather (temperature, pressure, humidity) VALUES (?, ?, ?)";
        PreparedStatement statement = connection.prepareStatement(query);
        statement.setDouble(1, temperature);
        statement.setDouble(2, pressure);
        statement.setDouble(3, humidity);
        statement.executeUpdate();
        statement.close();
        connection.close();
    }

    private static void generateCharts() throws SQLException {
        Connection connection = DriverManager.getConnection(DATABASE_URL, DATABASE_USERNAME, DATABASE_PASSWORD);
        Statement statement = connection.createStatement();
        String query = "SELECT * FROM weather";
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();

        java.sql.ResultSet resultSet = statement.executeQuery(query);
        while (resultSet.next()) {
            double temperature = resultSet.getDouble("temperature");
            double pressure = resultSet.getDouble("pressure");
            double humidity = resultSet.getDouble("humidity");
            Timestamp timestamp = resultSet.getTimestamp("timestamp");

            dataset.addValue(temperature, "Temperatura", timestamp.toString());
            dataset.addValue(pressure, "Ciśnienie", timestamp.toString());
            dataset.addValue(humidity, "Wilgotność", timestamp.toString());
        }

        resultSet.close();
        statement.close();
        connection.close();

        // wykres
        JFreeChart chart = ChartFactory.createLineChart(
                "Dane pogodowe", // Tytuł wykresu
                "Czas", // Etykieta osi X
                "Wartość", // Etykieta osi Y
                dataset // Zbiór danych
        );

        // wyswietlanie
        ChartFrame frame = new ChartFrame("Wykres", chart);
        frame.pack();
        frame.setVisible(true);
    }
}
