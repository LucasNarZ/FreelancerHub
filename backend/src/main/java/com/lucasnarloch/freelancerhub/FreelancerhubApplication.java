package com.lucasnarloch.freelancerhub;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class FreelancerhubApplication {

	public static void main(String[] args) {
		SpringApplication.run(FreelancerhubApplication.class, args);
	}

}
