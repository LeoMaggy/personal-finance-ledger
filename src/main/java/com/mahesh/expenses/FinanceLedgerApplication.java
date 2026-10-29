package com.mahesh.expenses;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.TimeZone;

@SpringBootApplication
public class FinanceLedgerApplication {

	public static void main(String[] args) {

		// Run in UTC everywhere: the same on a Windows laptop and on AWS,
		// and avoids old zone names like Asia/Calcutta that PostgreSQL rejects
		TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
		SpringApplication.run(FinanceLedgerApplication.class, args);
	}

}
