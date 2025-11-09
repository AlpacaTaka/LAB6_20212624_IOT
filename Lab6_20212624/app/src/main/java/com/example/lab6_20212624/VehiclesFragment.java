package com.example.lab6_20212624;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.DatePicker;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.lab6_20212624.model.Vehicle;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class VehiclesFragment extends Fragment implements VehicleAdapter.OnItemActionListener {

    private RecyclerView rvVehicles;
    private VehicleAdapter adapter;
    private FloatingActionButton fabAddVehicle;
    private View emptyState;

    private FirebaseFirestore db;
    private CollectionReference vehiclesRef;
    private FirebaseAuth mAuth;

    public VehiclesFragment() {
        // constructor publico vacio requerido
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_vehicles, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rvVehicles = view.findViewById(R.id.rvVehicles);
        fabAddVehicle = view.findViewById(R.id.fabAddVehicle);
        emptyState = view.findViewById(R.id.emptyState);

        adapter = new VehicleAdapter(requireContext(), this);
        rvVehicles.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvVehicles.setAdapter(adapter);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            vehiclesRef = db.collection("users").document(user.getUid()).collection("vehicles");
            listenVehicles();
        }

        fabAddVehicle.setOnClickListener(v -> showVehicleDialog(null));
    }

    private void listenVehicles() {
        vehiclesRef.addSnapshotListener((value, error) -> {
            if (error != null) {
                Toast.makeText(requireContext(), error.getMessage(), Toast.LENGTH_LONG).show();
                return;
            }

            List<Vehicle> list = new ArrayList<>();
            if (value != null) {
                for (QueryDocumentSnapshot doc : value) {
                    Vehicle veh = doc.toObject(Vehicle.class);
                    veh.setId(doc.getId());
                    list.add(veh);
                }
            }

            adapter.setItems(list);
            emptyState.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
        });
    }

    private void showVehicleDialog(Vehicle vehicle) {
        VehicleDialogFragment dialog = VehicleDialogFragment.newInstance(vehicle);
        dialog.setTargetFragment(this, 0);
        dialog.show(getParentFragmentManager(), "vehicle_dialog");
    }

    @Override
    public void onEdit(Vehicle vehicle) {
        showVehicleDialog(vehicle);
    }

    @Override
    public void onDelete(Vehicle vehicle) {
        if (vehicle.getId() != null) {
            vehiclesRef.document(vehicle.getId()).delete()
                    .addOnSuccessListener(aVoid -> Toast.makeText(requireContext(), R.string.success_deleted, Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e -> Toast.makeText(requireContext(), e.getMessage(), Toast.LENGTH_LONG).show());
        }
    }

    @Override
    public void onGenerateQR(Vehicle vehicle) {
        if (vehicle == null || vehicle.getId() == null) {
            Toast.makeText(requireContext(), R.string.error_occurred, Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            Toast.makeText(requireContext(), R.string.error_occurred, Toast.LENGTH_SHORT).show();
            return;
        }

        // obtener ultimo kilometraje para este vehiculo
        db.collection("users").document(user.getUid()).collection("records")
                .whereEqualTo("vehicleDocId", vehicle.getId())
                .get()
                .addOnSuccessListener(qs -> {
                    int lastMileage = -1;
                    Date lastReviewDate = vehicle.getTechnicalReviewDate();
                    for (QueryDocumentSnapshot doc : qs) {
                        Object mObj = doc.get("mileage");
                        if (mObj instanceof Number) {
                            int mv = ((Number) mObj).intValue();
                            if (mv > lastMileage) lastMileage = mv;
                        }
                        // tambien se podria tomar la fecha mas reciente si se desea
                    }

                    // preparar intent
                    android.content.Intent i = new android.content.Intent(requireContext(), QRGeneratorActivity.class);
                    i.putExtra(QRGeneratorActivity.EXTRA_PLATE, vehicle.getLicensePlate());
                    i.putExtra(QRGeneratorActivity.EXTRA_LAST_MILEAGE, lastMileage);
                    String lastReviewStr = lastReviewDate != null ? new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(lastReviewDate) : "";
                    i.putExtra(QRGeneratorActivity.EXTRA_LAST_REVIEW, lastReviewStr);
                    startActivity(i);
                })
                .addOnFailureListener(e -> Toast.makeText(requireContext(), e.getMessage(), Toast.LENGTH_LONG).show());
    }
}
