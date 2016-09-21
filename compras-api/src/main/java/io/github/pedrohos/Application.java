package io.github.pedrohos;

import com.hazelcast.config.Config;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.spi.cluster.ClusterManager;
import io.vertx.spi.cluster.hazelcast.HazelcastClusterManager;

public class Application extends AbstractVerticle {

	@Override
	public void start() throws Exception {

		Config hazelcastConfig = new Config();
		//hazelcastConfig.getNetworkConfig().getJoin().getTcpIpConfig().addMember("127.0.0.1").setEnabled(true);
		//hazelcastConfig.getNetworkConfig().getJoin().getMulticastConfig().setEnabled(false);

		ClusterManager mgr = new HazelcastClusterManager(hazelcastConfig);
		VertxOptions options = new VertxOptions().setClusterManager(mgr);
		
		Vertx.clusteredVertx(options, res -> {
			
			if (res.succeeded()) {
				Vertx vertx = res.result();
				vertx.deployVerticle(new ServerVerticle());
			} else {
				
			}
			
		});
	}

}
