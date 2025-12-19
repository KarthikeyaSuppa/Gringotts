package com.gringotts.banking.user;

import java.time.LocalDate;

public class UserDTO {
    private  String username;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private String email;
    private String address;
    private LocalDate dateOfBirth;
    private String profileImageUrl; // Added this so we can fetch the image too

    // Constructor matching your Controller call
    public UserDTO(String username,String firstName, String lastName, String phoneNumber, String email, String address, LocalDate dateOfBirth, String profileImageUrl) {
        this.username = username;
        this.firstName = firstName;
        this.lastName = lastName;
        this.phoneNumber = phoneNumber;
        this.email = email;
        this.address = address;
        this.dateOfBirth = dateOfBirth;
        this.profileImageUrl = profileImageUrl;
    }

    // Getters and Setters are required for Jackson to convert this to JSON
    public String getUsername() {
        return username;
    }
    public void setUsername(String username) {
        this.username = username;
    }
    public String getFirstName()
    { return firstName; }

    public void setFirstName(String firstName)
    { this.firstName = firstName; }

    public String getLastName()
    { return lastName; }
    public void setLastName(String lastName)
    { this.lastName = lastName; }

    public String getPhoneNumber()
    { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber)
    { this.phoneNumber = phoneNumber; }

    public String getEmail()
    { return email; }
    public void setEmail(String email)
    { this.email = email; }

    public String getAddress()
    { return address; }
    public void setAddress(String address)
    { this.address = address; }

    public LocalDate getDateOfBirth()
    { return dateOfBirth; }
    public void setDateOfBirth(LocalDate dateOfBirth)
    { this.dateOfBirth = dateOfBirth; }

    public String getProfileImageUrl()
    { return profileImageUrl; }
    public void setProfileImageUrl(String profileImageUrl)
    { this.profileImageUrl = profileImageUrl; }
}