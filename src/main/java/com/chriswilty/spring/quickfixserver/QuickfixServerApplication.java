package com.chriswilty.spring.quickfixserver;

import com.chriswilty.spring.quickfixserver.fix.FixConnectionManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class QuickfixServerApplication implements CommandLineRunner {

	@Autowired
	private FixConnectionManager fixConnectionManager;

	public static void main(String[] args) {
		SpringApplication.run(QuickfixServerApplication.class, args);
	}

	@Override
	public void run(String... args) {
		fixConnectionManager.run();
	}
}
