package com.foo.batch.anotherpoc.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import static springfox.documentation.builders.PathSelectors.regex;

// Go to: http://localhost:9097/swagger-ui.html
@Configuration
@EnableSwagger2
public class SwaggerConfiguration {
    @Bean
    public Docket productApi() {
        return new Docket(DocumentationType.SWAGGER_2)
                .select()
                .apis(RequestHandlerSelectors
                        .basePackage("com.foo.batch.anotherpoc.controller"))
                .paths(regex("/data-sync.*"))
                .build()
                .apiInfo(getApiInfo());

    }

    private ApiInfo getApiInfo() {
        Contact contact = new Contact("Vinodhini", "somedummyurl.com", "someuser@gmail.com");
        return new ApiInfoBuilder().description("Data Sync Job Controller").title("Data Sync").contact(contact).build();
    }
}
