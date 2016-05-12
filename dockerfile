FROM dgroup/java8-gradle
ADD /build/libs/bsdemo1-0.0.1-SNAPSHOT.jar bsdemo1.jar
CMD ["java", "-jar", "bsdemo1.jar"]