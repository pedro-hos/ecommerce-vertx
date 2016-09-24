package io.github.pedrohos;

import java.util.Collection;
import java.util.Objects;
import java.util.stream.Collectors;

import org.flywaydb.core.Flyway;

import io.github.pedrohos.model.entities.Product;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServer;
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

public class Application extends AbstractVerticle {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(Application.class);
	
	private JDBCClient jdbc;
	
	private static final String CONTENT_TYPE = "content-type";
	private static final String APPLICATION_JSON = "application/json; charset=utf-8";
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
		
		router.get(API_PREFIX)
			  .produces(APPLICATION_JSON)
			  .handler(this::getAll);
		
		router.get(API_PREFIX.concat("/:id"))
			  .produces(APPLICATION_JSON)
			  .handler(this::getOne);
		
		router.post(API_PREFIX).handler(this::create);
		
		router.put(API_PREFIX.concat("/:id"))
			  .consumes("application/json")
			  .produces(APPLICATION_JSON)
			  .handler(this::update);
		
		router.delete(API_PREFIX.concat("/:id"))
			  .produces(APPLICATION_JSON)
			  .handler(this::delete);
		
		vertx.createHttpServer().requestHandler(router::accept)
								.listen(config().getInteger("http.port", 8080), 
										next :: handle);
		
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
		
		jdbc.getConnection(handler -> {
			
			SQLConnection connection = handler.result();
			connection.query("SELECT * FROM PRODUCT", result -> {
				
				Collection<Product> products = result.result().getRows().stream()
																        .map(Product :: new)
																        .collect(Collectors.toList());
				
				if (products.isEmpty()) {
					routingContext.response().setStatusCode(404).end();
					
				} else {
					
					routingContext.response()
					  			  .putHeader(CONTENT_TYPE, APPLICATION_JSON)
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
			                		   .putHeader(CONTENT_TYPE, APPLICATION_JSON)
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
											 .putHeader(CONTENT_TYPE, APPLICATION_JSON)
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
												 .putHeader(CONTENT_TYPE, APPLICATION_JSON)
												 .end(Json.encodePrettily(product.result()));;
						
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
	
	private void insert(Product product, SQLConnection connection, Handler<AsyncResult<Product>> next) {
		
		String sql = "INSERT INTO PRODUCT (NAME, PRICE, AMOUNT) VALUES (?, ?, ?)";
		
		final JsonArray params = new JsonArray().add(product.getName())
											    .add(product.getPrice())
											    .add(product.getAmount());
		
		connection.updateWithParams(sql, params, (resultHandler) -> {
		          
			if (resultHandler.failed()) {
				next.handle(Future.failedFuture(resultHandler.cause()));
				connection.close();
				LOGGER.error(resultHandler.cause());
				return;	
			}
			
			UpdateResult result = resultHandler.result();
			Product newProduct = new Product(result.getKeys().getLong(0), product.getName(), product.getPrice(), product.getAmount());
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
	
	private void update(String id, JsonObject content, SQLConnection connection, Handler<AsyncResult<Product>> resultHandler) {
		
		String sql = "UPDATE PRODUCT SET NAME = ?, PRICE = ?, AMOUNT = ? WHERE ID = ?";
		
		final JsonArray params = new JsonArray().add(content.getString("name"))
												.add(content.getFloat("price"))
												.add(content.getInteger("amount"))
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
					
					Product product = new Product(Long.valueOf(id), content.getString("name"), content.getFloat("price"), content.getInteger("amount"));
					resultHandler.handle(Future.succeededFuture(product));
					
				});
	}
}