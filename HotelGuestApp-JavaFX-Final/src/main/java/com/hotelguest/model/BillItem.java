package com.hotelguest.model;

import java.time.LocalDate;
import java.util.UUID;

public class BillItem {
    private String id;
    private String description;
    private double amount;
    private boolean paid;
    private LocalDate date;

    public BillItem() {
        this.id = UUID.randomUUID().toString();
        this.date = LocalDate.now();
    }

    public BillItem(String description, double amount, boolean paid) {
        this();
        this.description = description;
        this.amount = amount;
        this.paid = paid;
    }

    public String getId()              { return id; }
    public void setId(String id)       { this.id = id; }
    public String getDescription()     { return description; }
    public void setDescription(String d){ this.description = d; }
    public double getAmount()          { return amount; }
    public void setAmount(double a)    { this.amount = a; }
    public boolean isPaid()            { return paid; }
    public void setPaid(boolean p)     { this.paid = p; }
    public LocalDate getDate()         { return date; }
    public void setDate(LocalDate d)   { this.date = d; }
}
