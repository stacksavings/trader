package com.stacksavings.client.api.dto;

import java.util.Arrays;
import java.util.List;

public class ChartDataMain {
	
	private List<ChartData> datas;

	public List<ChartData> getDatas() {
		return datas;
	}

	public void setDatas(List<ChartData> datas) {
		this.datas = datas;
	}
	
	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		
		sb.append("datas="+Arrays.toString(getDatas().toArray())+"\n");
		
		return sb.toString();
	}
}
