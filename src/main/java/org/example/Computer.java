package org.example;

import java.util.Objects;

public class Computer {

    private int ram;
    private int hdd;
    private int price;

    public Computer(int ram, int hdd, int price) {
        this.ram = ram;
        this.hdd = hdd;
        this.price = price;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Computer computer = (Computer) o;
        return ram == computer.ram && hdd == computer.hdd;
    }

    @Override
    public int hashCode() {
        return Objects.hash(ram, hdd, price);
    }
}
