package com.example.lab6_20212624;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.lab6_20212624.model.Vehicle;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

// adaptador para vehiculos, muestra card por vehiculo (comentario en espaniol con faltas)
public class VehicleAdapter extends RecyclerView.Adapter<VehicleAdapter.ViewHolder> {

    public interface OnItemActionListener {
        void onEdit(Vehicle vehicle);
        void onDelete(Vehicle vehicle);
        void onGenerateQR(Vehicle vehicle);
    }

    private final Context context;
    private final List<Vehicle> items = new ArrayList<>();
    private final OnItemActionListener listener;

    public VehicleAdapter(Context context, OnItemActionListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void setItems(List<Vehicle> vehicles) {
        items.clear();
        if (vehicles != null) items.addAll(vehicles);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_vehicle, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Vehicle v = items.get(position);
        holder.tvVehicleId.setText(v.getVehicleId());
        holder.tvLicensePlate.setText(v.getLicensePlate());
        holder.tvBrandModel.setText(v.getBrandModel());
        holder.tvYear.setText(String.valueOf(v.getYear()));
        holder.tvTechnicalReview.setText(v.getTechnicalReviewDate() != null ? "Revisión: " + new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(v.getTechnicalReviewDate()) : "");

        holder.btnMenu.setOnClickListener(view -> {
            PopupMenu popup = new PopupMenu(context, holder.btnMenu);
            popup.getMenu().add(0, 1, 0, context.getString(R.string.edit));
            popup.getMenu().add(0, 2, 1, context.getString(R.string.delete));
            popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    if (item.getItemId() == 1) {
                        listener.onEdit(v);
                        return true;
                    } else if (item.getItemId() == 2) {
                        listener.onDelete(v);
                        return true;
                    }
                    return false;
                }
            });
            popup.show();
        });

        holder.btnGenerateQR.setOnClickListener(view -> listener.onGenerateQR(v));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvVehicleId, tvLicensePlate, tvBrandModel, tvYear, tvTechnicalReview;
        ImageButton btnMenu;
        com.google.android.material.button.MaterialButton btnGenerateQR;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvVehicleId = itemView.findViewById(R.id.tvVehicleId);
            tvLicensePlate = itemView.findViewById(R.id.tvLicensePlate);
            tvBrandModel = itemView.findViewById(R.id.tvBrandModel);
            tvYear = itemView.findViewById(R.id.tvYear);
            tvTechnicalReview = itemView.findViewById(R.id.tvTechnicalReview);
            btnMenu = itemView.findViewById(R.id.btnMenu);
            btnGenerateQR = itemView.findViewById(R.id.btnGenerateQR);
        }
    }
}
