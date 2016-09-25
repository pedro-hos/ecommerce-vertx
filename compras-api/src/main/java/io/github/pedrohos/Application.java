package io.github.pedrohos;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpHeaders;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;

public class Application extends AbstractVerticle {
	
	private static final String APPLICATION_JSON = "application/json";
	private static final String APPLICATION_JSON_UTF8 = "application/json; charset=utf-8";
	
	@Override
	public void start(Future<Void> fut) throws Exception {

		Router router = Router.router(vertx);
		router.route("/api/*").handler(BodyHandler.create());
		
		router.post("/api/compras")
			  .consumes(APPLICATION_JSON)
			  .produces(APPLICATION_JSON_UTF8)
			  .handler(this::buy);

		vertx.createHttpServer()
			 .requestHandler(router::accept).listen(
				config().getInteger("http.port", 8081), result -> {
					if (result.succeeded()) {
						fut.complete();
					} else {
						fut.fail(result.cause());
					}
				});
	}

	private void buy(RoutingContext routingContext) {
		
		final HttpClient client = vertx.createHttpClient();
		client.post(8080, "localhost", "/api/produtos/comprar", httpClientRequest -> {
			
			httpClientRequest.bodyHandler(bodyHandler -> {
				
				if(httpClientRequest.statusCode() == 200) {
					routingContext.response()
								  .putHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_UTF8)
								  .setStatusCode(200)
								  .end(bodyHandler.toJsonArray().encodePrettily());
				} else {
					
					routingContext.response()
					  			  .putHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_UTF8)
					  			  .setStatusCode(500)
					  			  .end(bodyHandler.toJsonObject().encodePrettily());
					
				}
				
			});
			
		})
		.setChunked(true)
		.putHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON)
		.write(routingContext.getBodyAsString())
		.end();
		
	}

}
