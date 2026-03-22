package com.cargohub.mobile.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.cargohub.mobile.R;
import com.cargohub.mobile.data.model.Porte;

import java.util.ArrayList;
import java.util.List;

public class PorteSpinnerAdapter extends ArrayAdapter<Porte> {

    public PorteSpinnerAdapter(@NonNull Context context) {
        super(context, R.layout.item_porte_spinner, new ArrayList<>());
    }

    public void setPortes(List<Porte> portes) {
        clear();
        if (portes != null) {
            addAll(portes);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext())
                    .inflate(R.layout.item_porte_spinner, parent, false);
        }
        Porte porte = getItem(position);
        TextView text = convertView.findViewById(R.id.porteSpinnerText);
        text.setText(porte != null ? porte.toString() : "");
        return convertView;
    }

    @Override
    public View getDropDownView(int position, @Nullable View convertView,
                                @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext())
                    .inflate(R.layout.item_porte_spinner_dropdown, parent, false);
        }
        Porte porte = getItem(position);
        TextView text = convertView.findViewById(R.id.porteSpinnerText);
        text.setText(porte != null ? porte.toString() : "");
        return convertView;
    }
}
