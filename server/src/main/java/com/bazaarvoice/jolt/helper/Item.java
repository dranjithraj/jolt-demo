package com.bazaarvoice.jolt.helper;

public class Item {
    public Item(String name, int value) {
        this.name = name;
        this.value = value;
    }

    String name;
    int value;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }
}
