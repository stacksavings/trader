# cryptocurrency-analysis
cryptocurrency-analysis project is a maven project, in the pom.xml file you will find the dependencies used for this project.

This project contain a property file, if you want to change the project configuration, just have edit this file.

# Propierties file
```property
path.directory = C://data_feed//
endpoint.api = https://poloniex.com/public
return.chart.data = ?command=returnChartData&currencyPair=BTC_ETH&start=startbegin&end=startend&period=300
filename = chart_data
filename.extension = csv
```

# How get data from poloniex

You just have to execute PoloniexClientApi.java class

In C9.io environment, just execute this command,

```linux
mvn exec:java -Dexec.mainClass="com.stacksavings.client.api.PoloniexClientApi"
```

```java
	public static void main(String[] args) {

		PoloniexClientApi.getInstance().execute();
		
	}
```

 after it will generate a file csv in 

```property
path.directory = C://data_feed//
```

# Analyzing data

Previusly must exist datas in /home/ubuntu/data_feed/ directory to analysis data.
To display the price in BUY or SELL , just execute the next command

```linux
mvn exec:java -Dexec.mainClass="com.stacksavings.main.BuySell"
```

