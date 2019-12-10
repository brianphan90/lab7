#!/bin/bash
export LAB7_JDBC_URL=jdbc:mysql://db.labthreesixfive.com/bphan08?useSSL=false
export LAB7_JDBC_USER=bphan08
export LAB7_JDBC_PW=CSC365-F2019_012808506
java -classpath ./mysql-connector-java-8.0.16.jar:. connection
