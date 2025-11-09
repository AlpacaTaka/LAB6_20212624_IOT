package com.example.lab6_20212624.model;

import java.util.Date;

public class Vehicle {
    private String id; // id del documento en firestore
    private String ownerId;
    private String vehicleId; // nickname
    private String licensePlate;
    private String brandModel;
    private int year;
    private Date technicalReviewDate; // guardado como Date para Firestore

    public Vehicle() {
        // constructor vacio requerido para firestore
    }

    public Vehicle(String ownerId, String vehicleId, String licensePlate, String brandModel, int year, Date technicalReviewDate) {
        this.ownerId = ownerId;
        this.vehicleId = vehicleId;
        this.licensePlate = licensePlate;
        this.brandModel = brandModel;
        this.year = year;
        this.technicalReviewDate = technicalReviewDate;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }
    public String getVehicleId() { return vehicleId; }
    public void setVehicleId(String vehicleId) { this.vehicleId = vehicleId; }
    public String getLicensePlate() { return licensePlate; }
    public void setLicensePlate(String licensePlate) { this.licensePlate = licensePlate; }
    public String getBrandModel() { return brandModel; }
    public void setBrandModel(String brandModel) { this.brandModel = brandModel; }
    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }
    public Date getTechnicalReviewDate() { return technicalReviewDate; }
    public void setTechnicalReviewDate(Date technicalReviewDate) { this.technicalReviewDate = technicalReviewDate; }
}
