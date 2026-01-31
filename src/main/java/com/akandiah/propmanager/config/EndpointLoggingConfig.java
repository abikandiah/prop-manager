package com.akandiah.propmanager.config;

import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
public class EndpointLoggingConfig {

	@Bean
	ApplicationRunner logMappedEndpoints(
			@Qualifier("requestMappingHandlerMapping") RequestMappingHandlerMapping handlerMapping) {
		return (ApplicationArguments args) -> {
			if (!log.isInfoEnabled())
				return;

			log.info("--- REST endpoints ---");
			handlerMapping.getHandlerMethods().forEach((mapping, handlerMethod) -> {
				String httpMethods = mapping.getMethodsCondition().getMethods().stream()
						.map(Enum::name)
						.collect(Collectors.joining(", "));
				if (httpMethods.isEmpty())
					httpMethods = "ALL";
				final String methods = httpMethods;
				mapping.getPathPatternsCondition().getPatterns().stream()
						.map(p -> p.getPatternString())
						.filter(path -> !"/error".equals(path))
						.sorted()
						.forEach(path -> log.info("  {} {}", methods, path));
			});
			log.info("----------------------");
		};
	}
}
