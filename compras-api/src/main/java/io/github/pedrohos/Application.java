package io.github.pedrohos;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.spi.cluster.ClusterManager;
import io.vertx.spi.cluster.hazelcast.HazelcastClusterManager;

public class Application extends AbstractVerticle {
	
	private static final Logger logger = LoggerFactory.getLogger(Application.class);

	@Override
	public void start() throws Exception {

		ClusterManager mgr = new HazelcastClusterManager();
		VertxOptions options = new VertxOptions().setClusterManager(mgr);
		
		Vertx.clusteredVertx(options, res -> {
			
			if (res.succeeded()) {
				Vertx vertx = res.result();
				vertx.deployVerticle(new ServerVerticle());
			} else {
				logger.error("Fail");
				
			}
			
		});
	}

}
