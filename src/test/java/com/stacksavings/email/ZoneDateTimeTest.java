package com.stacksavings.email;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import org.junit.Test;

public class ZoneDateTimeTest {

	private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
	
	@Test
	public void test()
	{
		ZonedDateTime date = LocalDate.parse("2017-08-26 19:50:00", DATE_FORMAT).atStartOfDay(ZoneOffset.UTC);
		
		System.out.println(date.toString());
	}
}
