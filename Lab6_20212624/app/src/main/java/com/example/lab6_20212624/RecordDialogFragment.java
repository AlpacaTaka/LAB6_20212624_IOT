package com.example.lab6_20212624;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.DatePicker;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.example.lab6_20212624.model.Record;
import com.example.lab6_20212624.model.Vehicle;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class RecordDialogFragment extends DialogFragment {

    private TextInputEditText etRecordId, etDate, etLiters, etMileage, etPrice;
    private Spinner spinnerVehicle;
    private RadioButton rbGasoline, rbLPG, rbCNG;
    private MaterialButton btnCancel, btnSave;
    private Record record;
    private List<Vehicle> vehicles;
    private boolean vehiclesLoaded = false;
    private String desiredVehicleDisplay = null;

    public static RecordDialogFragment newInstance(Record record, List<Vehicle> vehicles) {
        RecordDialogFragment f = new RecordDialogFragment();
        if (record != null) {
            Bundle args = new Bundle();
            args.putString("id", record.getId());
            args.putString("recordId", record.getRecordId());
            args.putString("vehicleDocId", record.getVehicleDocId());
            args.putString("vehicleDisplay", record.getVehicleDisplay());
            args.putString("date", record.getDate() != null ? new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(record.getDate()) : null);
            args.putDouble("liters", record.getLiters());
            args.putInt("mileage", record.getMileage());
            args.putDouble("price", record.getTotalPrice());
            args.putString("fuelType", record.getFuelType());
            f.setArguments(args);
        }
        f.vehicles = vehicles;
        return f;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Dialog d = super.onCreateDialog(savedInstanceState);
        d.setCanceledOnTouchOutside(false);
        return d;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.dialog_record, container, false);
        etRecordId = v.findViewById(R.id.etRecordId);
        spinnerVehicle = v.findViewById(R.id.spinnerVehicle);
        etDate = v.findViewById(R.id.etDate);
        etLiters = v.findViewById(R.id.etLiters);
        etMileage = v.findViewById(R.id.etMileage);
        etPrice = v.findViewById(R.id.etPrice);
        rbGasoline = v.findViewById(R.id.rbGasoline);
        rbLPG = v.findViewById(R.id.rbLPG);
        rbCNG = v.findViewById(R.id.rbCNG);
        btnCancel = v.findViewById(R.id.btnCancel);
        btnSave = v.findViewById(R.id.btnSave);

        // poblar spinner
        com.google.firebase.auth.FirebaseUser cu = FirebaseAuth.getInstance().getCurrentUser();
        btnSave.setEnabled(false); // disable save until vehicles are loaded
        if (vehicles != null && !vehicles.isEmpty()) {
            populateSpinnerFromVehicles();
        } else if (cu != null) {
            // fallback: cargar vehículos desde Firestore si no se pasaron
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection("users").document(cu.getUid()).collection("vehicles")
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        vehicles = new java.util.ArrayList<>();
                        java.util.List<String> items = new java.util.ArrayList<>();
                        for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                            Vehicle vobj = doc.toObject(Vehicle.class);
                            vobj.setId(doc.getId());
                            vehicles.add(vobj);
                            items.add(vobj.getVehicleId() + " - " + vobj.getLicensePlate());
                        }
                        if (vehicles.isEmpty()) {
                            Toast.makeText(requireContext(), "No hay vehículos. Agrega un vehículo antes de registrar combustible.", Toast.LENGTH_LONG).show();
                            dismiss();
                            return;
                        } else {
                            populateSpinnerFromVehicles();
                        }
                    })
                    .addOnFailureListener(e -> Toast.makeText(requireContext(), e.getMessage(), Toast.LENGTH_LONG).show());
        } else {
            Toast.makeText(requireContext(), "No autenticado.", Toast.LENGTH_LONG).show();
        }

        if (getArguments() != null) {
            etRecordId.setText(getArguments().getString("recordId"));
            desiredVehicleDisplay = getArguments().getString("vehicleDisplay");
            if (desiredVehicleDisplay != null && vehiclesLoaded) {
                selectVehicleByDisplay(desiredVehicleDisplay);
            }
            etDate.setText(getArguments().getString("date"));
            etLiters.setText(String.valueOf(getArguments().getDouble("liters")));
            etMileage.setText(String.valueOf(getArguments().getInt("mileage")));
            etPrice.setText(String.valueOf(getArguments().getDouble("price")));
            String fuel = getArguments().getString("fuelType");
            if (fuel != null) {
                if (fuel.equalsIgnoreCase(requireContext().getString(R.string.gasoline))) rbGasoline.setChecked(true);
                else if (fuel.equalsIgnoreCase(requireContext().getString(R.string.lpg))) rbLPG.setChecked(true);
                else if (fuel.equalsIgnoreCase(requireContext().getString(R.string.cng))) rbCNG.setChecked(true);
            }
        } else {
            etRecordId.setText(generateRecordId());
        }

        etDate.setOnClickListener(view -> showDatePicker(etDate));
        btnCancel.setOnClickListener(view -> dismiss());
        btnSave.setOnClickListener(view -> saveRecord());

        return v;
    }

    private String generateRecordId() {
        Random rnd = new Random();
        int num = 10000 + rnd.nextInt(90000);
        return String.valueOf(num);
    }

    private void showDatePicker(TextInputEditText target) {
        final Calendar c = Calendar.getInstance();
        int y = c.get(Calendar.YEAR);
        int m = c.get(Calendar.MONTH);
        int d = c.get(Calendar.DAY_OF_MONTH);
        DatePickerDialog dp = new DatePickerDialog(requireContext(), (DatePicker view, int year, int month, int dayOfMonth) -> {
            Calendar sel = Calendar.getInstance();
            sel.set(year, month, dayOfMonth);
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            target.setText(sdf.format(sel.getTime()));
        }, y, m, d);
        dp.show();
    }

    private void saveRecord() {
        String recordId = etRecordId.getText() != null ? etRecordId.getText().toString().trim() : "";
        int vehiclePos = spinnerVehicle.getSelectedItemPosition();
        if (vehicles == null || vehicles.isEmpty() || vehiclePos < 0) {
            Toast.makeText(requireContext(), R.string.error_empty_fields, Toast.LENGTH_SHORT).show();
            return;
        }
        Vehicle selVehicle = vehicles.get(vehiclePos);
        String dateStr = etDate.getText() != null ? etDate.getText().toString().trim() : "";
        String litersStr = etLiters.getText() != null ? etLiters.getText().toString().trim() : "";
        String mileageStr = etMileage.getText() != null ? etMileage.getText().toString().trim() : "";
        String priceStr = etPrice.getText() != null ? etPrice.getText().toString().trim() : "";
        String fuelType = rbGasoline.isChecked() ? requireContext().getString(R.string.gasoline) : rbLPG.isChecked() ? requireContext().getString(R.string.lpg) : requireContext().getString(R.string.cng);

        if (TextUtils.isEmpty(recordId) || TextUtils.isEmpty(dateStr) || TextUtils.isEmpty(litersStr) || TextUtils.isEmpty(mileageStr) || TextUtils.isEmpty(priceStr)) {
            Toast.makeText(requireContext(), R.string.error_empty_fields, Toast.LENGTH_SHORT).show();
            return;
        }

        double liters; int mileage; double price;
        try {
            liters = Double.parseDouble(litersStr);
            mileage = Integer.parseInt(mileageStr);
            price = Double.parseDouble(priceStr);
        } catch (NumberFormatException e) {
            Toast.makeText(requireContext(), R.string.error_occurred, Toast.LENGTH_SHORT).show();
            return;
        }

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        Date date;
        try { date = sdf.parse(dateStr); } catch (ParseException e) { Toast.makeText(requireContext(), R.string.error_occurred, Toast.LENGTH_SHORT).show(); return; }

        // validar kilometraje: debe ser mayor que el ultimo guardado para el vehiculo
        com.google.firebase.auth.FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(requireContext(), R.string.error_occurred, Toast.LENGTH_LONG).show();
            return;
        }
        String uid = currentUser.getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        String editingDocId = getArguments() != null ? getArguments().getString("id") : null;

        // Query the most recent records for this vehicle (by date) to determine last mileage
        db.collection("users").document(uid).collection("records")
                .whereEqualTo("vehicleDocId", selVehicle.getId())
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int lastMileage = -1;
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        // skip the current document if we're editing
                        if (editingDocId != null && editingDocId.equals(doc.getId())) continue;
                        Object mObj = doc.get("mileage");
                        if (mObj instanceof Number) {
                            int mv = ((Number) mObj).intValue();
                            if (mv > lastMileage) lastMileage = mv;
                        }
                    }

                    boolean hasPrevious = !queryDocumentSnapshots.isEmpty() && lastMileage >= 0;
                    if (hasPrevious) {
                        if (mileage <= lastMileage) {
                            Toast.makeText(requireContext(), getString(R.string.error_mileage_invalid) + " (último: " + lastMileage + ")", Toast.LENGTH_LONG).show();
                            return;
                        }
                    }

                    Record rec = new Record(uid, recordId, selVehicle.getId(), selVehicle.getVehicleId() + " - " + selVehicle.getLicensePlate(), date, liters, mileage, price, fuelType);
                    if (editingDocId != null) {
                        db.collection("users").document(uid).collection("records").document(editingDocId)
                                .set(rec)
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(requireContext(), R.string.success_saved, Toast.LENGTH_SHORT).show();
                                    dismiss();
                                })
                                .addOnFailureListener(e -> Toast.makeText(requireContext(), e.getMessage(), Toast.LENGTH_LONG).show());
                    } else {
                        db.collection("users").document(uid).collection("records")
                                .add(rec)
                                .addOnSuccessListener(documentReference -> {
                                    Toast.makeText(requireContext(), R.string.success_saved, Toast.LENGTH_SHORT).show();
                                    dismiss();
                                })
                                .addOnFailureListener(e -> Toast.makeText(requireContext(), e.getMessage(), Toast.LENGTH_LONG).show());
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(requireContext(), e.getMessage(), Toast.LENGTH_LONG).show());
    }

    private void populateSpinnerFromVehicles() {
        java.util.List<String> items = new java.util.ArrayList<>();
        for (Vehicle v : vehicles) items.add(v.getVehicleId() + " - " + v.getLicensePlate());
        android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, items);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerVehicle.setAdapter(adapter);
        vehiclesLoaded = true;
        btnSave.setEnabled(true);
        // if there is a desired display (from args), select it
        if (desiredVehicleDisplay != null) selectVehicleByDisplay(desiredVehicleDisplay);
    }

    private void selectVehicleByDisplay(String display) {
        if (vehicles == null) return;
        for (int i = 0; i < vehicles.size(); i++) {
            String d = vehicles.get(i).getVehicleId() + " - " + vehicles.get(i).getLicensePlate();
            if (d.equals(display)) {
                spinnerVehicle.setSelection(i);
                break;
            }
        }
    }
}
