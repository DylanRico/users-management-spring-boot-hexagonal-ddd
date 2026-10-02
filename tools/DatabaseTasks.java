import at.favre.lib.crypto.bcrypt.BCrypt;
import java.sql.*;

/** Utilidades manuales de preparacion y migracion; las credenciales vienen del entorno. */
class DatabaseTasks {
  static String required(String name) {
    String value = System.getenv(name);
    if (value == null || value.isBlank()) throw new IllegalArgumentException("Falta " + name);
    return value;
  }

  static Connection connect(String prefix) throws SQLException {
    return DriverManager.getConnection(required(prefix + "URL"), required(prefix + "USERNAME"), required(prefix + "PASSWORD"));
  }

  public static void main(String[] args) throws Exception {
    if (args.length != 1) throw new IllegalArgumentException("Uso: DatabaseTasks.java bootstrap|migrate");
    switch (args[0]) {
      case "bootstrap" -> {
        try (Connection db = connect("DB_")) {
          String email = required("ADMIN_EMAIL").trim().toLowerCase(java.util.Locale.ROOT);
          try (PreparedStatement query = db.prepareStatement("SELECT id FROM users WHERE email = ?")) {
            query.setString(1, email);
            if (query.executeQuery().next()) throw new IllegalStateException("El administrador ya existe; no se sobrescribio");
          }
          String hash = BCrypt.withDefaults().hashToString(12, required("ADMIN_PASSWORD").toCharArray());
          try (PreparedStatement insert = db.prepareStatement("INSERT INTO users (id,name,email,password,role,status) VALUES (?,?,?,?,'ADMIN','ACTIVE')")) {
            insert.setString(1, java.util.UUID.randomUUID().toString());
            insert.setString(2, "Administrador del taller");
            insert.setString(3, email);
            insert.setString(4, hash);
            insert.executeUpdate();
          }
          System.out.println("Administrador ACTIVE creado con hash BCrypt; contrasena omitida.");
        }
      }
      case "migrate" -> migrate();
      default -> throw new IllegalArgumentException("Operacion desconocida");
    }
  }

  static void migrate() throws Exception {
    String columns = "id,name,email,password,role,status,created_at,updated_at";
    try (Connection source = connect("SOURCE_DB_"); Connection target = connect("DB_")) {
      target.setAutoCommit(false);
      try {
        try (ResultSet existing = target.createStatement().executeQuery("SELECT COUNT(*) FROM users")) {
          existing.next();
          if (existing.getLong(1) != 0) throw new IllegalStateException("Destino no vacio: migracion cancelada");
        }
        int count = 0;
        try (Statement query = source.createStatement(); ResultSet users = query.executeQuery("SELECT " + columns + " FROM users ORDER BY id");
             PreparedStatement insert = target.prepareStatement("INSERT INTO users (" + columns + ") VALUES (?,?,?,?,?,?,?,?)")) {
          while (users.next()) {
            for (int i = 1; i <= 6; i++) insert.setString(i, users.getString(i));
            insert.setTimestamp(7, users.getTimestamp(7));
            insert.setTimestamp(8, users.getTimestamp(8));
            insert.executeUpdate();
            count++;
          }
        }
        try (ResultSet result = target.createStatement().executeQuery("SELECT COUNT(*) FROM users")) {
          result.next();
          if (result.getLong(1) != count) throw new IllegalStateException("Conteo inconsistente");
        }
        target.commit();
        System.out.println("Migracion confirmada: " + count + " usuarios; UUID, hashes y fechas conservados.");
      } catch (Exception failure) {
        target.rollback();
        throw failure;
      }
    }
  }
}
