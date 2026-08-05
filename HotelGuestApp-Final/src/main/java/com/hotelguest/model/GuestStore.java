package com.hotelguest.model;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.lang.reflect.Type;
import java.net.*;
import java.nio.file.*;
import java.time.*;
import java.time.format.*;
import java.util.*;

public class GuestStore {

    private List<Guest> guests = new ArrayList<>();
    private AppSettings settings = new AppSettings();
    private final Path dataFile;
    private final Path settingsFile;
    private final Path ratesCacheFile;
    private final Gson gson;

    public GuestStore() {
        this.gson = buildGson();
        Path dir = Path.of(System.getProperty("user.home"), ".hotelguestapp");
        try { Files.createDirectories(dir); } catch (IOException ignored) {}
        this.dataFile     = dir.resolve("guests.json");
        this.settingsFile = dir.resolve("settings.json");
        this.ratesCacheFile = dir.resolve("rates_cache.json");
        loadSettings();
        load();
        if (guests.isEmpty()) insertSampleData();
        fetchExchangeRatesIfNeeded();
    }

    // ── CRUD ──────────────────────────────────────────────────────────────────

    public List<Guest>  getGuests()   { return guests; }
    public AppSettings  getSettings() { return settings; }

    public void add(Guest g) {
        g.getHistory().add(new HistoryEvent("Record created", HistoryEvent.EventType.CREATED));
        guests.add(g); save();
    }
    public void update(Guest updated) {
        for (int i = 0; i < guests.size(); i++) {
            if (guests.get(i).getId().equals(updated.getId())) {
                updated.getHistory().add(new HistoryEvent("Record updated", HistoryEvent.EventType.UPDATED));
                guests.set(i, updated); break;
            }
        }
        save();
    }
    public void delete(Guest g) { guests.removeIf(x -> x.getId().equals(g.getId())); save(); }

    public void addBillItem(String guestId, BillItem item) {
        guests.stream().filter(g -> g.getId().equals(guestId)).findFirst().ifPresent(g -> {
            g.getBillItems().add(item);
            String desc = item.isPaid()
                ? "Payment received: " + item.getDescription() + " — " + settings.formatted(item.getAmount())
                : "Charge added: "     + item.getDescription() + " — " + settings.formatted(item.getAmount());
            g.getHistory().add(new HistoryEvent(desc, HistoryEvent.EventType.PAYMENT));
        }); save();
    }
    public void markPaid(String guestId, String billItemId) {
        guests.stream().filter(g -> g.getId().equals(guestId)).findFirst().ifPresent(g -> {
            g.getBillItems().stream().filter(b -> b.getId().equals(billItemId))
                .findFirst().ifPresent(b -> b.setPaid(true));
            g.getHistory().add(new HistoryEvent("Bill item marked paid", HistoryEvent.EventType.PAYMENT));
        }); save();
    }
    public void markCarDamaged(String guestId, double damageAmount) {
        guests.stream().filter(g -> g.getId().equals(guestId)).findFirst().ifPresent(g -> {
            g.setCarCondition(Guest.CarCondition.DAMAGED);
            BillItem item = new BillItem("Car damage charge", damageAmount, false);
            g.getBillItems().add(item);
            g.getHistory().add(new HistoryEvent(
                "Car returned damaged — charge: " + settings.formatted(damageAmount),
                HistoryEvent.EventType.PAYMENT));
        }); save();
    }

    // ── Aggregates ─────────────────────────────────────────────────────────────

    public double getTotalRevenue()   { return guests.stream().mapToDouble(Guest::getTotalCharged).sum(); }
    public double getTotalProfit()    { return guests.stream().mapToDouble(Guest::getProfit).sum(); }
    public double getTotalOpenBills() { return guests.stream().mapToDouble(Guest::getOpenBill).sum(); }

    public double getProfitForWeek(LocalDate date) {
        LocalDate start = date.with(java.time.DayOfWeek.MONDAY);
        LocalDate end   = start.plusWeeks(1);
        return guests.stream()
            .filter(g -> g.getCheckInDate() != null
                && !g.getCheckInDate().isBefore(start)
                && g.getCheckInDate().isBefore(end))
            .mapToDouble(Guest::getProfit).sum();
    }

    // ── Settings ───────────────────────────────────────────────────────────────

    public void saveSettings() {
        try (Writer w = Files.newBufferedWriter(settingsFile)) {
            gson.toJson(settings, w);
        } catch (IOException e) { e.printStackTrace(); }
    }
    private void loadSettings() {
        if (!Files.exists(settingsFile)) return;
        try (Reader r = Files.newBufferedReader(settingsFile)) {
            AppSettings s = gson.fromJson(r, AppSettings.class);
            if (s != null) settings = s;
        } catch (IOException e) { e.printStackTrace(); }
    }

    // ── Exchange rates ─────────────────────────────────────────────────────────

    public void fetchExchangeRatesIfNeeded() {
        // Check cache
        if (Files.exists(ratesCacheFile)) {
            try (Reader r = Files.newBufferedReader(ratesCacheFile)) {
                JsonObject cache = JsonParser.parseReader(r).getAsJsonObject();
                String date = cache.get("date").getAsString();
                if (LocalDate.now().toString().equals(date)) {
                    // Already fetched today
                    Map<String,Double> rates = gson.fromJson(cache.get("rates"),
                        new TypeToken<Map<String,Double>>(){}.getType());
                    settings.setExchangeRates(rates);
                    return;
                }
            } catch (Exception ignored) {}
        }
        // Fetch in background thread
        new Thread(this::fetchRates).start();
    }

    private void fetchRates() {
        try {
            URL url = new URL("https://api.exchangerate-api.com/v4/latest/ZMW");
            String json;
            try (var in = url.openStream()) { json = new String(in.readAllBytes()); }
            JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
            JsonObject ratesJson = obj.getAsJsonObject("rates");
            Map<String,Double> rates = new LinkedHashMap<>();
            ratesJson.entrySet().forEach(e -> rates.put(e.getKey(), e.getValue().getAsDouble()));
            settings.setExchangeRates(rates);
            settings.setRatesUpdatedAt(LocalDate.now().toString());
            saveSettings();
            // Cache
            JsonObject cache = new JsonObject();
            cache.addProperty("date", LocalDate.now().toString());
            cache.add("rates", ratesJson);
            try (Writer w = Files.newBufferedWriter(ratesCacheFile)) { gson.toJson(cache, w); }
        } catch (Exception e) { System.out.println("Exchange rate fetch failed: " + e.getMessage()); }
    }

    // ── Persistence ─────────────────────────────────────────────────────────────

    private void save() {
        try (Writer w = Files.newBufferedWriter(dataFile)) { gson.toJson(guests, w); }
        catch (IOException e) { e.printStackTrace(); }
    }
    private void load() {
        if (!Files.exists(dataFile)) return;
        try (Reader r = Files.newBufferedReader(dataFile)) {
            Type listType = new TypeToken<List<Guest>>(){}.getType();
            List<Guest> loaded = gson.fromJson(r, listType);
            if (loaded != null) guests = loaded;
        } catch (IOException e) { e.printStackTrace(); }
    }

    // ── Export / Import ──────────────────────────────────────────────────────────

    public void exportToFile(File file) throws IOException {
        ExportContainer container = new ExportContainer(settings, guests);
        try (Writer w = new FileWriter(file)) { gson.toJson(container, w); }
    }

    public void importFromFile(File file) throws IOException {
        try (Reader r = new FileReader(file)) {
            JsonElement root = JsonParser.parseReader(r);
            if (!root.isJsonObject()) throw new IOException("Invalid format.");
            JsonObject obj = root.getAsJsonObject();
            if (obj.has("guests")) {
                Type listType = new TypeToken<List<Guest>>(){}.getType();
                List<Guest> loaded = gson.fromJson(obj.get("guests"), listType);
                if (loaded == null) throw new IOException("No guests found.");
                guests = loaded;
                // Import settings but keep display currency
                if (obj.has("settings")) {
                    String prevCurrency = settings.getDisplayCurrency();
                    AppSettings s = gson.fromJson(obj.get("settings"), AppSettings.class);
                    if (s != null) { settings = s; settings.setDisplayCurrency(prevCurrency); }
                }
                saveSettings(); save();
            } else throw new IOException("No 'guests' array found.");
        }
    }

    // ── Gson ───────────────────────────────────────────────────────────────────

    private static LocalDate parseFlexibleDate(String s) {
        if (s == null || s.isBlank()) return null;
        try { return LocalDate.parse(s, DateTimeFormatter.ISO_LOCAL_DATE); } catch (DateTimeParseException ignored) {}
        try { return OffsetDateTime.parse(s, DateTimeFormatter.ISO_OFFSET_DATE_TIME).toLocalDate(); } catch (DateTimeParseException ignored) {}
        try { return Instant.parse(s).atZone(ZoneId.of("UTC")).toLocalDate(); } catch (DateTimeParseException ignored) {}
        try { return LocalDateTime.parse(s, DateTimeFormatter.ISO_LOCAL_DATE_TIME).toLocalDate(); } catch (DateTimeParseException ignored) {}
        return null;
    }

    private static LocalDateTime parseFlexibleDateTime(String s) {
        if (s == null || s.isBlank()) return null;
        try { return LocalDateTime.parse(s, DateTimeFormatter.ISO_LOCAL_DATE_TIME); } catch (DateTimeParseException ignored) {}
        try { return OffsetDateTime.parse(s, DateTimeFormatter.ISO_OFFSET_DATE_TIME).toLocalDateTime(); } catch (DateTimeParseException ignored) {}
        try { return Instant.parse(s).atZone(ZoneId.of("UTC")).toLocalDateTime(); } catch (DateTimeParseException ignored) {}
        try { return LocalDate.parse(s, DateTimeFormatter.ISO_LOCAL_DATE).atStartOfDay(); } catch (DateTimeParseException ignored) {}
        return null;
    }

    private static <E extends Enum<E>> JsonSerializer<E> enumSerializer(
            java.util.function.Function<E, String> labelGetter) {
        return (src, type, ctx) -> new JsonPrimitive(labelGetter.apply(src));
    }

    private static <E extends Enum<E>> JsonDeserializer<E> enumDeserializer(
            Class<E> cls, java.util.function.Function<E, String> labelGetter) {
        return (json, type, ctx) -> {
            String s = json.getAsString().trim();
            for (E c : cls.getEnumConstants()) {
                if (c.name().equalsIgnoreCase(s)) return c;
                if (labelGetter.apply(c).equalsIgnoreCase(s)) return c;
            }
            String norm = s.toUpperCase().replace(" ", "_");
            for (E c : cls.getEnumConstants()) if (c.name().equals(norm)) return c;
            return cls.getEnumConstants()[0];
        };
    }

    private static Gson buildGson() {
        return new GsonBuilder().setPrettyPrinting()
            .registerTypeAdapter(LocalDate.class,
                (JsonSerializer<LocalDate>) (src, t, ctx) -> new JsonPrimitive(src.format(DateTimeFormatter.ISO_LOCAL_DATE)))
            .registerTypeAdapter(LocalDate.class,
                (JsonDeserializer<LocalDate>) (json, t, ctx) -> parseFlexibleDate(json.getAsString()))
            .registerTypeAdapter(LocalDateTime.class,
                (JsonSerializer<LocalDateTime>) (src, t, ctx) -> new JsonPrimitive(src.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)))
            .registerTypeAdapter(LocalDateTime.class,
                (JsonDeserializer<LocalDateTime>) (json, t, ctx) -> parseFlexibleDateTime(json.getAsString()))
            .registerTypeAdapter(Guest.RoomType.class,
                enumSerializer(Guest.RoomType::getLabel))
            .registerTypeAdapter(Guest.RoomType.class,
                enumDeserializer(Guest.RoomType.class, Guest.RoomType::getLabel))
            .registerTypeAdapter(Guest.CarType.class,
                enumSerializer(Guest.CarType::getLabel))
            .registerTypeAdapter(Guest.CarType.class,
                enumDeserializer(Guest.CarType.class, Guest.CarType::getLabel))
            .registerTypeAdapter(Guest.GuestFlag.class,
                enumSerializer(Guest.GuestFlag::getLabel))
            .registerTypeAdapter(Guest.GuestFlag.class,
                enumDeserializer(Guest.GuestFlag.class, Guest.GuestFlag::getLabel))
            .registerTypeAdapter(Guest.BreakfastType.class,
                enumSerializer(Guest.BreakfastType::getLabel))
            .registerTypeAdapter(Guest.BreakfastType.class,
                enumDeserializer(Guest.BreakfastType.class, Guest.BreakfastType::getLabel))
            .registerTypeAdapter(Guest.CarCondition.class,
                enumSerializer(Guest.CarCondition::getLabel))
            .registerTypeAdapter(Guest.CarCondition.class,
                enumDeserializer(Guest.CarCondition.class, Guest.CarCondition::getLabel))
            .registerTypeAdapter(HistoryEvent.EventType.class,
                enumSerializer(Enum::name))
            .registerTypeAdapter(HistoryEvent.EventType.class,
                enumDeserializer(HistoryEvent.EventType.class, Enum::name))
            .create();
    }

    // ── Export container ──────────────────────────────────────────────────────

    static class ExportContainer {
        int version = 2;
        String exportedAt = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        AppSettings settings;
        List<Guest> guests;
        ExportContainer(AppSettings settings, List<Guest> guests) {
            this.settings = settings; this.guests = guests;
        }
    }

    // ── Sample data ────────────────────────────────────────────────────────────

    private void insertSampleData() {
        Guest g1 = new Guest();
        g1.setName("James Harrington"); g1.setAddress("14 Kensington Gardens, London");
        g1.setCountry("United Kingdom"); g1.setPassportNumber("GB123456789");
        g1.setNumberOfGuests(2); g1.setRoomType(Guest.RoomType.SUITE);
        g1.setFlag(Guest.GuestFlag.VERY_GOOD);
        g1.setComments("Prefers high floor. Champagne on arrival.");
        g1.setRoomRatePerNight(settings.getRoomRates().getOrDefault("Suite", 6000.0));
        g1.setCarHired(true); g1.setCarType(Guest.CarType.LUXURY);
        g1.setCarInsurance(true); g1.setCarCondition(Guest.CarCondition.UNDAMAGED);
        g1.setCarRatePerDay(settings.getCarRates().getOrDefault("Luxury", 1800.0));
        g1.setBreakfastType(Guest.BreakfastType.FULL_ENGLISH);
        g1.setBreakfastPricePerPersonPerNight(settings.getBreakfastPrices().getOrDefault("Full English", 250.0));
        g1.setCheckOutDate(LocalDate.now().plusDays(5));
        g1.getBillItems().add(new BillItem("Minibar", 450, true));
        g1.getBillItems().add(new BillItem("Spa treatment", 1200, false));
        g1.getHistory().add(new HistoryEvent("Record created", HistoryEvent.EventType.CREATED));

        Guest g2 = new Guest();
        g2.setName("Sofia Marchetti"); g2.setAddress("Via Roma 22, Milan");
        g2.setCountry("Italy"); g2.setPassportNumber("IT987654321");
        g2.setRoomType(Guest.RoomType.SINGLE);
        g2.setRoomRatePerNight(settings.getRoomRates().getOrDefault("Single", 1500.0));
        g2.setCarHired(true); g2.setCarType(Guest.CarType.COMPACT);
        g2.setCarInsurance(false); g2.setCarCondition(Guest.CarCondition.PENDING);
        g2.setCarRatePerDay(settings.getCarRates().getOrDefault("Compact", 550.0));
        g2.setBreakfastType(Guest.BreakfastType.CONTINENTAL);
        g2.setBreakfastPricePerPersonPerNight(settings.getBreakfastPrices().getOrDefault("Continental", 150.0));
        g2.setCheckOutDate(LocalDate.now().plusDays(3));
        g2.getHistory().add(new HistoryEvent("Record created", HistoryEvent.EventType.CREATED));

        Guest g3 = new Guest();
        g3.setName("Robert Dunning"); g3.setAddress("88 Fifth Avenue, New York");
        g3.setCountry("USA"); g3.setPassportNumber("US556677889");
        g3.setNumberOfGuests(4); g3.setRoomType(Guest.RoomType.FAMILY_ROOM);
        g3.setFlag(Guest.GuestFlag.UNRELIABLE);
        g3.setComments("Late check-out requested twice without notice.");
        g3.setRoomRatePerNight(settings.getRoomRates().getOrDefault("Family Room", 5000.0));
        g3.setCarHired(true); g3.setCarType(Guest.CarType.SUV);
        g3.setCarInsurance(true); g3.setCarCondition(Guest.CarCondition.DAMAGED);
        g3.setCarRatePerDay(settings.getCarRates().getOrDefault("SUV", 1000.0));
        g3.setBreakfastType(Guest.BreakfastType.NONE);
        g3.setCheckOutDate(LocalDate.now().plusDays(7));
        g3.getBillItems().add(new BillItem("Room service", 880, false));
        g3.getBillItems().add(new BillItem("Car damage charge", 3500, false));
        g3.getHistory().add(new HistoryEvent("Record created", HistoryEvent.EventType.CREATED));

        guests = new ArrayList<>(List.of(g1, g2, g3)); save();
    }
}
