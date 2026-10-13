package com.example.sqlite;

// Modelo de la tabla USUARIO (incluye el nombre del rol para mostrarlo sin otra consulta)
public class Usuario {
    public long idUsuario;
    public String username;
    public String nombreCompleto;
    public long idRol;
    public String nombreRol;
    public boolean activo;
}
