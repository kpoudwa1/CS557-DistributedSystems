LIB_PATH=/home/vchaska1/protobuf/protobuf-java-3.5.1.jar
all: clean
	mkdir bin
	javac -classpath $(LIB_PATH) -d bin/ Bank.java BranchSender.java Branch.java Snapshot.java BranchReceiver.java BranchInitializer.java Controller.java

clean: 
	rm -rf bin/
