package com.example.sqlite;

import android.content.Context;
import android.content.SharedPreferences;

// Guarda la sesión activa (quién inició sesión y con qué rol) en preferencias locales.
// RF-01: asocia el rol (id_rol) a la sesión activa.
// RF-02: cerrar() limpia todo el estado de sesión.
public class SessionManager {

    public static final String ROL_ADMIN = "ADMIN";
    private static final String PREFS = "sesion_bodega";

    private final SharedPreferences sp;

    public SessionManager(Context context) {
        sp = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public void iniciar(Usuario u) {
        sp.edit()
                .putLong("id_usuario", u.idUsuario)
                .putString("username", u.username)
                .putString("nombre", u.nombreCompleto)
                .putLong("id_rol", u.idRol)
                .putString("nombre_rol", u.nombreRol)
                .apply();
    }

    public void cerrar() {
        sp.edit().clear().apply();
    }

    public boolean estaLogueado() { return sp.getLong("id_usuario", -1) != -1; }
    public long getIdUsuario()    { return sp.getLong("id_usuario", -1); }
    public String getUsername()   { return sp.getString("username", ""); }
    public String getNombre()     { return sp.getString("nombre", ""); }
    public long getIdRol()        { return sp.getLong("id_rol", -1); }
    public String getNombreRol()  { return sp.getString("nombre_rol", ""); }
    public boolean esAdmin()      { return ROL_ADMIN.equals(getNombreRol()); }
}
