package io.github.pedrohos.domain.service;

import io.vertx.core.AbstractVerticle;

public class ProdutoService extends AbstractVerticle {

	@Override
	public void start() throws Exception {
		
		vertx.eventBus().consumer("teste", msg -> {
			System.out.println(msg);
			msg.reply("ok");
		});
		
	}
}
