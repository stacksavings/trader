package com.stacksavings;

import org.junit.Test;

import com.stacksavings.client.api.BatchMain;

public class BatchMainTest {

	
	  @Test
	  public void test() {
		  BatchMain.getInstance().execute();
	  }
}
