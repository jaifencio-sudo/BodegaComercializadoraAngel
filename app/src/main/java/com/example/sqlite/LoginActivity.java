package com.example.sqlite;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

// RF-01: Iniciar sesión con username + contraseña (solo usuarios activos)
public class LoginActivity extends AppCompatActivity {

    private TextInputEditText txtUsuario, txtPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Si ya hay una sesión guardada, entra directo al menú
        if (new SessionManager(this).estaLogueado()) {
            irAlMenu();
            return;
        }

        setContentView(R.layout.activity_login);
        txtUsuario = findViewById(R.id.txtUsuario);
        txtPassword = findViewById(R.id.txtPassword);
        MaterialButton btnIngresar = findViewById(R.id.btnIngresar);

        btnIngresar.setOnClickListener(v -> ingresar());
    }

    private void ingresar() {
        String username = txtUsuario.getText().toString().trim();
        String password = txtPassword.getText().toString();

        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Ingrese usuario y contraseña.", Toast.LENGTH_SHORT).show();
            return;
        }

        DBHelper helper = new DBHelper(this);
        try {
            Usuario u = helper.autenticar(username, password);
            if (u == null) {
                Toast.makeText(this, "Credenciales incorrectas o usuario inactivo.", Toast.LENGTH_LONG).show();
                txtPassword.setText("");
                return;
            }
            new SessionManager(this).iniciar(u); // guarda usuario + rol en la sesión
            irAlMenu();
        } catch (Exception e) {
            Toast.makeText(this, "Error al iniciar sesión: " + e.getMessage(), Toast.LENGTH_LONG).show();
        } finally {
            helper.close();
        }
    }

    private void irAlMenu() {
        Intent i = new Intent(this, MenuActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
        finish();
    }
}
