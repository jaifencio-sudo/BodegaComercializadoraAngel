package com.example.sqlite;

// Importa las clases necesarias para gestionar SQLite
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DBHelper extends SQLiteOpenHelper {

    //  Se MANTIENE la misma base de datos (mismo archivo). Solo sube la versión 1 -> 2.
    private static final String DB_NAME = "BD_EJEMPLO.db";
    private static final int DB_VERSION = 2;

    public static final String[] CATEGORIAS = {
            "Frutos secos", "Semillas", "Frutas deshidratadas", "Mezclas", "Otros"
    };

    public DBHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true); // Activa las llaves foráneas (USUARIO -> ROL)
    }

    //  Se ejecuta UNA SOLA VEZ al crear la base de datos por primera vez
    @Override
    public void onCreate(SQLiteDatabase db) {
        crearTablaPersona(db);
        crearTablasBodega(db);
        sembrarDatos(db);
    }

    //  Al subir de versión NO se borra nada: la tabla "persona" y sus datos quedan intactos
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            crearTablasBodega(db);
            sembrarDatos(db);
        }
    }

    // ---------------------------------------------------------------- Esquema

    // Tabla original del proyecto (se conserva tal cual)
    private void crearTablaPersona(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS persona (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "nombre TEXT NOT NULL, " +
                "apellido TEXT NOT NULL, " +
                "edad INTEGER NOT NULL)");
    }

    private void crearTablasBodega(SQLiteDatabase db) {
        // RF-27: nombre_rol único (UK)
        db.execSQL("CREATE TABLE IF NOT EXISTS ROL (" +
                "id_rol INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "nombre_rol TEXT NOT NULL UNIQUE, " +
                "descripcion TEXT)");

        // RF-01 / RF-27: username único (UK), password_hash, activo (1 = activo)
        db.execSQL("CREATE TABLE IF NOT EXISTS USUARIO (" +
                "id_usuario INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "username TEXT NOT NULL UNIQUE, " +
                "password_hash TEXT NOT NULL, " +
                "nombre_completo TEXT NOT NULL, " +
                "id_rol INTEGER NOT NULL, " +
                "activo INTEGER NOT NULL DEFAULT 1, " +
                "FOREIGN KEY (id_rol) REFERENCES ROL(id_rol))");

        // Inventario de la bodega (stock en kg, precio CLP por kg)
        db.execSQL("CREATE TABLE IF NOT EXISTS PRODUCTO (" +
                "id_producto INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "nombre TEXT NOT NULL UNIQUE, " +
                "categoria TEXT NOT NULL, " +
                "stock REAL NOT NULL DEFAULT 0, " +
                "stock_minimo REAL NOT NULL DEFAULT 0, " +
                "precio_kg INTEGER NOT NULL DEFAULT 0)");
    }

    // Datos iniciales: 2 roles, 1 usuario administrador y algunos productos de ejemplo
    private void sembrarDatos(SQLiteDatabase db) {
        db.execSQL("INSERT OR IGNORE INTO ROL (nombre_rol, descripcion) VALUES (?, ?)",
                new Object[]{"ADMIN", "Administra usuarios, roles e inventario"});
        db.execSQL("INSERT OR IGNORE INTO ROL (nombre_rol, descripcion) VALUES (?, ?)",
                new Object[]{"BODEGUERO", "Gestiona el inventario de la bodega"});

        // Usuario inicial: admin / admin123  (cámbiela después de entrar)
        db.execSQL("INSERT OR IGNORE INTO USUARIO (username, password_hash, nombre_completo, id_rol, activo) " +
                        "SELECT ?, ?, ?, id_rol, 1 FROM ROL WHERE nombre_rol = 'ADMIN'",
                new Object[]{"admin", hash("admin", "admin123"), "Administrador"});

        Object[][] productos = {
                {"Almendras", "Frutos secos", 25.0, 5.0, 14500},
                {"Nueces mariposa", "Frutos secos", 18.5, 5.0, 12900},
                {"Maní salado", "Frutos secos", 3.0, 8.0, 5200},
                {"Pistachos", "Frutos secos", 9.0, 3.0, 19800},
                {"Castañas de cajú", "Frutos secos", 12.0, 4.0, 17500},
                {"Avellanas", "Frutos secos", 7.5, 3.0, 15900},
                {"Pasas rubias", "Frutas deshidratadas", 15.0, 5.0, 4900},
                {"Semillas de zapallo", "Semillas", 6.0, 2.0, 8900},
                {"Mix tropical", "Mezclas", 10.0, 4.0, 9900},
        };
        for (Object[] p : productos) {
            db.execSQL("INSERT OR IGNORE INTO PRODUCTO (nombre, categoria, stock, stock_minimo, precio_kg) " +
                    "VALUES (?, ?, ?, ?, ?)", p);
        }
    }

    // ---------------------------------------------------------------- Seguridad

    // Hash SHA-256 de (username en minúsculas + ":" + contraseña). Nunca se guarda la clave en texto plano.
    public static String hash(String username, String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest((username.trim().toLowerCase(Locale.ROOT) + ":" + password)
                    .getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    // RF-01: solo ingresan usuarios activos (activo = 1) con credenciales correctas.
    // Devuelve el usuario con su rol, o null si no coincide.
    public Usuario autenticar(String username, String password) {
        SQLiteDatabase db = getReadableDatabase();
        String sql = "SELECT u.id_usuario, u.username, u.nombre_completo, u.id_rol, r.nombre_rol, u.activo " +
                "FROM USUARIO u JOIN ROL r ON r.id_rol = u.id_rol " +
                "WHERE u.username = ? AND u.password_hash = ? AND u.activo = 1";
        try (Cursor c = db.rawQuery(sql, new String[]{username.trim(), hash(username, password)})) {
            return c.moveToFirst() ? leerUsuario(c) : null;
        }
    }

    // Para detectar si una sesión guardada pertenece a un usuario que fue desactivado o eliminado
    public boolean usuarioActivo(long idUsuario) {
        SQLiteDatabase db = getReadableDatabase();
        try (Cursor c = db.rawQuery("SELECT 1 FROM USUARIO WHERE id_usuario = ? AND activo = 1",
                new String[]{String.valueOf(idUsuario)})) {
            return c.moveToFirst();
        }
    }

    private Usuario leerUsuario(Cursor c) {
        Usuario u = new Usuario();
        u.idUsuario = c.getLong(0);
        u.username = c.getString(1);
        u.nombreCompleto = c.getString(2);
        u.idRol = c.getLong(3);
        u.nombreRol = c.getString(4);
        u.activo = c.getInt(5) == 1;
        return u;
    }

    // ---------------------------------------------------------------- Roles (RF-27)

    public List<Rol> listarRoles() {
        List<Rol> lista = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        try (Cursor c = db.rawQuery("SELECT id_rol, nombre_rol, descripcion FROM ROL ORDER BY nombre_rol", null)) {
            while (c.moveToNext()) {
                Rol r = new Rol();
                r.idRol = c.getLong(0);
                r.nombreRol = c.getString(1);
                r.descripcion = c.getString(2);
                lista.add(r);
            }
        }
        return lista;
    }

    // Lanza SQLiteConstraintException si el nombre del rol ya existe (UK)
    public void insertarRol(String nombreRol, String descripcion) throws SQLiteConstraintException {
        ContentValues v = new ContentValues();
        v.put("nombre_rol", nombreRol);
        v.put("descripcion", descripcion);
        getWritableDatabase().insertOrThrow("ROL", null, v);
    }

    public void actualizarRol(long idRol, String nombreRol, String descripcion) throws SQLiteConstraintException {
        ContentValues v = new ContentValues();
        v.put("nombre_rol", nombreRol);
        v.put("descripcion", descripcion);
        getWritableDatabase().updateWithOnConflict("ROL", v, "id_rol = ?",
                new String[]{String.valueOf(idRol)}, SQLiteDatabase.CONFLICT_ABORT);
    }

    // ---------------------------------------------------------------- Usuarios (RF-27)

    public List<Usuario> listarUsuarios() {
        List<Usuario> lista = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        String sql = "SELECT u.id_usuario, u.username, u.nombre_completo, u.id_rol, r.nombre_rol, u.activo " +
                "FROM USUARIO u JOIN ROL r ON r.id_rol = u.id_rol ORDER BY u.username";
        try (Cursor c = db.rawQuery(sql, null)) {
            while (c.moveToNext()) lista.add(leerUsuario(c));
        }
        return lista;
    }

    // Lanza SQLiteConstraintException si el username ya existe (UK)
    public void insertarUsuario(String username, String password, String nombreCompleto,
                                long idRol, boolean activo) throws SQLiteConstraintException {
        ContentValues v = new ContentValues();
        v.put("username", username);
        v.put("password_hash", hash(username, password));
        v.put("nombre_completo", nombreCompleto);
        v.put("id_rol", idRol);
        v.put("activo", activo ? 1 : 0);
        getWritableDatabase().insertOrThrow("USUARIO", null, v);
    }

    // nuevaPassword puede ser null/vacía para dejar la contraseña actual
    public void actualizarUsuario(long idUsuario, String username, String nombreCompleto,
                                  long idRol, boolean activo, String nuevaPassword) {
        ContentValues v = new ContentValues();
        v.put("nombre_completo", nombreCompleto);
        v.put("id_rol", idRol);
        v.put("activo", activo ? 1 : 0);
        if (nuevaPassword != null && !nuevaPassword.isEmpty()) {
            v.put("password_hash", hash(username, nuevaPassword));
        }
        getWritableDatabase().update("USUARIO", v, "id_usuario = ?", new String[]{String.valueOf(idUsuario)});
    }

    // ---------------------------------------------------------------- Productos

    public List<Producto> listarProductos(String filtro) {
        List<Producto> lista = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        String sql = "SELECT id_producto, nombre, categoria, stock, stock_minimo, precio_kg FROM PRODUCTO";
        String[] args = null;
        if (filtro != null && !filtro.trim().isEmpty()) {
            sql += " WHERE nombre LIKE ? OR categoria LIKE ?";
            String like = "%" + filtro.trim() + "%";
            args = new String[]{like, like};
        }
        sql += " ORDER BY nombre";
        try (Cursor c = db.rawQuery(sql, args)) {
            while (c.moveToNext()) lista.add(leerProducto(c));
        }
        return lista;
    }

    public Producto obtenerProducto(long id) {
        SQLiteDatabase db = getReadableDatabase();
        try (Cursor c = db.rawQuery("SELECT id_producto, nombre, categoria, stock, stock_minimo, precio_kg " +
                "FROM PRODUCTO WHERE id_producto = ?", new String[]{String.valueOf(id)})) {
            return c.moveToFirst() ? leerProducto(c) : null;
        }
    }

    private Producto leerProducto(Cursor c) {
        Producto p = new Producto();
        p.idProducto = c.getLong(0);
        p.nombre = c.getString(1);
        p.categoria = c.getString(2);
        p.stock = c.getDouble(3);
        p.stockMinimo = c.getDouble(4);
        p.precioKg = c.getInt(5);
        return p;
    }

    public int contarProductos() {
        try (Cursor c = getReadableDatabase().rawQuery("SELECT COUNT(*) FROM PRODUCTO", null)) {
            return c.moveToFirst() ? c.getInt(0) : 0;
        }
    }

    public int contarStockBajo() {
        try (Cursor c = getReadableDatabase().rawQuery(
                "SELECT COUNT(*) FROM PRODUCTO WHERE stock <= stock_minimo", null)) {
            return c.moveToFirst() ? c.getInt(0) : 0;
        }
    }

    // Lanza SQLiteConstraintException si el nombre del producto ya existe
    public void insertarProducto(Producto p) throws SQLiteConstraintException {
        getWritableDatabase().insertOrThrow("PRODUCTO", null, valores(p));
    }

    public void actualizarProducto(Producto p) throws SQLiteConstraintException {
        getWritableDatabase().updateWithOnConflict("PRODUCTO", valores(p), "id_producto = ?",
                new String[]{String.valueOf(p.idProducto)}, SQLiteDatabase.CONFLICT_ABORT);
    }

    public boolean eliminarProducto(long id) {
        return getWritableDatabase().delete("PRODUCTO", "id_producto = ?",
                new String[]{String.valueOf(id)}) > 0;
    }

    private ContentValues valores(Producto p) {
        ContentValues v = new ContentValues();
        v.put("nombre", p.nombre);
        v.put("categoria", p.categoria);
        v.put("stock", p.stock);
        v.put("stock_minimo", p.stockMinimo);
        v.put("precio_kg", p.precioKg);
        return v;
    }
}
