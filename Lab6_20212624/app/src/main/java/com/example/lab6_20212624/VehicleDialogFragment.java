package com.example.lab6_20212624;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.DatePicker;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.example.lab6_20212624.model.Vehicle;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class VehicleDialogFragment extends DialogFragment {

    private TextInputEditText etVehicleId, etLicensePlate, etBrandModel, etYear, etTechnicalReviewDate;
    private MaterialButton btnCancel, btnSave;
    private Vehicle vehicle;

    public static VehicleDialogFragment newInstance(Vehicle vehicle) {
        VehicleDialogFragment f = new VehicleDialogFragment();
        if (vehicle != null) {
            Bundle args = new Bundle();
            args.putString("id", vehicle.getId());
            args.putString("vehicleId", vehicle.getVehicleId());
            args.putString("licensePlate", vehicle.getLicensePlate());
            args.putString("brandModel", vehicle.getBrandModel());
            args.putInt("year", vehicle.getYear());
            // formatear fecha si existe (coment en espaniol con faltas)
            if (vehicle.getTechnicalReviewDate() != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                args.putString("technicalReviewDate", sdf.format(vehicle.getTechnicalReviewDate()));
            }
            f.setArguments(args);
        }
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
        View v = inflater.inflate(R.layout.dialog_vehicle, container, false);
        etVehicleId = v.findViewById(R.id.etVehicleId);
        etLicensePlate = v.findViewById(R.id.etLicensePlate);
        etBrandModel = v.findViewById(R.id.etBrandModel);
        etYear = v.findViewById(R.id.etYear);
        etTechnicalReviewDate = v.findViewById(R.id.etTechnicalReviewDate);
        btnCancel = v.findViewById(R.id.btnCancel);
        btnSave = v.findViewById(R.id.btnSave);

        if (getArguments() != null) {
            etVehicleId.setText(getArguments().getString("vehicleId"));
            etLicensePlate.setText(getArguments().getString("licensePlate"));
            etBrandModel.setText(getArguments().getString("brandModel"));
            etYear.setText(String.valueOf(getArguments().getInt("year")));
            String dateStr = getArguments().getString("technicalReviewDate");
            if (dateStr != null) {
                etTechnicalReviewDate.setText(dateStr);
            }
        }

        etTechnicalReviewDate.setOnClickListener(view -> showDatePicker());

        btnCancel.setOnClickListener(view -> dismiss());

        btnSave.setOnClickListener(view -> saveVehicle());

        return v;
    }

    private void showDatePicker() {
        final Calendar c = Calendar.getInstance();
        int y = c.get(Calendar.YEAR);
        int m = c.get(Calendar.MONTH);
        int d = c.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog dp = new DatePickerDialog(requireContext(), (DatePicker view, int year, int month, int dayOfMonth) -> {
            Calendar sel = Calendar.getInstance();
            sel.set(year, month, dayOfMonth);
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            etTechnicalReviewDate.setText(sdf.format(sel.getTime()));
        }, y, m, d);
        dp.show();
    }

    private void saveVehicle() {
        String vehicleId = etVehicleId.getText() != null ? etVehicleId.getText().toString().trim() : "";
        String license = etLicensePlate.getText() != null ? etLicensePlate.getText().toString().trim() : "";
        String brand = etBrandModel.getText() != null ? etBrandModel.getText().toString().trim() : "";
        String yearStr = etYear.getText() != null ? etYear.getText().toString().trim() : "";
        String reviewDateStr = etTechnicalReviewDate.getText() != null ? etTechnicalReviewDate.getText().toString().trim() : "";

        if (TextUtils.isEmpty(vehicleId) || TextUtils.isEmpty(license) || TextUtils.isEmpty(brand) || TextUtils.isEmpty(yearStr) || TextUtils.isEmpty(reviewDateStr)) {
            Toast.makeText(requireContext(), R.string.error_empty_fields, Toast.LENGTH_SHORT).show();
            return;
        }

        int year;
        try {
            year = Integer.parseInt(yearStr);
        } catch (NumberFormatException e) {
            Toast.makeText(requireContext(), R.string.error_occurred, Toast.LENGTH_SHORT).show();
            return;
        }

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        Date reviewDate = null;
        try {
            reviewDate = sdf.parse(reviewDateStr);
        } catch (ParseException e) {
            Toast.makeText(requireContext(), R.string.error_occurred, Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        Vehicle veh = new Vehicle(uid, vehicleId, license, brand, year, reviewDate);

        String docId = getArguments() != null ? getArguments().getString("id") : null;
        if (docId != null) {
            db.collection("users").document(uid).collection("vehicles").document(docId)
                    .set(veh)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(requireContext(), R.string.success_saved, Toast.LENGTH_SHORT).show();
                        dismiss();
                    })
                    .addOnFailureListener(e -> Toast.makeText(requireContext(), e.getMessage(), Toast.LENGTH_LONG).show());
        } else {
            db.collection("users").document(uid).collection("vehicles")
                    .add(veh)
                    .addOnSuccessListener(documentReference -> {
                        Toast.makeText(requireContext(), R.string.success_saved, Toast.LENGTH_SHORT).show();
                        dismiss();
                    })
                    .addOnFailureListener(e -> Toast.makeText(requireContext(), e.getMessage(), Toast.LENGTH_LONG).show());
        }
    }
}
