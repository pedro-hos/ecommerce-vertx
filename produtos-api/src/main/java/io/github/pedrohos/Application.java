package io.github.pedrohos;

import java.util.Arrays;

import org.flywaydb.core.Flyway;

import io.github.pedrohos.model.entities.Product;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.sql.SQLConnection;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;

public class Application extends AbstractVerticle {
	
	private JDBCClient jdbc;
	
	private static final String CONTENT_TYPE = "content-type";
	private static final String APPLICATION_JSON = "application/json; charset=utf-8";
	private static final String API_PREFIX = "/api/produtos";
	
	@Override
	public void start(Future<Void> fut) throws Exception {
		
		Flyway flyway = new Flyway();
		flyway.setDataSource("jdbc:hsqldb:file:db/ecommerce", null, null);
		flyway.migrate();
		
		JsonObject config = new JsonObject().put("url", "jdbc:hsqldb:file:db/ecommerce")
											.put("driver_class", "org.hsqldb.jdbcDriver");
		
		jdbc = JDBCClient.createShared(vertx, config);

		Router router = Router.router(vertx);
		router.route(API_PREFIX.concat("*")).handler(BodyHandler.create());
		
		router.get(API_PREFIX)
			  .produces(APPLICATION_JSON)
			  .handler(this::getAll);
		
		router.get(API_PREFIX.concat("/:id"))
			  .produces(APPLICATION_JSON)
			  .handler(this::getOne);
		
		router.post(API_PREFIX)
			  .produces(APPLICATION_JSON)
			  .handler(this::create);
		
		router.put(API_PREFIX.concat("/:id"))
			  .produces(APPLICATION_JSON)
			  .handler(this::update);
		
		router.delete(API_PREFIX.concat("/:id"))
			  .produces(APPLICATION_JSON)
			  .handler(this::delete);
		
		vertx.createHttpServer()
			 .requestHandler(router::accept).listen(
				config().getInteger("http.port", 8080), result -> {
					
					if (result.succeeded()) {
						fut.complete();
					} else {
						fut.fail(result.cause());
					}
					
				});
	}

	private void getAll(RoutingContext routingContext) {
		
		HttpServerResponse response = routingContext.response();
		
		Product prod01 = new Product();
		prod01.setId(1L);
		prod01.setName("Name");
		prod01.setPrice(13.0f);
		prod01.setAmount(1);
		
		response.putHeader(CONTENT_TYPE, APPLICATION_JSON)
				.end(Json.encodePrettily(Arrays.asList(prod01)));
	}

	private void getOne(RoutingContext routingContext) {
		
		final String id = routingContext.request().getParam("id");
		
		if (id == null) {
		      routingContext.response().setStatusCode(400).end();
		      
		} else {
			
			jdbc.getConnection(ar -> {
				
				 SQLConnection connection = ar.result();
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
	
	private void create(RoutingContext routingContext) {}
	
	private void update(RoutingContext routingContext) {}
	
	private void delete(RoutingContext routingContext) {}
	
	public void select(String id, SQLConnection connection, Handler<AsyncResult<Product>> resultHandler) {
		
	    connection.queryWithParams("SELECT * FROM PRODUCT WHERE ID=?", new JsonArray().add(id), ar -> {
	      if (ar.failed()) {
	    	  resultHandler.handle(Future.failedFuture("Product not found"));
	        
	      } else {
	    	  
	        if (ar.result().getNumRows() >= 1) {
	          resultHandler.handle(Future.succeededFuture(new Product(ar.result().getRows().get(0))));
	          
	        } else {
	          resultHandler.handle(Future.failedFuture("Product not found"));
	        }
	      }
	    });
	}
	
	
}