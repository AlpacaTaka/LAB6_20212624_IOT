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

import com.example.lab6_20212624.model.Record;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

// adaptador para los registros, muestra cada item en la lista (pequeño coment con errores)
public class RecordAdapter extends RecyclerView.Adapter<RecordAdapter.ViewHolder> {

    public interface OnItemActionListener {
        void onEdit(Record record);
        void onDelete(Record record);
    }

    private final Context context;
    private final List<Record> items = new ArrayList<>();
    private final OnItemActionListener listener;

    public RecordAdapter(Context context, OnItemActionListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void setItems(List<Record> records) {
        items.clear();
        if (records != null) items.addAll(records);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_record, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Record r = items.get(position);
        holder.tvRecordId.setText("ID: " + r.getRecordId());
        holder.tvDate.setText(new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(r.getDate()));
        holder.tvFuelType.setText(r.getFuelType());
        holder.tvVehicle.setText(r.getVehicleDisplay());
        holder.tvLiters.setText(String.format(Locale.getDefault(), "%.2f L", r.getLiters()));
        holder.tvMileage.setText(String.format(Locale.getDefault(), "%d km", r.getMileage()));
        holder.tvPrice.setText(String.format(Locale.getDefault(), "S/ %.2f", r.getTotalPrice()));

        holder.btnMenu.setOnClickListener(view -> {
            PopupMenu popup = new PopupMenu(context, holder.btnMenu);
            popup.getMenu().add(0, 1, 0, context.getString(R.string.edit));
            popup.getMenu().add(0, 2, 1, context.getString(R.string.delete));
            popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    if (item.getItemId() == 1) {
                        listener.onEdit(r);
                        return true;
                    } else if (item.getItemId() == 2) {
                        listener.onDelete(r);
                        return true;
                    }
                    return false;
                }
            });
            popup.show();
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvRecordId, tvDate, tvFuelType, tvVehicle, tvLiters, tvMileage, tvPrice;
        ImageButton btnMenu;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvRecordId = itemView.findViewById(R.id.tvRecordId);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvFuelType = itemView.findViewById(R.id.tvFuelType);
            tvVehicle = itemView.findViewById(R.id.tvVehicle);
            tvLiters = itemView.findViewById(R.id.tvLiters);
            tvMileage = itemView.findViewById(R.id.tvMileage);
            tvPrice = itemView.findViewById(R.id.tvPrice);
            btnMenu = itemView.findViewById(R.id.btnMenu);
        }
    }
}
