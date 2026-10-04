package com.hotelguest.model;

import java.time.LocalDateTime;
import java.util.UUID;

public class BillItem {
    private String id;
    private String description;
    private double amount;

    // Gson serialises this as "paid".
    // Swift Codable serialises the same field as "isPaid".
    // The GuestStore deserializer handles both — see buildGson().
    private boolean paid;

    // Swift also writes "isPaid" — we keep this field so Gson can
    // read it directly when present; the custom deserializer merges both.
    private Boolean isPaid;

    private LocalDateTime date;

    public BillItem() {
        this.id = UUID.randomUUID().toString();
        this.date = LocalDateTime.now();
    }

    public BillItem(String description, double amount, boolean paid) {
        this();
        this.description = description;
        this.amount = amount;
        this.paid = paid;
    }

    /**
     * Returns true if either "paid" (Java) or "isPaid" (Swift) was true.
     * Called after Gson populates the object.
     */
    public boolean isPaid() {
        return paid || Boolean.TRUE.equals(isPaid);
    }

    public void setPaid(boolean p) {
        this.paid = p;
        this.isPaid = p;   // keep both in sync so export works either way
    }

    public String getId()               { return id; }
    public void setId(String id)        { this.id = id; }
    public String getDescription()      { return description; }
    public void setDescription(String d){ this.description = d; }
    public double getAmount()           { return amount; }
    public void setAmount(double a)     { this.amount = a; }
    public LocalDateTime getDate()          { return date; }
    public void setDate(LocalDateTime d)    { this.date = d; }
}
