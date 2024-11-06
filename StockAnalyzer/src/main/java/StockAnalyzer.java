import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONObject;

public class StockAnalyzer {
    private static final String API_URL = "https://www.alphavantage.co/query?function=TIME_SERIES_INTRADAY&symbol=DJT&interval=5min&apikey=VTKIYZP17N2LXAET"; // Changed interval to 5min
    private static final String SYMBOL = "DJT   "; // Name of the symbol

    public static void main(String[] args) {
        try {
            String response = getApiResponse(API_URL);
            JSONObject jsonResponse = new JSONObject(response);

            // Check if the time series key exists
            if (!jsonResponse.has("Time Series (5min)")) {
                System.out.println("Error: No time series data found for the specified symbol.");
                return; // Exit if no data is found
            }

            JSONObject timeSeries = jsonResponse.getJSONObject("Time Series (5min)");

            // Analyze the latest data
            double lastClosePrice = getLatestClosePrice(timeSeries);
            double lastVolume = getLastVolume(timeSeries);
            double highToday = getTodayHigh(timeSeries);
            double lowToday = getTodayLow(timeSeries);
            double currentPrice = lastClosePrice; // Last close price is the current price for this context
            double buyLimit = calculateBuyLimit(lastClosePrice);
            double stopLimit = calculateStopLimit(lastClosePrice);
            double rsi = getRSI(jsonResponse); // Get RSI from the response

            // Calculate price change
            double priceChange = currentPrice - buyLimit;
            double priceChangePercent = (priceChange / buyLimit) * 100;

            // Calculate Fibonacci retracement levels
            double[] fibonacciLevels = calculateFibonacciLevels(highToday, lowToday);
            
            // Wyckoff Volume Analysis
            String wyckoffAnalysis = analyzeWyckoffVolume(timeSeries);

            // Output the results with the new parameters
            System.out.printf("Symbol: %s\n", SYMBOL); // Output symbol name
            System.out.printf("Current Price: %.2f\n", currentPrice); // Display current price
            System.out.printf("Last Close Price: %.2f\n", lastClosePrice); // Display last close price
            System.out.printf("Price Change from Buy Point: %.2f (%.2f%%)\n", priceChange, priceChangePercent);
            System.out.printf("Last Volume: %.0fK\n", lastVolume / 1000); // Format volume with "K"
            System.out.printf("Today's High: %.2f\n", highToday);
            System.out.printf("Today's Low: %.2f\n", lowToday);
            System.out.printf("Recommended Buy Limit: %.2f\n", buyLimit);
            System.out.printf("Recommended Stop Limit: %.2f\n", stopLimit);
            System.out.printf("Relative Strength Index (RSI): %.2f\n", rsi); // Output RSI
            
            // Output Fibonacci levels
            System.out.println("Fibonacci Retracement Levels:");
            System.out.printf("Level 0%%: %.2f\n", lowToday);
            System.out.printf("Level 23.6%%: %.2f\n", fibonacciLevels[0]);
            System.out.printf("Level 38.2%%: %.2f\n", fibonacciLevels[1]);
            System.out.printf("Level 50%%: %.2f\n", fibonacciLevels[2]);
            System.out.printf("Level 61.8%%: %.2f\n", fibonacciLevels[3]);
            System.out.printf("Level 100%%: %.2f\n", highToday);
            
            // Output Wyckoff Volume Analysis
            System.out.println("Wyckoff Volume Analysis: " + wyckoffAnalysis);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String analyzeWyckoffVolume(JSONObject timeSeries) {
        double previousClose = 0.0;
        double currentVolume = 0.0;
        double previousVolume = 0.0;
        int count = 0;

        // Create a list of keys from the JSONObject
        List<String> keys = new ArrayList<>();
        timeSeries.keys().forEachRemaining(keys::add); // Add all keys to the list

        for (String key : keys) {
            if (count == 0) {
                previousClose = getLatestClosePrice(timeSeries);
                previousVolume = getLastVolume(timeSeries);
                count++;
                continue;
            }
            JSONObject dataPoint = timeSeries.getJSONObject(key);
            currentVolume = dataPoint.getDouble("5. volume");
            double currentClose = dataPoint.getDouble("4. close");

            // Wyckoff Method: Volume Analysis
            if (currentClose > previousClose && currentVolume > previousVolume) {
                return "Accumulation Phase Detected (Price up with high volume)";
            } else if (currentClose < previousClose && currentVolume > previousVolume) {
                return "Distribution Phase Detected (Price down with high volume)";
            } else if (currentClose > previousClose && currentVolume < previousVolume) {
                return "Possible Weakness (Price up with low volume)";
            } else if (currentClose < previousClose && currentVolume < previousVolume) {
                return "Possible Strength (Price down with low volume)";
            }

            previousClose = currentClose;
            previousVolume = currentVolume;
        }
        return "No clear phase detected.";
    }

    private static String getApiResponse(String apiUrl) throws Exception {
        URL url = new URL(apiUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");

        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder response = new StringBuilder();
        String inputLine;
        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();

        return response.toString();
    }

    private static double getLatestClosePrice(JSONObject timeSeries) {
        String latestTime = timeSeries.keys().next();
        JSONObject latestData = timeSeries.getJSONObject(latestTime);
        return latestData.getDouble("4. close");
    }

    private static double getLastVolume(JSONObject timeSeries) {
        String latestTime = timeSeries.keys().next();
        JSONObject latestData = timeSeries.getJSONObject(latestTime);
        return latestData.getDouble("5. volume");
    }

    private static double getTodayHigh(JSONObject timeSeries) {
        double todayHigh = 0.0;
        for (String key : timeSeries.keySet()) {
            JSONObject dataPoint = timeSeries.getJSONObject(key);
            double high = dataPoint.getDouble("2. high");
            if (high > todayHigh) {
                todayHigh = high;
            }
        }
        return todayHigh;
    }

    private static double getTodayLow(JSONObject timeSeries) {
        double todayLow = Double.MAX_VALUE;
        for (String key : timeSeries.keySet()) {
            JSONObject dataPoint = timeSeries.getJSONObject(key);
            double low = dataPoint.getDouble("3. low");
            if (low < todayLow) {
                todayLow = low;
            }
        }
        return todayLow;
    }

    private static double calculateBuyLimit(double lastClosePrice) {
        return lastClosePrice * 0.95; // Buy limit logic
    }

    private static double calculateStopLimit(double lastClosePrice) {
        return lastClosePrice * 0.90; // Stop limit logic
    }

    private static double[] calculateFibonacciLevels(double high, double low) {
        double range = high - low;
        return new double[]{
            high - range * 0.236, // 23.6%
            high - range * 0.382, // 38.2%
            high - range * 0.500, // 50%
            high - range * 0.618  // 61.8%
        };
    }

    private static double getRSI(JSONObject jsonResponse) {
        return 50.0; // Placeholder value; implement actual RSI retrieval logic
    }
}
