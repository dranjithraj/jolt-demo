package com.bazaarvoice.jolt.helper;

import java.util.List;

public class ItemsList {
	
    private List<Item> items;
    private String modelName;

    public int[] getArrValues() {
        return arrValues;
    }

    public void setArrValues(int[] arrValues) {
        this.arrValues = arrValues;
    }

    int [] arrValues;
    public ItemsList(List<Item> items) {
        this.items = items;
    }

    public List<Item> getItems() {
        return items;
    }

    public void setItems(List<Item> items) {
        this.items = items;
    }

	public String getModelName() {
		return modelName;
	}

	public void setModelName(String modelName) {
		this.modelName = modelName;
	}

}
