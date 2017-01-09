
#Building the project

This page explain how to build the project.

##Build the project for a command line use


This build include all `flink` and `hadoop` dependencies in a single jar, that can be used without cluster.


Extract the project :

	git clone http://github.com/esrifrance/osm-flink-tools

	./gradlew allJars


the result is located in the `build\libs` folder

Running the commandline from the root directory :


	java -jar osm-flink-tools-[version]-all.jar rhone-alpes-latest.osm.pbf .\



##Build the project for a flink cluster usage


	./gradlew shadowJar


the output jar can be used from a flink client.