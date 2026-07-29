package com.mysterytrip.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "places")
public class Place {

    public enum PlaceType { LODGING, RESTAURANT, ATTRACTION }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PlaceType type;

    @Column(nullable = false)
    private String address;

    @Column(nullable = false)
    private double latitude;

    @Column(nullable = false)
    private double longitude;

    /**
     * LODGING: 1박 요금(원), RESTAURANT: 1인 평균 식사 가격(원), ATTRACTION: 1인 입장료(원)
     */
    @Column(nullable = false)
    private int price;

    @Column(nullable = false)
    private double rating;

    private String description;

    protected Place() {}

    public Place(String name, PlaceType type, String address,
                 double latitude, double longitude, int price, double rating, String description) {
        this.name = name;
        this.type = type;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
        this.price = price;
        this.rating = rating;
        this.description = description;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public PlaceType getType() { return type; }
    public String getAddress() { return address; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public int getPrice() { return price; }
    public double getRating() { return rating; }
    public String getDescription() { return description; }
}
