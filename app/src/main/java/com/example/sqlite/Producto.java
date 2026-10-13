package com.example.sqlite;

import java.text.NumberFormat;
import java.util.Locale;

// Modelo de la tabla PRODUCTO (frutos secos, semillas, frutas deshidratadas, etc.)
// Todo el stock se maneja en kilos (kg) y el precio es CLP por kilo.
public class Producto {
    public long idProducto;
    public String nombre;
    public String categoria;
    public double stock;        // kg disponibles
    public double stockMinimo;  // bajo este nivel se avisa "STOCK BAJO"
    public int precioKg;        // CLP por kg

    public boolean stockBajo() {
        return stock <= stockMinimo;
    }

    public static String formatoKg(double v) {
        if (v == Math.rint(v)) return String.valueOf((long) v);
        return String.format(Locale.US, "%.2f", v);
    }

    public static String formatoPrecio(int clp) {
        return "$" + NumberFormat.getIntegerInstance(new Locale("es", "CL")).format(clp);
    }
}
