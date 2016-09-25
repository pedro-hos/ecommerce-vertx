package io.github.pedrohos.model.entities;

import java.util.Collection;

public class Cart {

	private Collection<Product> products;

	public Collection<Product> getProducts() {
		return products;
	}

	public void setProducts(Collection<Product> products) {
		this.products = products;
	}

}
