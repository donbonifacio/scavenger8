
test:
	mvn test

ci:
	mvn -Dcobertura.report.format=xml \
			-DsourceEncoding=utf8 \
			clean \
			cobertura:cobertura \
			sonar:sonar \
			coveralls:report

uberjar:
	mvn clean assembly:assembly -DdescriptorId=jar-with-dependencies
