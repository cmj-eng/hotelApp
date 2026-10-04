package com.hotelguest.model;

import java.time.LocalDateTime;
import java.util.UUID;

public class HistoryEvent {
    public enum EventType { CREATED, UPDATED, DELETED, PAYMENT, NOTE, CHECKED_IN, CHECKED_OUT }

    private String id;
    private LocalDateTime date;
    private String description;
    private EventType type;

    public HistoryEvent() {
        this.id = UUID.randomUUID().toString();
        this.date = LocalDateTime.now();
    }

    public HistoryEvent(String description, EventType type) {
        this();
        this.description = description;
        this.type = type;
    }

    public String getId()              { return id; }
    public void setId(String id)       { this.id = id; }
    public LocalDateTime getDate()     { return date; }
    public void setDate(LocalDateTime d){ this.date = d; }
    public String getDescription()     { return description; }
    public void setDescription(String d){ this.description = d; }
    public EventType getType()         { return type; }
    public void setType(EventType t)   { this.type = t; }
}
