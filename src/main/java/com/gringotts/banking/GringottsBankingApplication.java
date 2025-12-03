package com.gringotts.banking;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class GringottsBankingApplication {

	public static void main(String[] args) {
		SpringApplication.run(GringottsBankingApplication.class, args);
			System.out.println("Gringotts Bank is Ready");
	}

}
