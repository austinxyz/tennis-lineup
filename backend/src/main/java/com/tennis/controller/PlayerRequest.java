package com.tennis.controller;

public class PlayerRequest {
    private String name;
    private String gender;
    private Double utr;
    private Double verifiedDoublesUtr;
    private Boolean verified;
    private String profileUrl;

    // Getters and setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
    public Double getUtr() { return utr; }
    public void setUtr(Double utr) { this.utr = utr; }
    public Double getVerifiedDoublesUtr() { return verifiedDoublesUtr; }
    public void setVerifiedDoublesUtr(Double verifiedDoublesUtr) { this.verifiedDoublesUtr = verifiedDoublesUtr; }
    public Boolean getVerified() { return verified; }
    public void setVerified(Boolean verified) { this.verified = verified; }
    public String getProfileUrl() { return profileUrl; }
    public void setProfileUrl(String profileUrl) { this.profileUrl = profileUrl; }
    private String notes;
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}