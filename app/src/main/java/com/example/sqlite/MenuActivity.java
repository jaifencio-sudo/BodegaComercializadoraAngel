package com.example.sqlite;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import com.google.android.material.button.MaterialButton;

// Panel principal: resumen del inventario y accesos según el rol de la sesión
public class MenuActivity extends ProtegidaActivity {

    private TextView lblSaludo, lblRol, lblTotal, lblBajo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);

        lblSaludo = findViewById(R.id.lblSaludo);
        lblRol = findViewById(R.id.lblRol);
        lblTotal = findViewById(R.id.lblTotal);
        lblBajo = findViewById(R.id.lblBajo);

        MaterialButton btnInventario = findViewById(R.id.btnInventario);
        MaterialButton btnNuevo = findViewById(R.id.btnNuevoProducto);
        MaterialButton btnUsuarios = findViewById(R.id.btnUsuarios);
        MaterialButton btnSalir = findViewById(R.id.btnCerrarSesion);

        lblSaludo.setText("Hola, " + sesion.getNombre());
        lblRol.setText("Rol: " + sesion.getNombreRol());

        // Solo el ADMIN administra usuarios y roles
        btnUsuarios.setVisibility(sesion.esAdmin() ? android.view.View.VISIBLE : android.view.View.GONE);

        btnInventario.setOnClickListener(v -> startActivity(new Intent(this, InventarioActivity.class)));
        btnNuevo.setOnClickListener(v -> startActivity(new Intent(this, ProductoActivity.class)));
        btnUsuarios.setOnClickListener(v -> startActivity(new Intent(this, UsuariosActivity.class)));
        btnSalir.setOnClickListener(v -> cerrarSesion());
    }

    @Override
    protected void onResume() {
        super.onResume();
        DBHelper helper = new DBHelper(this);
        try {
            lblTotal.setText(String.valueOf(helper.contarProductos()));
            lblBajo.setText(String.valueOf(helper.contarStockBajo()));
        } finally {
            helper.close();
        }
    }
}
