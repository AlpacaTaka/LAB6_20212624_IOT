package com.example.lab6_20212624;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class SummaryFragment extends Fragment {

    private BarChart barChart;
    private PieChart pieChart;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_summary, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        barChart = view.findViewById(R.id.barChart);
        pieChart = view.findViewById(R.id.pieChart);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            Toast.makeText(requireContext(), "Usuario no autenticado", Toast.LENGTH_LONG).show();
            return;
        }

        loadAndRender(user.getUid());
    }

    private void loadAndRender(String uid) {
        db.collection("users").document(uid).collection("records")
                .get()
                .addOnSuccessListener(qs -> {
                    List<Map<String, Object>> docs = new ArrayList<>();
                    for (com.google.firebase.firestore.QueryDocumentSnapshot doc : qs) {
                        docs.add(doc.getData());
                    }
                    renderCharts(docs);
                })
                .addOnFailureListener(e -> Toast.makeText(requireContext(), e.getMessage(), Toast.LENGTH_LONG).show());
    }

    private void renderCharts(List<Map<String, Object>> records) {
        // calcular litros por mes (ultimos 12 meses)
        Map<String, Double> litersByMonth = new HashMap<>();
        Map<String, Double> litersByFuel = new HashMap<>();

        SimpleDateFormat monthFmt = new SimpleDateFormat("yyyy-MM", Locale.getDefault());
        Calendar cal = Calendar.getInstance();
        // inicializar llaves de los ultimos 12 meses
        for (int i = 11; i >= 0; i--) {
            Calendar c = (Calendar) cal.clone();
            c.add(Calendar.MONTH, -i);
            String key = monthFmt.format(c.getTime());
            litersByMonth.put(key, 0.0);
        }

        for (Map<String, Object> doc : records) {
            Object dateObj = doc.get("date");
            Date date = null;
            if (dateObj instanceof com.google.firebase.Timestamp) {
                date = ((com.google.firebase.Timestamp) dateObj).toDate();
            } else if (dateObj instanceof Date) {
                date = (Date) dateObj;
            }

            double liters = 0;
            Object l = doc.get("liters");
            if (l instanceof Number) liters = ((Number) l).doubleValue();

            String fuelType = doc.get("fuelType") != null ? doc.get("fuelType").toString() : "Desconocido";

            if (date != null) {
                String key = monthFmt.format(date);
                if (litersByMonth.containsKey(key)) litersByMonth.put(key, litersByMonth.get(key) + liters);
            }

            litersByFuel.put(fuelType, litersByFuel.getOrDefault(fuelType, 0.0) + liters);
        }

        // Bar chart entries
        List<BarEntry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        int idx = 0;
        for (String key : litersByMonth.keySet()) {
            entries.add(new BarEntry(idx, litersByMonth.get(key).floatValue()));
            labels.add(key);
            idx++;
        }

        BarDataSet set = new BarDataSet(entries, "Litros por mes");
        set.setColor(Color.parseColor("#00BCD4"));
        BarData data = new BarData(set);
        data.setBarWidth(0.9f);
        barChart.setData(data);
        barChart.getDescription().setEnabled(false);
        barChart.setFitBars(true);
        XAxis xAxis = barChart.getXAxis();
        xAxis.setGranularity(1f);
        xAxis.setValueFormatter(new com.github.mikephil.charting.formatter.IndexAxisValueFormatter(labels) {
            @Override
            public String getFormattedValue(float value) {
                int i = (int) value;
                if (i >= 0 && i < labels.size()) return labels.get(i).substring(5);
                return "";
            }
        });
        barChart.invalidate();

        // Pie chart entries
        List<PieEntry> pieEntries = new ArrayList<>();
        for (Map.Entry<String, Double> e : litersByFuel.entrySet()) {
            pieEntries.add(new PieEntry(e.getValue().floatValue(), e.getKey()));
        }
        PieDataSet pieSet = new PieDataSet(pieEntries, "Consumo por tipo");
        pieSet.setColors(new int[]{Color.parseColor("#FF6B6B"), Color.parseColor("#4ECDC4"), Color.parseColor("#95E1D3")});
        PieData pieData = new PieData(pieSet);
        pieChart.setData(pieData);
        pieChart.getDescription().setEnabled(false);
        pieChart.invalidate();
    }
}
