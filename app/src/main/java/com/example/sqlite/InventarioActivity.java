package com.example.sqlite;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

// Lista del inventario de la bodega con búsqueda y aviso de stock bajo
public class InventarioActivity extends ProtegidaActivity {

    private TextInputEditText txtBuscar;
    private ProductoAdapter adapter;
    private final List<Producto> productos = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inventario);

        txtBuscar = findViewById(R.id.txtBuscar);
        ListView lista = findViewById(R.id.lstProductos);
        MaterialButton btnNuevo = findViewById(R.id.btnNuevo);

        adapter = new ProductoAdapter(this, productos);
        lista.setAdapter(adapter);

        // Tocar un producto abre el formulario en modo edición
        lista.setOnItemClickListener((parent, view, position, id) -> {
            Intent i = new Intent(this, ProductoActivity.class);
            i.putExtra(ProductoActivity.EXTRA_ID, productos.get(position).idProducto);
            startActivity(i);
        });

        btnNuevo.setOnClickListener(v -> startActivity(new Intent(this, ProductoActivity.class)));

        txtBuscar.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { cargar(); }
            @Override public void afterTextChanged(Editable s) { }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        cargar(); // Recarga al volver desde el formulario
    }

    private void cargar() {
        DBHelper helper = new DBHelper(this);
        try {
            productos.clear();
            productos.addAll(helper.listarProductos(txtBuscar.getText().toString()));
            adapter.notifyDataSetChanged();
        } catch (Exception e) {
            Toast.makeText(this, "Error al leer: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        } finally {
            helper.close();
        }
    }

    private static class ProductoAdapter extends ArrayAdapter<Producto> {
        ProductoAdapter(Context ctx, List<Producto> data) {
            super(ctx, 0, data);
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            View v = convertView != null ? convertView
                    : LayoutInflater.from(getContext()).inflate(R.layout.item_producto, parent, false);
            Producto p = getItem(position);

            ((TextView) v.findViewById(R.id.itemNombre)).setText(p.nombre);
            ((TextView) v.findViewById(R.id.itemDetalle)).setText(
                    p.categoria + "  •  " + Producto.formatoPrecio(p.precioKg) + "/kg");

            TextView stock = v.findViewById(R.id.itemStock);
            stock.setText(Producto.formatoKg(p.stock) + " kg");
            stock.setTextColor(getContext().getColor(p.stockBajo() ? R.color.btn_delete : R.color.primary));

            v.findViewById(R.id.itemAlerta).setVisibility(p.stockBajo() ? View.VISIBLE : View.GONE);
            return v;
        }
    }
}
