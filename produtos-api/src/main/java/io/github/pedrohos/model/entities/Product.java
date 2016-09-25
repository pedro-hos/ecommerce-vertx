package io.github.pedrohos.model.entities;

import java.util.List;
import java.util.stream.Collectors;

import io.vertx.core.json.JsonObject;

public class Product {

	private Long id;
	private String name;
	private Float price;
	private Integer stock;
	private Integer quantity;

	public Product() { }

	public Product(final JsonObject jsonObject) {
		this.id = jsonObject.getLong("id");
		this.name = jsonObject.getString("name");
		this.price = jsonObject.getFloat("price");
		this.stock = jsonObject.getInteger("stock");
	}

	public Product(final Long id, final String name, final Float price, final Integer stock) {
		this.id = id;
		this.name = name;
		this.price = price;
		this.stock = stock;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Float getPrice() {
		return price;
	}

	public void setPrice(Float price) {
		this.price = price;
	}

	public Integer getQuantity() {
		return quantity;
	}

	public void setQuantity(Integer quantity) {
		this.quantity = quantity;
	}

	public Integer getStock() {
		return stock;
	}

	public void setStock(Integer stock) {
		this.stock = stock;
	}
	
	public static List<Product> convertToListProduct(List<JsonObject> rows) {
		return rows.stream().map(Product :: new).collect(Collectors.toList());
	}

	public void sale(Integer quantity) {
		this.stock = this.stock - quantity;
		
	}

}
