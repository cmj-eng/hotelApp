package com.hotelguest.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Guest {

    public enum RoomType {
        SINGLE("Single"), DOUBLE("Double"), TWIN("Twin"), SUITE("Suite"),
        PENTHOUSE("Penthouse"), DELUXE("Deluxe"), FAMILY_ROOM("Family Room");
        private final String label;
        RoomType(String label) { this.label = label; }
        public String getLabel() { return label; }
        @Override public String toString() { return label; }
    }

    public enum CarType {
        ECONOMY("Economy"), COMPACT("Compact"), MIDSIZE("Midsize"),
        SUV("SUV"), LUXURY("Luxury"), VAN("Van");
        private final String label;
        CarType(String label) { this.label = label; }
        public String getLabel() { return label; }
        @Override public String toString() { return label; }
    }

    public enum GuestFlag {
        NONE("None"), VERY_GOOD("Very Good"), UNRELIABLE("Unreliable"), BLACKLISTED("Blacklisted");
        private final String label;
        GuestFlag(String label) { this.label = label; }
        public String getLabel() { return label; }
        @Override public String toString() { return label; }
    }

    private String id;
    private String name;
    private String address;
    private String passportNumber;
    private int numberOfGuests;
    private LocalDate birthdate;
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private RoomType roomType;
    private boolean carHired;
    private CarType carType;
    private String comments;
    private GuestFlag flag;
    private LocalDate createdAt;
    private double roomRatePerNight;
    private double carRatePerDay;
    private List<BillItem> billItems;
    private List<HistoryEvent> history;

    public Guest() {
        this.id = UUID.randomUUID().toString();
        this.name = "";
        this.address = "";
        this.passportNumber = "";
        this.numberOfGuests = 1;
        this.birthdate = LocalDate.now().minusYears(30);
        this.checkInDate = LocalDate.now();
        this.checkOutDate = LocalDate.now().plusDays(1);
        this.roomType = RoomType.DOUBLE;
        this.carHired = false;
        this.carType = null;
        this.comments = "";
        this.flag = GuestFlag.NONE;
        this.createdAt = LocalDate.now();
        this.roomRatePerNight = 0.0;
        this.carRatePerDay = 0.0;
        this.billItems = new ArrayList<>();
        this.history = new ArrayList<>();
    }

    // --- Computed ---
    public int getStayDuration() {
        if (checkInDate == null || checkOutDate == null) return 0;
        return (int) java.time.temporal.ChronoUnit.DAYS.between(checkInDate, checkOutDate);
    }

    public String getInitials() {
        if (name == null || name.isBlank()) return "?";
        String[] parts = name.trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Math.min(2, parts.length); i++) {
            if (!parts[i].isEmpty()) sb.append(Character.toUpperCase(parts[i].charAt(0)));
        }
        return sb.toString();
    }

    public boolean hasSpecialRequest() {
        return comments != null && !comments.isBlank();
    }

    public double getRoomTotal() { return roomRatePerNight * getStayDuration(); }
    public double getCarTotal()  { return carHired ? carRatePerDay * getStayDuration() : 0; }
    public double getExtraCharges() {
        return billItems.stream().mapToDouble(BillItem::getAmount).sum();
    }
    public double getTotalCharged() { return getRoomTotal() + getCarTotal() + getExtraCharges(); }
    public double getTotalPaid() {
        return getRoomTotal() + getCarTotal() +
               billItems.stream().filter(BillItem::isPaid).mapToDouble(BillItem::getAmount).sum();
    }
    public double getOpenBill() { return Math.max(0, getTotalCharged() - getTotalPaid()); }
    public double getProfit()   { return getTotalCharged(); }

    // --- Getters / Setters ---
    public String getId()                          { return id; }
    public void setId(String id)                   { this.id = id; }
    public String getName()                        { return name; }
    public void setName(String name)               { this.name = name; }
    public String getAddress()                     { return address; }
    public void setAddress(String address)         { this.address = address; }
    public String getPassportNumber()              { return passportNumber; }
    public void setPassportNumber(String n)        { this.passportNumber = n; }
    public int getNumberOfGuests()                 { return numberOfGuests; }
    public void setNumberOfGuests(int n)           { this.numberOfGuests = n; }
    public LocalDate getBirthdate()                { return birthdate; }
    public void setBirthdate(LocalDate d)          { this.birthdate = d; }
    public LocalDate getCheckInDate()              { return checkInDate; }
    public void setCheckInDate(LocalDate d)        { this.checkInDate = d; }
    public LocalDate getCheckOutDate()             { return checkOutDate; }
    public void setCheckOutDate(LocalDate d)       { this.checkOutDate = d; }
    public RoomType getRoomType()                  { return roomType; }
    public void setRoomType(RoomType t)            { this.roomType = t; }
    public boolean isCarHired()                    { return carHired; }
    public void setCarHired(boolean b)             { this.carHired = b; }
    public CarType getCarType()                    { return carType; }
    public void setCarType(CarType t)              { this.carType = t; }
    public String getComments()                    { return comments; }
    public void setComments(String c)              { this.comments = c; }
    public GuestFlag getFlag()                     { return flag; }
    public void setFlag(GuestFlag f)               { this.flag = f; }
    public LocalDate getCreatedAt()                { return createdAt; }
    public void setCreatedAt(LocalDate d)          { this.createdAt = d; }
    public double getRoomRatePerNight()            { return roomRatePerNight; }
    public void setRoomRatePerNight(double r)      { this.roomRatePerNight = r; }
    public double getCarRatePerDay()               { return carRatePerDay; }
    public void setCarRatePerDay(double r)         { this.carRatePerDay = r; }
    public List<BillItem> getBillItems()           { return billItems; }
    public void setBillItems(List<BillItem> b)     { this.billItems = b; }
    public List<HistoryEvent> getHistory()         { return history; }
    public void setHistory(List<HistoryEvent> h)   { this.history = h; }
}
