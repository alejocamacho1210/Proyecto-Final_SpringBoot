# CoWorkHub

API REST y aplicación web responsive para administrar una red de coworking. Incluye gestión de sedes, salas, equipamientos, servicios adicionales, planes, miembros, disponibilidad y reservas. Está construida con Spring Boot, Java 21, Maven y MySQL; la API utiliza autenticación JWT.

## Requisitos

- Java 21
- Maven
- MySQL compatible, instalado localmente o ejecutado con Docker

## Preparar MySQL

La configuración de `src/main/resources/application.properties` conecta con `jdbc:mysql://localhost:3307/coworkhub` y usa el usuario y contraseña `coworkhub`. Se usa el puerto `3307` para evitar conflictos con una instalación local que escuche en `3306`; si MySQL usa otro puerto, ajuste la URL o defina `SPRING_DATASOURCE_URL`.

Ejemplo de base y usuario en MySQL:

```sql
CREATE DATABASE coworkhub CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'coworkhub'@'localhost' IDENTIFIED BY 'cambie-esta-clave';
GRANT ALL PRIVILEGES ON coworkhub.* TO 'coworkhub'@'localhost';
```

Configure la contraseña correspondiente en `spring.datasource.password` o mediante `SPRING_DATASOURCE_PASSWORD` antes de iniciar la aplicación.

### MySQL con Docker

Docker es opcional; también puede usar una instalación local de MySQL. Este comando inicia MySQL y crea la base y el usuario de la aplicación. Sustituya las contraseñas y configure la misma contraseña para la aplicación.

```bash
docker run --name coworkhub-mysql -e MYSQL_ROOT_PASSWORD=cambie-root -e MYSQL_DATABASE=coworkhub -e MYSQL_USER=coworkhub -e MYSQL_PASSWORD=cambie-esta-clave -p 3307:3306 -v coworkhub-datos:/var/lib/mysql -d mysql:8.4
```

Espere a que `docker logs coworkhub-mysql` indique `ready for connections`. Consulte el estado del contenedor:

```bash
docker logs coworkhub-mysql
docker ps
```

Para detenerlo y volverlo a iniciar:

```bash
docker stop coworkhub-mysql
docker start coworkhub-mysql
```

El volumen `coworkhub-datos` conserva la base aunque se elimine el contenedor. Para iniciar otra vez un contenedor detenido, use `docker start coworkhub-mysql`.

## Arrancar la aplicación

Desde la raíz del proyecto:

```bash
mvn spring-boot:run
```

La aplicación escucha por defecto en el puerto `8080`. Para ejecutarla en `8081` (por ejemplo, si `8080` ya está ocupado) y conservar los datos existentes, use en Linux/macOS:

```bash
SPRING_JPA_HIBERNATE_DDL_AUTO=update mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8081
```

En PowerShell:

```powershell
$env:SPRING_JPA_HIBERNATE_DDL_AUTO = "update"
mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8081
```

**Importante sobre los datos:** `spring.jpa.hibernate.ddl-auto` está configurado como `create` en `application.properties`. Ese modo recrea el esquema al arrancar y puede borrar los datos existentes. Use `SPRING_JPA_HIBERNATE_DDL_AUTO=update` en los arranques posteriores si quiere conservarlos. `import.sql` carga los datos de ejemplo cuando Hibernate crea el esquema.

Para detener la aplicación iniciada con Maven, presione `Ctrl+C` en esa misma terminal antes de volver a arrancarla; así evita el error de puerto ocupado. Esto no detiene MySQL.

También puede compilar y ejecutar el JAR:

```bash
mvn clean package
java -jar target/coworkhub-api-1.0.0.jar
```

Ejecute las pruebas automatizadas (usan H2 y no requieren una instancia local de MySQL):

```bash
mvn test
```

Para desarrollo, `COWORKHUB_JWT_SECRET` permite reemplazar el secreto JWT predeterminado. Defina un valor aleatorio de al menos 32 bytes antes de un despliegue; no use el secreto local de desarrollo en producción.

## Credenciales de prueba

Los usuarios de ejemplo cargados desde `import.sql` comparten la contraseña `coworkhub123`:

| Usuario | Rol |
|---|---|
| `admin` | Administrador |
| `recepcionista` | Recepcionista |
| `ana` | Miembro |
| `luis` | Miembro |

Las credenciales son solo para pruebas locales. Cambie los secretos y contraseñas antes de desplegar en un entorno real.

## Swagger UI y OpenAPI

- Swagger UI: `http://localhost:8080/doc/swagger-ui.html`
- Documento OpenAPI servido: `http://localhost:8080/v3/api-docs`
- Estado público de la aplicación: `http://localhost:8080/actuator/health`
- Especificación del repositorio: [`docs/openapi.json`](docs/openapi.json)

Para acceder a endpoints protegidos, use `POST /auth/login` con `username` y `password`; copie `token` de la respuesta en **Authorize** de Swagger UI o Postman. El perfil autenticado está disponible en `GET /auth/perfil`.

Si inició la aplicación en `8081`, reemplace `8080` por `8081` en estas direcciones.

La colección de Postman [`docs/postman/CoWorkHub.postman_collection.json`](docs/postman/CoWorkHub.postman_collection.json) incluye preparación de tokens y los 14 casos de aceptación especificados, con fechas dinámicas y la nota para ejecutar manualmente las solicitudes concurrentes.

## Frontend web

Además de la API, el proyecto incluye una interfaz responsive integrada con **Thymeleaf, HTML, CSS y JavaScript**:

- Inicio de sesión: `http://localhost:8080/login`
- Panel: `http://localhost:8080/dashboard`
- La sesión y las acciones disponibles se determinan por el rol `ADMIN`, `RECEPTIONIST` o `MEMBER`.
- La disponibilidad se filtra por sede, fecha, horario, capacidad y equipamiento; desde un resultado se puede iniciar una reserva y agregar varios servicios.
- ADMIN gestiona los catálogos; ADMIN y RECEPTIONIST registran, editan, suspenden, activan y eliminan miembros, además de gestionar estados y cancelaciones de reservas.
- MEMBER consulta catálogos, crea, consulta, modifica y cancela solo sus reservas, y consulta su consumo por mes. Las reservas se pueden editar mientras están pendientes; la edición conserva sus servicios y aplica las mismas reglas de horario y disponibilidad.
- La navegación y los formularios se adaptan a pantallas móviles.

El frontend guarda el JWT recibido por `/auth/login` en el navegador y lo usa como `Authorization: Bearer` al consumir los endpoints. Las plantillas están en `src/main/resources/templates`; los estilos y scripts están en `src/main/resources/static/css` y `src/main/resources/static/js`.

## API

Los catálogos están bajo `/api`: `/sedes`, `/salas`, `/equipamientos`, `/servicios-adicionales` y `/planes`. Los demás recursos principales son `/api/disponibilidad`, `/api/miembros` y `/api/reservas`. Las rutas de administración requieren rol administrador; las operaciones sobre miembros y reservas aplican permisos por rol y propiedad. La especificación OpenAPI describe métodos, parámetros, cuerpos, respuestas y seguridad.

`PUT /api/reservas/{id}` permite editar la sala, el horario y los asistentes de una reserva `PENDING`; conserva el miembro y los servicios asociados y vuelve a validar disponibilidad, sede, capacidad y horarios. Un miembro solo puede modificar sus propias reservas.

Los errores de la API se responden con los campos `error`, `codigo` y `timestamp`.
