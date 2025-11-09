package com.example.lab6_20212624.model;

import java.util.Date;

public class Record {
    private String id; // document id
    private String ownerId;
    private String recordId; // 5-digit code
    private String vehicleDocId; // reference to vehicle document id
    private String vehicleDisplay; // e.g., "Mi Auto - ABC123"
    private Date date;
    private double liters;
    private int mileage;
    private double totalPrice;
    private String fuelType; // Gasolina, GLP, GNV

    public Record() {}

    public Record(String ownerId, String recordId, String vehicleDocId, String vehicleDisplay, Date date, double liters, int mileage, double totalPrice, String fuelType) {
        this.ownerId = ownerId;
        this.recordId = recordId;
        this.vehicleDocId = vehicleDocId;
        this.vehicleDisplay = vehicleDisplay;
        this.date = date;
        this.liters = liters;
        this.mileage = mileage;
        this.totalPrice = totalPrice;
        this.fuelType = fuelType;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }
    public String getRecordId() { return recordId; }
    public void setRecordId(String recordId) { this.recordId = recordId; }
    public String getVehicleDocId() { return vehicleDocId; }
    public void setVehicleDocId(String vehicleDocId) { this.vehicleDocId = vehicleDocId; }
    public String getVehicleDisplay() { return vehicleDisplay; }
    public void setVehicleDisplay(String vehicleDisplay) { this.vehicleDisplay = vehicleDisplay; }
    public Date getDate() { return date; }
    public void setDate(Date date) { this.date = date; }
    public double getLiters() { return liters; }
    public void setLiters(double liters) { this.liters = liters; }
    public int getMileage() { return mileage; }
    public void setMileage(int mileage) { this.mileage = mileage; }
    public double getTotalPrice() { return totalPrice; }
    public void setTotalPrice(double totalPrice) { this.totalPrice = totalPrice; }
    public String getFuelType() { return fuelType; }
    public void setFuelType(String fuelType) { this.fuelType = fuelType; }
}
