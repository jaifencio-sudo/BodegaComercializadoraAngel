package com.example.sqlite;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

// Base de todas las pantallas que requieren sesión iniciada.
// Si no hay sesión (o el usuario fue desactivado), vuelve al Login.
public abstract class ProtegidaActivity extends AppCompatActivity {

    protected SessionManager sesion;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sesion = new SessionManager(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!sesion.estaLogueado() || !usuarioSigueActivo()) {
            sesion.cerrar();
            irALogin();
        }
    }

    private boolean usuarioSigueActivo() {
        DBHelper helper = new DBHelper(this);
        try {
            return helper.usuarioActivo(sesion.getIdUsuario());
        } finally {
            helper.close();
        }
    }

    // RF-02: limpia el estado de sesión y redirige al Login
    protected void cerrarSesion() {
        sesion.cerrar();
        Toast.makeText(this, "Sesión cerrada.", Toast.LENGTH_SHORT).show();
        irALogin();
    }

    private void irALogin() {
        Intent i = new Intent(this, LoginActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
        finish();
    }
}
