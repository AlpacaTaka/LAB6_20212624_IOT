package com.example.lab6_20212624;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.DatePicker;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.lab6_20212624.model.Record;
import com.example.lab6_20212624.model.Vehicle;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class RecordsFragment extends Fragment implements RecordAdapter.OnItemActionListener {

    private RecyclerView rvRecords;
    private RecordAdapter adapter;
    private FloatingActionButton fabAddRecord;
    private View emptyState;

    private Spinner spinnerVehicle;
    private TextInputEditText etStartDate, etEndDate;
    private View btnApplyFilters, btnClearFilters;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private List<Vehicle> vehicles = new ArrayList<>();
    private ArrayAdapter<String> vehicleSpinnerAdapter;

    private ListenerRegistration recordsListener;

    public RecordsFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_records, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        rvRecords = view.findViewById(R.id.rvRecords);
        fabAddRecord = view.findViewById(R.id.fabAddRecord);
        emptyState = view.findViewById(R.id.emptyState);

        spinnerVehicle = view.findViewById(R.id.spinnerVehicle);
        etStartDate = view.findViewById(R.id.etStartDate);
        etEndDate = view.findViewById(R.id.etEndDate);
        btnApplyFilters = view.findViewById(R.id.btnApplyFilters);
        btnClearFilters = view.findViewById(R.id.btnClearFilters);

        adapter = new RecordAdapter(requireContext(), this);
        rvRecords.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvRecords.setAdapter(adapter);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        vehicleSpinnerAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, new ArrayList<>());
        vehicleSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerVehicle.setAdapter(vehicleSpinnerAdapter);

        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            loadVehicles(user.getUid());
            // escuchar registros iniciales (sin filtros) -> escucha en tiempo real
            listenRecords(user.getUid(), null, null, null);
        }

        fabAddRecord.setOnClickListener(v -> showRecordDialog(null));

        etStartDate.setOnClickListener(v -> showDatePicker(etStartDate));
        etEndDate.setOnClickListener(v -> showDatePicker(etEndDate));

        btnApplyFilters.setOnClickListener(v -> applyFilters());
        btnClearFilters.setOnClickListener(v -> {
            etStartDate.setText("");
            etEndDate.setText("");
            spinnerVehicle.setSelection(0);
            FirebaseUser u = mAuth.getCurrentUser();
            if (u != null) listenRecords(u.getUid(), null, null, null);
        });
    }

    private void loadVehicles(String uid) {
        db.collection("users").document(uid).collection("vehicles").get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    vehicles.clear();
                    vehicleSpinnerAdapter.clear();
                    vehicleSpinnerAdapter.add("Todos");
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Vehicle v = doc.toObject(Vehicle.class);
                        v.setId(doc.getId());
                        vehicles.add(v);
                        vehicleSpinnerAdapter.add(v.getVehicleId() + " - " + v.getLicensePlate());
                    }
                    vehicleSpinnerAdapter.notifyDataSetChanged();
                }).addOnFailureListener(e -> Toast.makeText(requireContext(), e.getMessage(), Toast.LENGTH_LONG).show());
    }

    private void listenRecords(String uid, Date start, Date end) {
        if (recordsListener != null) recordsListener.remove();

        Query q = db.collection("users").document(uid).collection("records");
        // aplicar filtro por vehiculo si corresponde (esta version es para compatibilidad)
        // no modificar lomas de aqui si no sabes, se usa la sobrecarga con vehicleDocId
        q = q.orderBy("date", Query.Direction.DESCENDING);

        recordsListener = q.addSnapshotListener((value, error) -> {
            if (error != null) {
                Toast.makeText(requireContext(), error.getMessage(), Toast.LENGTH_LONG).show();
                return;
            }

            List<Record> list = new ArrayList<>();
            if (value != null) {
                for (QueryDocumentSnapshot doc : value) {
                    Record r = doc.toObject(Record.class);
                    r.setId(doc.getId());
                    list.add(r);
                }
            }

            adapter.setItems(list);
            emptyState.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
        });
    }

    // nuevo metodo: listenRecords con filtro explicito vehicleDocId
    private void listenRecords(String uid, Date start, Date end, String vehicleDocId) {
        if (recordsListener != null) recordsListener.remove();

        Query q = db.collection("users").document(uid).collection("records");

        if (vehicleDocId != null) q = q.whereEqualTo("vehicleDocId", vehicleDocId);
        if (start != null) q = q.whereGreaterThanOrEqualTo("date", start);
        if (end != null) q = q.whereLessThanOrEqualTo("date", end);

        q = q.orderBy("date", Query.Direction.DESCENDING);

        recordsListener = q.addSnapshotListener((value, error) -> {
            if (error != null) {
                String msg = error.getMessage() != null ? error.getMessage() : "";
                // Si Firestore indica que se requiere un índice compuesto, aplicamos un fallback local
                if (msg.toLowerCase().contains("index") || msg.toLowerCase().contains("requires an index")) {
                    // índice faltante: aplicamos fallback local (no mostrar toast de depuración)
                    if (vehicleDocId != null) {
                        // Traer todos los registros de ese vehículo y filtrar por fecha en el cliente
                        db.collection("users").document(uid).collection("records")
                                .whereEqualTo("vehicleDocId", vehicleDocId)
                                .get()
                                .addOnSuccessListener(qs -> {
                                    List<Record> list = new ArrayList<>();
                                    for (QueryDocumentSnapshot doc : qs) {
                                        Record r = doc.toObject(Record.class);
                                        r.setId(doc.getId());
                                        Date d = r.getDate();
                                        boolean ok = true;
                                        if (start != null && d != null && d.before(start)) ok = false;
                                        if (end != null && d != null && d.after(end)) ok = false;
                                        if (ok) list.add(r);
                                    }
                                    adapter.setItems(list);
                                    emptyState.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
                                })
                                .addOnFailureListener(e -> Toast.makeText(requireContext(), e.getMessage(), Toast.LENGTH_LONG).show());
                    } else {
                        // No vehicleDocId: no es trivial hacer fallback eficiente. Mostrar diálogo con enlace para crear índice.
                        String url = null;
                        int pos = msg.indexOf("https://");
                        if (pos >= 0) url = msg.substring(pos);
                        android.app.AlertDialog.Builder b = new android.app.AlertDialog.Builder(requireContext());
                        b.setTitle("Índice requerido");
                        b.setMessage("La consulta requiere un índice compuesto en Firestore. Puedes crear el índice en la consola de Firebase.");
                        if (url != null) {
                            final String link = url;
                            b.setPositiveButton("Abrir enlace", (dialog, which) -> {
                                try {
                                    Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(link));
                                    startActivity(i);
                                } catch (Exception ex) { Toast.makeText(requireContext(), "No se pudo abrir el enlace", Toast.LENGTH_SHORT).show(); }
                            });
                            b.setNegativeButton("Cerrar", null);
                        } else {
                            b.setPositiveButton("Cerrar", null);
                        }
                        b.show();
                    }
                    return;
                }
                // otro error
                Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show();
                return;
            }

            List<Record> list = new ArrayList<>();
            if (value != null) {
                for (QueryDocumentSnapshot doc : value) {
                    Record r = doc.toObject(Record.class);
                    r.setId(doc.getId());
                    list.add(r);
                }
            }

            adapter.setItems(list);
            emptyState.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
        });
    }

    private void showRecordDialog(Record record) {
        RecordDialogFragment dialog = RecordDialogFragment.newInstance(record, vehicles);
        dialog.setTargetFragment(this, 0);
        dialog.show(getParentFragmentManager(), "record_dialog");
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

    private void applyFilters() {
        String startStr = etStartDate.getText() != null ? etStartDate.getText().toString().trim() : "";
        String endStr = etEndDate.getText() != null ? etEndDate.getText().toString().trim() : "";
        Date start = null, end = null;
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        try {
            if (!startStr.isEmpty()) start = sdf.parse(startStr);
            if (!endStr.isEmpty()) end = sdf.parse(endStr);
        } catch (Exception e) {
            Toast.makeText(requireContext(), R.string.error_occurred, Toast.LENGTH_SHORT).show();
            return;
        }

        String sel = (String) spinnerVehicle.getSelectedItem();
        String vehicleDocId = null;
        if (!sel.equals("Todos")) {
            // find vehicle by display
            for (Vehicle v : vehicles) {
                String disp = v.getVehicleId() + " - " + v.getLicensePlate();
                if (disp.equals(sel)) {
                    vehicleDocId = v.getId();
                    break;
                }
            }
        }

        // adjust end to end of day if provided (include the full day)
        if (end != null) {
            Calendar c = Calendar.getInstance();
            c.setTime(end);
            c.set(Calendar.HOUR_OF_DAY, 23);
            c.set(Calendar.MINUTE, 59);
            c.set(Calendar.SECOND, 59);
            c.set(Calendar.MILLISECOND, 999);
            end = c.getTime();
        }

        FirebaseUser u = mAuth.getCurrentUser();
        if (u != null) {
            if (vehicleDocId != null) {
                listenRecords(u.getUid(), start, end, vehicleDocId);
            } else {
                listenRecords(u.getUid(), start, end, null);
            }
        }
    }

    @Override
    public void onEdit(Record record) {
        showRecordDialog(record);
    }

    @Override
    public void onDelete(Record record) {
        FirebaseUser u = mAuth.getCurrentUser();
        if (u != null && record.getId() != null) {
            db.collection("users").document(u.getUid()).collection("records").document(record.getId())
                    .delete()
                    .addOnSuccessListener(aVoid -> Toast.makeText(requireContext(), R.string.success_deleted, Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e -> Toast.makeText(requireContext(), e.getMessage(), Toast.LENGTH_LONG).show());
        }
    }
}
