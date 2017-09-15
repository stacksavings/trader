# cryptocurrency-analysis

cryptocurrency-analysis project analysis datas extract from poloniex api, the target is check when there is buy signal.

cryptocurrency-analysis project is a maven project, in the pom.xml file you will find the dependencies used for this project.

This project contain a property file, if you want to change the project configuration, just have edit this file.

# Dependencies

This project uses the next dependencies
- [Ta4j](https://github.com/mdeverdelhan/ta4j) , it is an open source Java library for technical analysis.
- [HttpClient](http://hc.apache.org/httpclient-3.x/), HttpClient is a HTTP/1.1 compliant HTTP agent implementation based on HttpCore.
- [Junit](http://junit.org/junit5/) The JUnit Platform serves as a foundation for launching testing frameworks on the JVM.
- [opencsv](http://opencsv.sourceforge.net/) opencsv is an easy-to-use CSV (comma-separated values) parser library for Java. 

# Filter Maven

we have 3 environment in this project. 

- local, this enviroment is by default in fact you can see in pom.xml file it's setted 
```xml
	<properties>
		<env>local</env>
	</properties>
```
- c9, this enviroment is c9.io

- server this enviroment is official.


# Propierties file
```property
path.directory = ${path.directory}
endpoint.api = ${endpoint.api}
return.chart.data = ${return.chart.data}
return.ticker = ${return.ticker}
filename = ${filename}
filename.extension = ${filename.extension}
```

# How start up the project

Execute this command( this is for server environment ), look at -Denv=server

```linux
mvn clean install -DskipTests -Denv=server
```


Execute this command to analysis


```linux
mvn exec:java -Dexec.mainClass="com.stacksavings.client.api.BatchMain"
```

