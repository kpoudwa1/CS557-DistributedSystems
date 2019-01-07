all: Server.java
	javac Server.java
	jar cfm server.jar MANIFEST.MF Server.class

run: all
	java -jar server.jar

clean: 
	rm -rf Server.class server.jar
