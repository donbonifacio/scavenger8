
test:
	mvn test

ci:
	mvn -Dcobertura.report.format=xml \
			-DsourceEncoding=utf8 \
			clean \
			cobertura:cobertura \
			coveralls:report \
			org.jacoco:jacoco-maven-plugin:prepare-agent \
			sonar:sonar

uberjar:
	mvn clean assembly:assembly -DdescriptorId=jar-with-dependencies
