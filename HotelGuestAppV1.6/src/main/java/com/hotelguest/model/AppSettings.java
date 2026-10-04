package com.hotelguest.model;

import java.util.*;

public class AppSettings {
    private String displayCurrency = "ZMW";
    private Map<String, Double> exchangeRates = new LinkedHashMap<>();
    private String ratesUpdatedAt = null;
    // ── Server sync ───────────────────────────────────────────────────────────
    // syncApiUrl:  e.g. https://www.nduyaka.com/nduyaka-staff/api/sync.php
    // syncApiKey:  the secret key you set in sync.php on Bluehost
    private String syncApiUrl = "";
    private String syncApiKey = "";

    private Map<String, Double> roomRates = new LinkedHashMap<>(Map.of(
        "Single", 1500.0, "Double", 2500.0, "Twin", 2200.0, "Suite", 6000.0,
        "Penthouse", 12000.0, "Deluxe", 4000.0, "Family Room", 5000.0
    ));
    private Map<String, Double> carRates = new LinkedHashMap<>(Map.of(
        "Economy", 400.0, "Compact", 550.0, "Midsize", 700.0,
        "SUV", 1000.0, "Luxury", 1800.0, "Van", 1200.0
    ));
    private Map<String, Double> breakfastPrices = new LinkedHashMap<>(Map.of(
        "Continental", 150.0, "Full English", 250.0, "Vegetarian", 200.0
    ));

    public double toDisplay(double zmw) {
        if ("ZMW".equals(displayCurrency)) return zmw;
        return zmw * exchangeRates.getOrDefault(displayCurrency, 1.0);
    }

    public String currencySymbol() {
        return switch (displayCurrency) {
            case "EUR" -> "€"; case "USD" -> "$"; case "GBP" -> "£";
            case "ZMW" -> "K"; default -> displayCurrency + " ";
        };
    }

    public String formatted(double zmw) {
        return currencySymbol() + String.format("%.2f", toDisplay(zmw));
    }

    public String getDisplayCurrency()                       { return displayCurrency; }
    public void   setDisplayCurrency(String c)               { this.displayCurrency = c; }
    public Map<String,Double> getExchangeRates()             { return exchangeRates; }
    public void   setExchangeRates(Map<String,Double> r)     { this.exchangeRates = r; }
    public String getRatesUpdatedAt()                        { return ratesUpdatedAt; }
    public void   setRatesUpdatedAt(String d)                { this.ratesUpdatedAt = d; }
    public Map<String,Double> getRoomRates()                 { return roomRates; }
    public void   setRoomRates(Map<String,Double> r)         { this.roomRates = r; }
    public Map<String,Double> getCarRates()                  { return carRates; }
    public void   setCarRates(Map<String,Double> r)          { this.carRates = r; }
    public Map<String,Double> getBreakfastPrices()           { return breakfastPrices; }
    public void   setBreakfastPrices(Map<String,Double> p)   { this.breakfastPrices = p; }

    public String getSyncApiUrl()              { return syncApiUrl != null ? syncApiUrl : ""; }
    public void   setSyncApiUrl(String u)      { this.syncApiUrl = u; }
    public String getSyncApiKey()              { return syncApiKey != null ? syncApiKey : ""; }
    public void   setSyncApiKey(String k)      { this.syncApiKey = k; }

    public List<String> supportedCurrencies() {
        List<String> all = new ArrayList<>(exchangeRates.keySet());
        if (!all.contains("ZMW")) all.add(0, "ZMW");
        Collections.sort(all);
        return all;
    }
}
