package com.example.sqlite;

import android.database.sqlite.SQLiteConstraintException;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;
import android.widget.ListView;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.List;

//  Administración de usuarios y roles (registrar y listar). Solo accesible para ADMIN.
public class UsuariosActivity extends ProtegidaActivity {

    private ListView lista;
    private MaterialButton btnAgregar;
    private ArrayAdapter<String> adapter;
    private final List<String> textos = new ArrayList<>();
    private final List<Usuario> usuarios = new ArrayList<>();
    private final List<Rol> roles = new ArrayList<>();
    private boolean verUsuarios = true; // true = pestaña Usuarios, false = pestaña Roles

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!sesion.esAdmin()) {
            Toast.makeText(this, "Acceso solo para administradores.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        setContentView(R.layout.activity_usuarios);
        lista = findViewById(R.id.lstUsuarios);
        btnAgregar = findViewById(R.id.btnAgregar);
        MaterialButtonToggleGroup tabs = findViewById(R.id.tabs);
        MaterialButton btnVolver = findViewById(R.id.btnVolver);

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, textos);
        lista.setAdapter(adapter);

        tabs.check(R.id.tabUsuarios);
        tabs.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;
            verUsuarios = (checkedId == R.id.tabUsuarios);
            cargar();
        });

        lista.setOnItemClickListener((parent, view, position, id) -> {
            if (verUsuarios) dialogoUsuario(usuarios.get(position));
            else dialogoRol(roles.get(position));
        });
        btnAgregar.setOnClickListener(v -> {
            if (verUsuarios) dialogoUsuario(null);
            else dialogoRol(null);
        });
        btnVolver.setOnClickListener(v -> finish());

        cargar();
    }

    private void cargar() {
        btnAgregar.setText(verUsuarios ? "+ Nuevo usuario" : "+ Nuevo rol");
        textos.clear();
        DBHelper helper = new DBHelper(this);
        try {
            if (verUsuarios) {
                usuarios.clear();
                usuarios.addAll(helper.listarUsuarios());
                for (Usuario u : usuarios) {
                    textos.add(u.username + "  —  " + u.nombreCompleto + "\nRol: " + u.nombreRol
                            + "  •  " + (u.activo ? "Activo" : "INACTIVO"));
                }
            } else {
                roles.clear();
                roles.addAll(helper.listarRoles());
                for (Rol r : roles) {
                    String desc = (r.descripcion == null || r.descripcion.isEmpty()) ? "(sin descripción)" : r.descripcion;
                    textos.add(r.nombreRol + "\n" + desc);
                }
            }
        } catch (Exception e) {
            Toast.makeText(this, "Error al leer: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        } finally {
            helper.close();
        }
        adapter.notifyDataSetChanged();
    }

    // ------------------------------------------------------------ Diálogo de usuario

    private void dialogoUsuario(Usuario existente) {
        View v = LayoutInflater.from(this).inflate(R.layout.dialog_usuario, null);
        TextInputLayout tilUser = v.findViewById(R.id.tilUsername);
        TextInputEditText txtUser = v.findViewById(R.id.dlgUsername);
        TextInputEditText txtNombre = v.findViewById(R.id.dlgNombreCompleto);
        TextInputLayout tilPass = v.findViewById(R.id.tilPassword);
        TextInputEditText txtPass = v.findViewById(R.id.dlgPassword);
        Spinner spRol = v.findViewById(R.id.dlgRol);
        MaterialSwitch swActivo = v.findViewById(R.id.dlgActivo);

        // Carga los roles en el Spinner
        List<Rol> listaRoles;
        DBHelper h = new DBHelper(this);
        try {
            listaRoles = h.listarRoles();
        } finally {
            h.close();
        }
        spRol.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, listaRoles));

        boolean esNuevo = (existente == null);
        boolean esPropio = !esNuevo && existente.idUsuario == sesion.getIdUsuario();

        if (esNuevo) {
            swActivo.setChecked(true);
        } else {
            txtUser.setText(existente.username);
            tilUser.setEnabled(false);   // el username no se cambia
            txtUser.setEnabled(false);
            txtNombre.setText(existente.nombreCompleto);
            tilPass.setHint("Nueva contraseña (opcional)");
            swActivo.setChecked(existente.activo);
            for (int i = 0; i < listaRoles.size(); i++) {
                if (listaRoles.get(i).idRol == existente.idRol) spRol.setSelection(i);
            }
            if (esPropio) { // evita que el admin se quite el acceso a sí mismo
                swActivo.setEnabled(false);
                spRol.setEnabled(false);
            }
        }

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(esNuevo ? "Nuevo usuario" : "Editar usuario")
                .setView(v)
                .setPositiveButton("Guardar", null)
                .setNegativeButton("Cancelar", null)
                .create();

        // Se sobreescribe el click para NO cerrar el diálogo si hay errores de validación
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(b -> {
            String username = txtUser.getText().toString().trim();
            String nombre = txtNombre.getText().toString().trim();
            String pass = txtPass.getText().toString();
            Rol rol = (Rol) spRol.getSelectedItem();

            if (username.isEmpty() || nombre.isEmpty() || rol == null) {
                Toast.makeText(this, "Complete usuario, nombre y rol.", Toast.LENGTH_SHORT).show();
                return;
            }
            if (esNuevo && pass.length() < 6) {
                Toast.makeText(this, "La contraseña debe tener al menos 6 caracteres.", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!esNuevo && !pass.isEmpty() && pass.length() < 6) {
                Toast.makeText(this, "La nueva contraseña debe tener al menos 6 caracteres.", Toast.LENGTH_SHORT).show();
                return;
            }

            DBHelper helper = new DBHelper(this);
            try {
                if (esNuevo) {
                    helper.insertarUsuario(username, pass, nombre, rol.idRol, swActivo.isChecked());
                } else {
                    helper.actualizarUsuario(existente.idUsuario, existente.username, nombre,
                            rol.idRol, swActivo.isChecked(), pass);
                }
                Toast.makeText(this, "Usuario guardado.", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
                cargar();
            } catch (SQLiteConstraintException e) {
                Toast.makeText(this, "El username ya existe. Debe ser único.", Toast.LENGTH_LONG).show();
            } catch (Exception e) {
                Toast.makeText(this, "Error al guardar: " + e.getMessage(), Toast.LENGTH_LONG).show();
            } finally {
                helper.close();
            }
        }));
        dialog.show();
    }

    // ------------------------------------------------------------ Diálogo de rol

    private void dialogoRol(Rol existente) {
        View v = LayoutInflater.from(this).inflate(R.layout.dialog_rol, null);
        TextInputEditText txtNombre = v.findViewById(R.id.dlgNombreRol);
        TextInputEditText txtDesc = v.findViewById(R.id.dlgDescripcion);

        boolean esNuevo = (existente == null);
        if (!esNuevo) {
            txtNombre.setText(existente.nombreRol);
            txtDesc.setText(existente.descripcion);
            // El rol ADMIN no se puede renombrar (el sistema lo usa para dar permisos)
            if (SessionManager.ROL_ADMIN.equals(existente.nombreRol)) txtNombre.setEnabled(false);
        }

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(esNuevo ? "Nuevo rol" : "Editar rol")
                .setView(v)
                .setPositiveButton("Guardar", null)
                .setNegativeButton("Cancelar", null)
                .create();

        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(b -> {
            String nombre = txtNombre.getText().toString().trim().toUpperCase();
            String desc = txtDesc.getText().toString().trim();
            if (nombre.isEmpty()) {
                Toast.makeText(this, "Ingrese el nombre del rol.", Toast.LENGTH_SHORT).show();
                return;
            }

            DBHelper helper = new DBHelper(this);
            try {
                if (esNuevo) helper.insertarRol(nombre, desc);
                else helper.actualizarRol(existente.idRol, nombre, desc);
                Toast.makeText(this, "Rol guardado.", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
                cargar();
            } catch (SQLiteConstraintException e) {
                Toast.makeText(this, "El nombre del rol ya existe. Debe ser único.", Toast.LENGTH_LONG).show();
            } catch (Exception e) {
                Toast.makeText(this, "Error al guardar: " + e.getMessage(), Toast.LENGTH_LONG).show();
            } finally {
                helper.close();
            }
        }));
        dialog.show();
    }
}
