FROM public.ecr.aws/docker/library/maven:3-eclipse-temurin-11-alpine AS build

COPY ./pom.xml ./

RUN mvn initialize \
    ; mvn dependency:tree \
    ;

COPY  ./src ./

RUN mvn package \
    ; cp ./target/spring-on-eks-*.jar /spring-on-eks.jar


FROM public.ecr.aws/docker/library/eclipse-temurin:11-jre-alpine AS runtime

COPY --from=build /spring-on-eks.jar /spring-on-eks.jar

ENTRYPOINT []
CMD ["java", "-jar", "/spring-on-eks.jar"]
