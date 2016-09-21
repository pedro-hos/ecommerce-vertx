package io.github.pedrohos;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;

public class ServerVerticle extends AbstractVerticle {
	
	@Override
	public void start(Future<Void> fut) throws Exception {

		Router router = Router.router(vertx);
		router.route("/api/*").handler(BodyHandler.create());
		router.get("/api/produto").produces("application/json").handler(this::teste);

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

	private void teste(RoutingContext routingContext) {
		
		vertx.eventBus().send("sample.data", "hello vert.x", r -> {
			System.out.println("[Main] Receiving reply ' " + r.result().body() + "' in " + Thread.currentThread().getName());
		});
		
		HttpServerResponse response = routingContext.response();
		JsonObject json = new JsonObject();
		json.put("msg","ola 1");
		response.end(json.encodePrettily());
	}

}
