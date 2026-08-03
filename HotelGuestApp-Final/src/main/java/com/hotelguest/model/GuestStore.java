package com.hotelguest.model;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.*;
import java.time.*;
import java.time.format.*;
import java.util.ArrayList;
import java.util.List;

public class GuestStore {

    private List<Guest> guests = new ArrayList<>();
    private final Path dataFile;
    private final Gson gson;

    public GuestStore() {
        this.gson = buildGson();
        Path dir = Path.of(System.getProperty("user.home"), ".hotelguestapp");
        try { Files.createDirectories(dir); } catch (IOException ignored) {}
        this.dataFile = dir.resolve("guests.json");
        load();
        if (guests.isEmpty()) insertSampleData();
    }

    // ── CRUD ─────────────────────────────────────────────────────────────────

    public List<Guest> getGuests() { return guests; }

    public void add(Guest g) {
        g.getHistory().add(new HistoryEvent("Record created", HistoryEvent.EventType.CREATED));
        guests.add(g);
        save();
    }

    public void update(Guest updated) {
        for (int i = 0; i < guests.size(); i++) {
            if (guests.get(i).getId().equals(updated.getId())) {
                updated.getHistory().add(new HistoryEvent("Record updated", HistoryEvent.EventType.UPDATED));
                guests.set(i, updated);
                break;
            }
        }
        save();
    }

    public void delete(Guest g) {
        guests.removeIf(x -> x.getId().equals(g.getId()));
        save();
    }

    public void addBillItem(String guestId, BillItem item) {
        guests.stream().filter(g -> g.getId().equals(guestId)).findFirst().ifPresent(g -> {
            g.getBillItems().add(item);
            String desc = item.isPaid()
                ? "Payment received: " + item.getDescription() + " — $" + String.format("%.2f", item.getAmount())
                : "Charge added: " + item.getDescription() + " — $" + String.format("%.2f", item.getAmount());
            g.getHistory().add(new HistoryEvent(desc, HistoryEvent.EventType.PAYMENT));
        });
        save();
    }

    public void markPaid(String guestId, String billItemId) {
        guests.stream().filter(g -> g.getId().equals(guestId)).findFirst().ifPresent(g -> {
            g.getBillItems().stream().filter(b -> b.getId().equals(billItemId)).findFirst()
                .ifPresent(b -> b.setPaid(true));
            g.getHistory().add(new HistoryEvent("Bill item marked paid", HistoryEvent.EventType.PAYMENT));
        });
        save();
    }

    // ── Aggregates ────────────────────────────────────────────────────────────

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

    // ── Persistence ───────────────────────────────────────────────────────────

    private void save() {
        try (Writer w = Files.newBufferedWriter(dataFile)) {
            gson.toJson(guests, w);
        } catch (IOException e) { e.printStackTrace(); }
    }

    private void load() {
        if (!Files.exists(dataFile)) return;
        try (Reader r = Files.newBufferedReader(dataFile)) {
            Type listType = new TypeToken<List<Guest>>(){}.getType();
            List<Guest> loaded = gson.fromJson(r, listType);
            if (loaded != null) guests = loaded;
        } catch (IOException e) { e.printStackTrace(); }
    }

    // ── Export / Import ───────────────────────────────────────────────────────

    public void exportToFile(File file) throws IOException {
        var container = new ExportContainer(guests);
        try (Writer w = new FileWriter(file)) {
            gson.toJson(container, w);
        }
    }

    public void importFromFile(File file) throws IOException {
        try (Reader r = new FileReader(file)) {
            // First parse as a raw JSON tree so we can inspect it
            JsonElement root = JsonParser.parseReader(r);
            if (!root.isJsonObject()) throw new IOException("Invalid file format.");

            JsonObject obj = root.getAsJsonObject();

            // Support two shapes:
            //  1. Our own export: { "version":1, "exportedAt":"...", "guests":[...] }
            //  2. Raw list written by older versions: [...]
            JsonArray guestArray;
            if (obj.has("guests")) {
                guestArray = obj.getAsJsonArray("guests");
            } else {
                throw new IOException("No 'guests' array found in file.");
            }

            Type listType = new TypeToken<List<Guest>>(){}.getType();
            List<Guest> loaded = gson.fromJson(guestArray, listType);
            if (loaded == null || loaded.isEmpty()) {
                throw new IOException("File contained no guest records.");
            }
            guests = loaded;
            save();
        }
    }

    // ── Gson — tolerant date parsing ──────────────────────────────────────────
    //
    // The iOS Swift app exports LocalDate fields as full ISO-8601 timestamps,
    // e.g. "2025-01-15T00:00:00+02:00" or "2025-01-15T00:00:00Z".
    // The Java app writes plain dates like "2025-01-15".
    // The deserializers below accept ALL of these formats.

    private static LocalDate parseFlexibleDate(String s) {
        if (s == null || s.isBlank()) return null;
        // Plain date: 2025-01-15
        try { return LocalDate.parse(s, DateTimeFormatter.ISO_LOCAL_DATE); }
        catch (DateTimeParseException ignored) {}
        // Full ISO with offset: 2025-01-15T00:00:00+02:00 or Z
        try { return OffsetDateTime.parse(s, DateTimeFormatter.ISO_OFFSET_DATE_TIME).toLocalDate(); }
        catch (DateTimeParseException ignored) {}
        // ISO instant: 2025-01-15T00:00:00Z
        try { return Instant.parse(s).atZone(ZoneId.of("UTC")).toLocalDate(); }
        catch (DateTimeParseException ignored) {}
        // Local datetime without offset: 2025-01-15T00:00:00
        try { return LocalDateTime.parse(s, DateTimeFormatter.ISO_LOCAL_DATE_TIME).toLocalDate(); }
        catch (DateTimeParseException ignored) {}
        throw new JsonParseException("Cannot parse date: " + s);
    }

    private static LocalDateTime parseFlexibleDateTime(String s) {
        if (s == null || s.isBlank()) return null;
        // Plain local datetime
        try { return LocalDateTime.parse(s, DateTimeFormatter.ISO_LOCAL_DATE_TIME); }
        catch (DateTimeParseException ignored) {}
        // With timezone offset
        try { return OffsetDateTime.parse(s, DateTimeFormatter.ISO_OFFSET_DATE_TIME).toLocalDateTime(); }
        catch (DateTimeParseException ignored) {}
        // Instant / Z suffix
        try { return Instant.parse(s).atZone(ZoneId.of("UTC")).toLocalDateTime(); }
        catch (DateTimeParseException ignored) {}
        // Plain date only — treat as midnight
        try { return LocalDate.parse(s, DateTimeFormatter.ISO_LOCAL_DATE).atStartOfDay(); }
        catch (DateTimeParseException ignored) {}
        throw new JsonParseException("Cannot parse datetime: " + s);
    }

    private static Gson buildGson() {
        return new GsonBuilder()
            .setPrettyPrinting()
            // LocalDate — write as plain date, read anything
            .registerTypeAdapter(LocalDate.class,
                (JsonSerializer<LocalDate>)
                    (src, type, ctx) -> new JsonPrimitive(src.format(DateTimeFormatter.ISO_LOCAL_DATE)))
            .registerTypeAdapter(LocalDate.class,
                (JsonDeserializer<LocalDate>)
                    (json, type, ctx) -> parseFlexibleDate(json.getAsString()))
            // LocalDateTime — write as plain datetime, read anything
            .registerTypeAdapter(LocalDateTime.class,
                (JsonSerializer<LocalDateTime>)
                    (src, type, ctx) -> new JsonPrimitive(src.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)))
            .registerTypeAdapter(LocalDateTime.class,
                (JsonDeserializer<LocalDateTime>)
                    (json, type, ctx) -> parseFlexibleDateTime(json.getAsString()))
            .create();
    }

    // ── Sample data ───────────────────────────────────────────────────────────

    private void insertSampleData() {
        Guest g1 = new Guest();
        g1.setName("James Harrington");
        g1.setAddress("14 Kensington Gardens, London");
        g1.setCountry("United Kingdom");
        g1.setPassportNumber("GB123456789");
        g1.setNumberOfGuests(2);
        g1.setRoomType(Guest.RoomType.SUITE);
        g1.setFlag(Guest.GuestFlag.VERY_GOOD);
        g1.setComments("Prefers high floor. Champagne on arrival.");
        g1.setRoomRatePerNight(320);
        g1.setCarHired(true); g1.setCarType(Guest.CarType.LUXURY); g1.setCarRatePerDay(85);
        g1.setCheckOutDate(LocalDate.now().plusDays(5));
        g1.getBillItems().add(new BillItem("Minibar", 45, true));
        g1.getBillItems().add(new BillItem("Spa treatment", 120, false));
        g1.getHistory().add(new HistoryEvent("Record created", HistoryEvent.EventType.CREATED));

        Guest g2 = new Guest();
        g2.setName("Sofia Marchetti");
        g2.setAddress("Via Roma 22, Milan");
        g2.setCountry("Italy");
        g2.setPassportNumber("IT987654321");
        g2.setRoomType(Guest.RoomType.SINGLE);
        g2.setRoomRatePerNight(95);
        g2.setCarHired(true); g2.setCarType(Guest.CarType.COMPACT); g2.setCarRatePerDay(35);
        g2.setCheckOutDate(LocalDate.now().plusDays(3));
        g2.getHistory().add(new HistoryEvent("Record created", HistoryEvent.EventType.CREATED));

        Guest g3 = new Guest();
        g3.setName("Robert Dunning");
        g3.setAddress("88 Fifth Avenue, New York");
        g3.setCountry("USA");
        g3.setPassportNumber("US556677889");
        g3.setNumberOfGuests(4);
        g3.setRoomType(Guest.RoomType.FAMILY_ROOM);
        g3.setFlag(Guest.GuestFlag.UNRELIABLE);
        g3.setComments("Late check-out requested twice without notice.");
        g3.setRoomRatePerNight(210);
        g3.setCarHired(true); g3.setCarType(Guest.CarType.SUV); g3.setCarRatePerDay(60);
        g3.setCheckOutDate(LocalDate.now().plusDays(7));
        g3.getBillItems().add(new BillItem("Room service", 88, false));
        g3.getHistory().add(new HistoryEvent("Record created", HistoryEvent.EventType.CREATED));

        guests = new ArrayList<>(List.of(g1, g2, g3));
        save();
    }

    // ── Export container ──────────────────────────────────────────────────────

    static class ExportContainer {
        int version = 1;
        String exportedAt = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        List<Guest> guests;
        ExportContainer(List<Guest> guests) { this.guests = guests; }
    }
}
