
test:
	mvn -Dorg.slf4j.simpleLogger.defaultLogLevel=trace -e clean test

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

rupeal: uberjar
	java -Dorg.slf4j.simpleLogger.defaultLogLevel=debug -jar target/scavenger8-0.1.0-jar-with-dependencies.jar -file rupeal.txt

clanhr: uberjar
	java -Dorg.slf4j.simpleLogger.defaultLogLevel=trace -jar target/scavenger8-0.1.0-jar-with-dependencies.jar -file clan.txt

alexa1M: uberjar
	java -Dorg.slf4j.simpleLogger.defaultLogLevel=info -jar target/scavenger8-0.1.0-jar-with-dependencies.jar -file alexa1M.txt
