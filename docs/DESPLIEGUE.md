# Taller: MySQL, PostgreSQL y Render

Requiere JDK 17, Maven, Git y Docker. En Windows se usa `mvnw.cmd`; en Linux, `./mvnw`.

## Compilacion y MySQL

```powershell
mvn clean install -U
```

Ejecutar `src/main/resources/schema.sql` en MySQL. Configurar las variables `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD` y `JWT_SECRET`. Las claves de `application.properties` leen estas variables para evitar publicar contrasenas.

Para correo local configurar `SMTP_USERNAME` con la cuenta que genero la contraseña de aplicacion de Google, `SMTP_PASSWORD` con esa contraseña, y `SMTP_FROM_ADDRESS` con el mismo correo. No utilizar la contraseña normal de Gmail.

```powershell
.\mvnw.cmd spring-boot:run
```

Abrir http://localhost:8080/swagger-ui/index.html. El registro publico acepta MEMBER y crea cuentas PENDING. Un administrador debe cambiar el estado a ACTIVE antes del login. `POST /api/auth/login` devuelve `accessToken`; colocarlo en Authorize para las consultas protegidas. Solo ADMIN puede crear ADMIN/REVIEWER, actualizar y eliminar; ADMIN y REVIEWER pueden consultar.

Preparar un administrador inicial mediante variables `ADMIN_EMAIL`, `ADMIN_PASSWORD` y la conexion `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`:

```powershell
mvn dependency:copy-dependencies
java --class-path 'target/dependency/*' tools/DatabaseTasks.java bootstrap
```

## Docker

Copiar `.env.example` a `.env` y completar secretos. El puerto MySQL del contenedor es 3307 en la maquina local para no interferir con MySQL instalado.

```powershell
docker compose up --build -d --wait
docker compose logs --tail=30
docker compose down
```

El esquema solo se inicializa cuando el volumen de datos esta vacio. `down` conserva los datos. No eliminar volumenes para reparar una configuracion.

## Migracion a PostgreSQL / Supabase

1. Crear un proyecto Supabase gratuito, con Data API deshabilitada para acceso exclusivo por JDBC.
2. Ejecutar `src/main/resources/schema-postgresql.sql` en SQL Editor.
3. En Connect seleccionar Session pooler, puerto 5432. Copiar el host y el usuario completos; el usuario suele ser `postgres.<referencia>`. Usar `jdbc:postgresql://<host>:5432/postgres?sslmode=require`, `DB_ENGINE=postgresql` y `DB_POOL_SIZE=2`.
4. Detener la API MySQL durante la copia. Definir `SOURCE_DB_URL`, `SOURCE_DB_USERNAME`, `SOURCE_DB_PASSWORD` para el origen, y `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` para el destino.

```powershell
java --class-path 'target/dependency/*' tools/DatabaseTasks.java migrate
java --class-path 'target/dependency/*' tools/DatabaseTasks.java verify
```

La copia conserva los ocho campos, incluyendo UUID, hashes BCrypt y fechas sin conversion de zona horaria. El destino debe estar vacio; cualquier fallo revierte la transaccion. `verify` compara cada campo sin imprimir credenciales.

Para probar PostgreSQL local con Docker:

```powershell
docker compose -f compose.postgresql.yaml up --build -d --wait
```

No ejecutar ambas API en el puerto 8080 simultaneamente. Para comprobar registro, activacion y login definir `ADMIN_EMAIL`, `ADMIN_PASSWORD`, `TEST_EMAIL` y ejecutar `tools/Smoke.ps1`. El usuario de prueba se conserva.

## Render gratuito

Crear un Web Service desde el fork, rama main, runtime Docker, plan Free. `render.yaml` contiene la configuracion reproducible. Health Check Path: `/v3/api-docs`. Mantener la URL del pooler con `sslmode=require` y definir JWT_SECRET aleatorio, nunca el valor de desarrollo.

Render Free restringe los puertos SMTP 25, 465 y 587. El adaptador `GmailApiEmailSenderAdapter` envia por HTTPS usando `EMAIL_PROVIDER=gmail-api`. Habilitar Gmail API en un proyecto Google Cloud y autorizar exclusivamente `https://www.googleapis.com/auth/gmail.send`. Configurar `GMAIL_CLIENT_ID`, `GMAIL_CLIENT_SECRET`, `GMAIL_REFRESH_TOKEN` y `SMTP_FROM_ADDRESS` en Render. El remitente debe coincidir con la cuenta autorizada. No publicar estos valores en GitHub ni en el informe.

Si la aplicacion OAuth permanece en Testing, los refresh tokens para Gmail caducan tras siete dias: renovar la autorizacion antes de una demostracion posterior. Evitar usar el cliente OAuth compartido de Playground; generar un cliente propio para conservar el control de la integracion.

Las instancias Free se suspenden por inactividad y pueden tardar en responder al primer acceso. Abrir `/swagger-ui/index.html`, crear un MEMBER, activarlo con ADMIN, obtener un JWT y comprobar la notificacion recibida, incluida la carpeta Spam.

## Referencias

- https://render.com/docs/free
- https://render.com/docs/blueprint-spec
- https://supabase.com/docs/guides/database/connecting-to-postgres
- https://developers.google.com/gmail/api/guides/sending
- https://developers.google.com/identity/protocols/oauth2
