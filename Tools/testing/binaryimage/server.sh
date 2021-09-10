cd dockermc
java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5001 -jar DMCServerManager.jar
