package com.example.sqlite;

// Modelo de la tabla ROL
public class Rol {
    public long idRol;
    public String nombreRol;
    public String descripcion;

    @Override
    public String toString() {
        return nombreRol; // Así se muestra en el Spinner
    }
}
