# Usa una imagen base oficial de Java 21 (o la versión que estés usando)
FROM openjdk:21-jdk-slim

# Establece el directorio de trabajo dentro del contenedor
WORKDIR /app

# Copia el archivo pom.xml para descargar las dependencias primero
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copia el resto del código fuente de tu aplicación
COPY src ./src

# Compila la aplicación y crea el archivo .jar
# El comando "clean package" asegura una compilación limpia
# "-DskipTests" acelera la construcción al omitir las pruebas
RUN mvn clean package -DskipTests

# Expone el puerto 8080 para que el mundo exterior pueda acceder a la aplicaciónnnnn
EXPOSE 8080

# El comando que se ejecutará cuando el contenedor se inicie
# Asegúrate de que el nombre del .jar coincida con el de tu proyecto
ENTRYPOINT ["java", "-jar", "target/play-0.0.1-SNAPSHOT.jar"]