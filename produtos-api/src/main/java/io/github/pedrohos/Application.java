package io.github.pedrohos;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.flywaydb.core.Flyway;

import io.github.pedrohos.model.entities.Cart;
import io.github.pedrohos.model.entities.Product;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.sql.SQLConnection;
import io.vertx.ext.sql.UpdateResult;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CorsHandler;

public class Application extends AbstractVerticle {
	

	private static final Logger LOGGER = LoggerFactory.getLogger(Application.class);
	
	private JDBCClient jdbc;
	
	private static final String CONTENT_TYPE = "content-type";
	private static final String APPLICATION_JSON = "application/json";
	private static final String APPLICATION_JSON_UTF8 = "application/json; charset=utf-8";
	private static final String API_PREFIX = "/api/produtos";
	
	@Override
	public void start(Future<Void> fut) throws Exception {
		
		LOGGER.info("Starting Products API...");
		
		JsonObject config = new JsonObject().put("url", "jdbc:mysql://localhost/ecommerce")
											.put("driver_class", "com.mysql.jdbc.Driver")
											.put("user", "root")
											.put("password", "root");
		
		jdbc = JDBCClient.createShared(vertx, config);
		
		startMigration(
				(next) -> startBackend(
						(connection) -> startWebApp(
									(http) -> completeStartup(http, fut)
								), fut));

		
	}
	
	 @Override
	  public void stop() throws Exception {
		 LOGGER.info("Stop Products API...");
		 jdbc.close();
	}

	private void startMigration(Handler<AsyncResult<Void>> next) {
		
		LOGGER.info("Migrate data ...");
		
		Flyway flyway = new Flyway();
		flyway.setDataSource("jdbc:mysql://localhost/ecommerce", "root", "root");
		flyway.migrate();
		next.handle(Future.<Void>succeededFuture());
	}
	
	private void startBackend(Handler<AsyncResult<SQLConnection>> next, Future<Void> fut) {
		
		LOGGER.info("Start backend ...");
		
	    jdbc.getConnection(ar -> {
	      if (ar.failed()) {
	        fut.fail(ar.cause());
	      } else {
	        next.handle(Future.succeededFuture(ar.result()));
	      }
	    });
	  }
	
	private void startWebApp(Handler<AsyncResult<HttpServer>> next) {
		
		LOGGER.info("Start routes ...");
		
		Router router = Router.router(vertx);
		
		router.route(API_PREFIX.concat("*")).handler(BodyHandler.create());
		
		router.route().handler(CorsHandler.create("*")
			      .allowedMethod(HttpMethod.GET)
			      .allowedMethod(HttpMethod.POST)
			      .allowedMethod(HttpMethod.OPTIONS)
			      .allowedHeader("X-PINGARUNER")
			      .allowedHeader("Content-Type"));
		
		router.get(API_PREFIX)
			  .produces(APPLICATION_JSON_UTF8)
			  .handler(this::getAll);
		
		router.get(API_PREFIX.concat("/:id"))
			  .produces(APPLICATION_JSON_UTF8)
			  .handler(this::getOne);
		
		router.post(API_PREFIX)
			  .consumes(APPLICATION_JSON)
		      .produces(APPLICATION_JSON_UTF8)
			  .handler(this::create);
		
		router.put(API_PREFIX.concat("/:id"))
			  .consumes(APPLICATION_JSON)
			  .produces(APPLICATION_JSON_UTF8)
			  .handler(this::update);
		
		router.post(API_PREFIX.concat("/comprar"))
			  .consumes(APPLICATION_JSON)
			  .produces(APPLICATION_JSON_UTF8)
			  .handler(this :: buy);
		
		router.delete(API_PREFIX.concat("/:id"))
			  .produces(APPLICATION_JSON_UTF8)
			  .handler(this::delete);
		
		vertx.createHttpServer().requestHandler(router::accept)
								.listen(config().getInteger("http.port", 8080), next :: handle);
		
	}
	
	private void completeStartup(AsyncResult<HttpServer> http, Future<Void> fut) {
		
		LOGGER.info("Complete Startup API ...");
		
	    if (http.succeeded()) {
	      fut.complete();
	    } else {
	      fut.fail(http.cause());
	    }
	}

	private void getAll(RoutingContext routingContext) {
		
		HttpServerResponse response = routingContext.response();
		response.setChunked(true);
		
		jdbc.getConnection(handler -> {
			
			SQLConnection connection = handler.result();
			connection.query("SELECT * FROM PRODUCT", result -> {
				
				Collection<Product> products = result.result().getRows().stream()
																        .map(Product :: new)
																        .collect(Collectors.toList());
				
				if (products.isEmpty()) {
					response.setStatusCode(404).end();
					
				} else {
					
					response.putHeader(CONTENT_TYPE, APPLICATION_JSON_UTF8)
					  	    .end(Json.encodePrettily(products));
					
				}
				
				connection.close();
				
			});
			
		});
		
	}

	private void getOne(RoutingContext routingContext) {
		
		final String id = routingContext.request().getParam("id");
		
		if (Objects.isNull(id)) {
		      routingContext.response().setStatusCode(404).end();
		      
		} else {
			
			jdbc.getConnection(handler -> {
				
				 SQLConnection connection = handler.result();
				 select(id, connection, result -> {
					 
					 if(result.succeeded()) {
						 routingContext.response()
			                		   .setStatusCode(200)
			                		   .putHeader(CONTENT_TYPE, APPLICATION_JSON_UTF8)
			                		   .end(Json.encodePrettily(result.result()));
					 } else {
						 routingContext.response()
						 			   .setStatusCode(404).end();
					 }
					 
					 connection.close();
					 
				 });
				
			});
			
		}
	}
	
	private void create(RoutingContext routingContext) {
		
		jdbc.getConnection(handler -> {
			
			final Product product = Json.decodeValue(routingContext.getBodyAsString(), Product.class);
			SQLConnection connection = handler.result();
			
			insert(product, connection, result -> {
				
				if (result.succeeded()) {
					routingContext.response().setStatusCode(200)
											 .putHeader(CONTENT_TYPE, APPLICATION_JSON_UTF8)
											 .end(Json.encodePrettily(result.result()));
				} else {
					routingContext.response().setStatusCode(500).end();
				}
				
				connection.close();
				
				
			});
			
		});
		
	}

	private void update(RoutingContext routingContext) {
		
		final String id = routingContext.request().getParam("id");
		final JsonObject json = routingContext.getBodyAsJson();
		
		if (Objects.isNull(id) || Objects.isNull(json)) {
			routingContext.response().setStatusCode(400).end();
			
		} else {
			
			jdbc.getConnection(handler -> {
				
				SQLConnection connection = handler.result();
				update(id, json, connection, (product) -> {
					
					if(product.succeeded()) {
						routingContext.response().setStatusCode(201)
												 .putHeader(CONTENT_TYPE, APPLICATION_JSON_UTF8)
												 .end(Json.encodePrettily(product.result()));
						
					} else {
						routingContext.response().setStatusCode(404).end();
						
					}
					
					connection.close();
					
				});
				
			});
			
		}
	}
	
	private void delete(RoutingContext routingContext) {
		
		 String id = routingContext.request().getParam("id");
		 
		    if (Objects.isNull(id)) {
		      routingContext.response().setStatusCode(400).end();
		      
		    } else {
		    	
		      jdbc.getConnection(handler -> {
		    	  
		        SQLConnection connection = handler.result();
		        String sql = "DELETE FROM PRODUCT WHERE ID ='" + id + "'";
		        
				connection.execute(sql, result -> {
		              routingContext.response().setStatusCode(204).end();
		              connection.close();
				});
			});
		}
		
	}
	
	private void buy (RoutingContext routingContext) {
		
		routingContext.response().setChunked(true);
		
		Cart cart = Json.decodeValue(routingContext.getBodyAsString(), Cart.class);
		
		final Map<Long, Product> productsMap = cart.getProducts().stream()
						  										 .collect(Collectors.toMap(Product :: getId, Function.identity()));
		
		jdbc.getConnection(handler -> {

			SQLConnection connection = handler.result();
			
			selectAllByIds(productsMap.keySet().stream().collect(Collectors.toList()), connection, resultHandler -> {
				
				if(resultHandler.succeeded()) {
					
					final List<Product> products = resultHandler.result();
					Boolean hasError = false;
					
					for (Product product : products) {
						
						Integer quantity = productsMap.get(product.getId()).getQuantity();
						
						if (product.getStock().compareTo(quantity) == -1) {
							hasError = true;
							routingContext.response()
										  .setStatusCode(500)
										  .end(new JsonObject().put("message", "Produto " +  product.getName() + " sem estoque!")
															   .encodePrettily());
							
						} else {
							product.sale(quantity);
						}
						
					}
					
					if(!hasError) {
					
						updateQuantity(products, connection, result -> {
							
							if(result.failed()) {
								LOGGER.error(result.cause());
								routingContext.response()
											  .setStatusCode(500)
											  .end(new JsonObject().put("message", "Erro ao comprar produto").encodePrettily());
								
							} else {
								
								routingContext.response().setStatusCode(200)
														 .putHeader(CONTENT_TYPE, APPLICATION_JSON_UTF8)
														 .end(Json.encodePrettily(products));
							}
							
						});	
						
					}
					
					
				} else {
					
					LOGGER.error(resultHandler.cause());
					routingContext.response().setStatusCode(500)
											 .end(new JsonObject().put("message", "Erro ao comprar produto").encodePrettily());
				}
				
				connection.close();
				
			});
			
		});
		
	}
	
	private void updateQuantity(List<Product> products, SQLConnection connection, Handler<AsyncResult<JsonObject>> resultHandler) {
		
		products.forEach(p -> {

			String sql = "UPDATE PRODUCT SET STOCK = ? WHERE ID = ?";

			connection.updateWithParams(sql, new JsonArray().add(p.getStock()).add(p.getId()), updateResult -> {

				if (updateResult.failed()) {
					resultHandler.handle(Future.failedFuture("Cannot update the product"));
					LOGGER.error(updateResult.cause());
					return;
				}

				if (updateResult.result().getUpdated() == 0) {
					resultHandler.handle(Future.failedFuture("Product not found"));
					LOGGER.error(updateResult.cause());
					return;
				}

			});
			
		});
		
		resultHandler.handle(Future.succeededFuture());
		
	}
	
	private void selectAllByIds(List<Long> ids, SQLConnection connection, Handler<AsyncResult<List<Product>>> resultHandler) {
		
		StringBuilder sql = new StringBuilder("SELECT * FROM PRODUCT WHERE ID IN (");
		ids.stream().forEach(id -> sql.append(id).append(","));
		sql.deleteCharAt(sql.length() - 1);
		sql.append(")");
		
		connection.query(sql.toString(), handler -> {

			if (handler.failed()) {
				resultHandler.handle(Future.failedFuture(handler.cause()));

			} else {

				if (handler.result().getNumRows() >= 1) {
					resultHandler.handle(Future.succeededFuture(Product.convertToListProduct(handler.result().getRows())));

				} else {
					resultHandler.handle(Future.failedFuture(handler.cause()));
				}
			}

		});
		
	}
	
	private void insert(Product product, SQLConnection connection, Handler<AsyncResult<Product>> next) {
		
		String sql = "INSERT INTO PRODUCT (NAME, PRICE, STOCK) VALUES (?, ?, ?)";
		
		final JsonArray params = new JsonArray().add(product.getName())
											    .add(product.getPrice())
											    .add(product.getStock());
		
		connection.updateWithParams(sql, params, (resultHandler) -> {
		          
			if (resultHandler.failed()) {
				next.handle(Future.failedFuture(resultHandler.cause()));
				connection.close();
				LOGGER.error(resultHandler.cause());
				return;	
			}
			
			UpdateResult result = resultHandler.result();
			
			Product newProduct = new Product( result.getKeys().getLong(0), 
											  product.getName(), 
											  product.getPrice(), 
											  product.getStock());
			
		    next.handle(Future.succeededFuture(newProduct));
		    
		});
		
	}
	
	public void select(String id, SQLConnection connection, Handler<AsyncResult<Product>> resultHandler) {
		
	    connection.queryWithParams("SELECT * FROM PRODUCT WHERE ID = ?", new JsonArray().add(id), handler -> {
	    	
	      if (handler.failed()) {
	    	  resultHandler.handle(Future.failedFuture("Product not found"));
	        
	      } else {
	    	  
	        if (handler.result().getNumRows() >= 1) {
	          resultHandler.handle(Future.succeededFuture(new Product(handler.result().getRows().get(0))));
	          
	        } else {
	          resultHandler.handle(Future.failedFuture("Product not found"));
	        }
	      }
	      
	    });
	}
	
	private void update(String id, JsonObject jsonObject, SQLConnection connection, Handler<AsyncResult<Product>> resultHandler) {
		
		String sql = "UPDATE PRODUCT SET NAME = ?, PRICE = ?, STOCK = ? WHERE ID = ?";
		
		final JsonArray params = new JsonArray().add(jsonObject.getString("name"))
												.add(jsonObject.getFloat("price"))
												.add(jsonObject.getInteger("stock"))
												.add(id);
		
		connection.updateWithParams(sql, params, updateResult -> {
					
					if (updateResult.failed()) {
						resultHandler.handle(Future.failedFuture("Cannot update the product"));
						LOGGER.error(updateResult.cause());
						return;
					}
					
					if (updateResult.result().getUpdated() == 0) {
						resultHandler.handle(Future.failedFuture("Product not found"));
						LOGGER.error(updateResult.cause());
						return;
					}
					
					Product product = new Product(Long.valueOf(id), jsonObject.getString("name"), jsonObject.getFloat("price"), jsonObject.getInteger("stock"));
					resultHandler.handle(Future.succeededFuture(product));
					
				});
	}

}