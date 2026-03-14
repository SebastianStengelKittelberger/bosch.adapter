package de.kittelberger.bosch.adapter;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableCaching
public class Application {

	static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

	@Bean
	CacheManager cacheManager() {
		return new ConcurrentMapCacheManager(
			"attrs",
			"categories",
			"categorytypes",
			"lobtypes",
			"mediaobjects",
			"mediaobjecttypes",
			"mfacts",
			"prices",
			"producttypes"
		);
	}

}
