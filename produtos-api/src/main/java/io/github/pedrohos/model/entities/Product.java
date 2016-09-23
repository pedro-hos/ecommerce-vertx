package io.github.pedrohos.model.entities;

import io.vertx.core.json.JsonObject;

public class Product {

	private Long id;
	private String name;
	private Float price;
	private Integer amount;

	public Product() {}
	
	public Product(JsonObject jsonObject) {
		this.id = jsonObject.getLong("ID");
		this.name = jsonObject.getString("NAME");
		this.price = jsonObject.getFloat("PRICE");
		this.amount = jsonObject.getInteger("AMOUNT");
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

	public Integer getAmount() {
		return amount;
	}

	public void setAmount(Integer amount) {
		this.amount = amount;
	}

}
