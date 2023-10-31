package com.github.tylerjpohlman.database.register.helper_classes;

public class Item {
    private long upc;
    private String name;
    private double price;
    private double discount;

    public Item(long upc, String name, double price, double discount) {
        this.upc = upc;
        this.name = name;
        this.price = price;
        this.discount = discount;
    }


    public long getUpc() {
        return upc;
    }
    public String getName() {
        return name;
    }
    public double getPrice() {
        return price;
    }
    public double getDiscount() {
        return discount;
    }

    @Override
    public String toString() {
        return name + '\n'
                + "Price: $" + price;
    }
}
