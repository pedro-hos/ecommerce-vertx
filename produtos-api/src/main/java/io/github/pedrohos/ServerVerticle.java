package io.github.pedrohos;

import java.math.BigDecimal;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;

public class ServerVerticle extends AbstractVerticle {
	
	private static final String APPLICATION_JSON = "application/json";
	private static final String API_PREFIX = "/api/";
	
	@Override
	public void start(Future<Void> fut) throws Exception {

		Router router = Router.router(vertx);
		router.route(API_PREFIX + "*").handler(BodyHandler.create());
		router.get( API_PREFIX + "produtos").produces(APPLICATION_JSON).handler(this::getAll);

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
		
		JsonArray produtos = new JsonArray();
		
		JsonObject produto01 = new JsonObject();
		produto01.put("name","Produto 01");
		produto01.put("value", new BigDecimal("10").toPlainString());
		produto01.put("quantidade", 10);
		
		JsonObject produto02 = new JsonObject();
		produto02.put("name","Produto 02");
		produto02.put("value", new BigDecimal("20").toPlainString());
		produto02.put("quantidade", 100);
		
		produtos.add(produto01);
		produtos.add(produto02);
		
		response.end(produtos.encodePrettily());
	}

}