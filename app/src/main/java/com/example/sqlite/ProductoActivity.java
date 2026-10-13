package com.example.sqlite;

import android.database.sqlite.SQLiteConstraintException;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;

// Formulario de producto: crea uno nuevo o edita/elimina uno existente (EXTRA_ID)
public class ProductoActivity extends ProtegidaActivity {

    public static final String EXTRA_ID = "id_producto";

    private TextInputEditText txtNombre, txtStock, txtStockMin, txtPrecio;
    private MaterialAutoCompleteTextView cmbCategoria;
    private MaterialButton btnEliminar;
    private long idProducto = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_producto);

        TextView titulo = findViewById(R.id.lblTitulo);
        txtNombre = findViewById(R.id.txtNombre);
        cmbCategoria = findViewById(R.id.cmbCategoria);
        txtStock = findViewById(R.id.txtStock);
        txtStockMin = findViewById(R.id.txtStockMin);
        txtPrecio = findViewById(R.id.txtPrecio);
        MaterialButton btnGuardar = findViewById(R.id.btnGuardar);
        btnEliminar = findViewById(R.id.btnEliminar);
        MaterialButton btnVolver = findViewById(R.id.btnVolver);

        cmbCategoria.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, DBHelper.CATEGORIAS));
        cmbCategoria.setText(DBHelper.CATEGORIAS[0], false);

        idProducto = getIntent().getLongExtra(EXTRA_ID, -1);
        btnEliminar.setVisibility(View.GONE);

        if (idProducto != -1) {
            titulo.setText("Editar producto");
            cargarProducto();
            // Solo el ADMIN puede eliminar productos
            if (sesion.esAdmin()) btnEliminar.setVisibility(View.VISIBLE);
        }

        btnGuardar.setOnClickListener(v -> guardar());
        btnEliminar.setOnClickListener(v -> confirmarEliminar());
        btnVolver.setOnClickListener(v -> finish());
    }

    private void cargarProducto() {
        DBHelper helper = new DBHelper(this);
        try {
            Producto p = helper.obtenerProducto(idProducto);
            if (p == null) {
                Toast.makeText(this, "El producto ya no existe.", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
            txtNombre.setText(p.nombre);
            cmbCategoria.setText(p.categoria, false);
            txtStock.setText(Producto.formatoKg(p.stock));
            txtStockMin.setText(Producto.formatoKg(p.stockMinimo));
            txtPrecio.setText(String.valueOf(p.precioKg));
        } finally {
            helper.close();
        }
    }

    private void guardar() {
        String nombre = txtNombre.getText().toString().trim();
        String categoria = cmbCategoria.getText().toString().trim();
        String stockStr = txtStock.getText().toString().trim().replace(',', '.');
        String minStr = txtStockMin.getText().toString().trim().replace(',', '.');
        String precioStr = txtPrecio.getText().toString().trim();

        if (nombre.isEmpty() || categoria.isEmpty() || stockStr.isEmpty()
                || minStr.isEmpty() || precioStr.isEmpty()) {
            Toast.makeText(this, "Complete todos los campos.", Toast.LENGTH_SHORT).show();
            return;
        }

        Producto p = new Producto();
        try {
            p.nombre = nombre;
            p.categoria = categoria;
            p.stock = Double.parseDouble(stockStr);
            p.stockMinimo = Double.parseDouble(minStr);
            p.precioKg = Integer.parseInt(precioStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Stock, mínimo y precio deben ser números válidos.", Toast.LENGTH_LONG).show();
            return;
        }
        if (p.stock < 0 || p.stockMinimo < 0 || p.precioKg < 0) {
            Toast.makeText(this, "Los valores no pueden ser negativos.", Toast.LENGTH_SHORT).show();
            return;
        }

        DBHelper helper = new DBHelper(this);
        try {
            if (idProducto == -1) {
                helper.insertarProducto(p);
                Toast.makeText(this, "Producto guardado.", Toast.LENGTH_SHORT).show();
            } else {
                p.idProducto = idProducto;
                helper.actualizarProducto(p);
                Toast.makeText(this, "Producto actualizado.", Toast.LENGTH_SHORT).show();
            }
            finish();
        } catch (SQLiteConstraintException e) {
            Toast.makeText(this, "Ya existe un producto con ese nombre.", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "Error al guardar: " + e.getMessage(), Toast.LENGTH_LONG).show();
        } finally {
            helper.close();
        }
    }

    private void confirmarEliminar() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Eliminar producto")
                .setMessage("¿Seguro que desea eliminar este producto del inventario?")
                .setPositiveButton("Eliminar", (d, w) -> eliminar())
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void eliminar() {
        DBHelper helper = new DBHelper(this);
        try {
            if (helper.eliminarProducto(idProducto)) {
                Toast.makeText(this, "Producto eliminado.", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(this, "No se encontró el producto.", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Error al eliminar: " + e.getMessage(), Toast.LENGTH_LONG).show();
        } finally {
            helper.close();
        }
    }
}
