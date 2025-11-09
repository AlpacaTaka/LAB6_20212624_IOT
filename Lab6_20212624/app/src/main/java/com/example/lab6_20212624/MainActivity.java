package com.example.lab6_20212624;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.FrameLayout;
import android.view.View;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.auth.FirebaseUser;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // Inicializar drawerLayout antes de usarlo para insets
        drawerLayout = findViewById(R.id.drawerLayout);

        ViewCompat.setOnApplyWindowInsetsListener(drawerLayout, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mAuth = FirebaseAuth.getInstance();

        navigationView = findViewById(R.id.navigationView);

        // toolbar drawer togle
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        // ajustar padding-top segun la altura de la status bar para q no se tape
        ViewCompat.setOnApplyWindowInsetsListener(toolbar, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(v.getPaddingLeft(), systemBars.top, v.getPaddingRight(), systemBars.bottom);
            return insets;
        });
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.app_name, R.string.app_name);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        // actualizar header del nav (nombre y correo)
        updateNavHeader();

        // Default fragment or content can be loaded here
        FrameLayout container = findViewById(R.id.fragmentContainer);

        // cargar fragment por defecto: vehiclesfragment
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragmentContainer, new VehiclesFragment())
                    .commit();
        }

        navigationView.setNavigationItemSelectedListener(this::onNavigationItemSelected);
    }

    private boolean onNavigationItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.nav_logout) {
            doLogout();
            return true;
        }

        // manejar fragments
        if (id == R.id.nav_vehicles) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragmentContainer, new VehiclesFragment())
                    .commit();
        } else if (id == R.id.nav_records) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragmentContainer, new RecordsFragment())
                    .commit();
        } else if (id == R.id.nav_summary) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragmentContainer, new SummaryFragment())
                    .commit();
        }

        drawerLayout.closeDrawers();
        return true;
    }

    private void doLogout() {
        mAuth.signOut();
        // also sign out from Google if needed
        startActivity(new Intent(MainActivity.this, LoginActivity.class));
        finish();
    }

    @Override
    protected void onStart() {
        super.onStart();
        updateNavHeader();
    }

    private void updateNavHeader() {
        if (navigationView == null) return;
        View header = navigationView.getHeaderView(0);
        if (header == null) return;
        TextView tvUserName = header.findViewById(R.id.tvUserName);
        TextView tvUserEmail = header.findViewById(R.id.tvUserEmail);

        com.google.firebase.auth.FirebaseUser current = FirebaseAuth.getInstance().getCurrentUser();
        if (current == null) {
            if (tvUserName != null) tvUserName.setText(R.string.app_name);
            if (tvUserEmail != null) tvUserEmail.setText("");
            return;
        }

        String uid = current.getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    if (doc != null && doc.exists()) {
                        String fullName = doc.getString("fullName");
                        String email = doc.getString("email");
                        if (tvUserName != null) tvUserName.setText(fullName != null ? fullName : (current.getDisplayName() != null ? current.getDisplayName() : getString(R.string.app_name)));
                        if (tvUserEmail != null) tvUserEmail.setText(email != null ? email : (current.getEmail() != null ? current.getEmail() : ""));
                    } else {
                        if (tvUserName != null) tvUserName.setText(current.getDisplayName() != null ? current.getDisplayName() : getString(R.string.app_name));
                        if (tvUserEmail != null) tvUserEmail.setText(current.getEmail() != null ? current.getEmail() : "");
                    }
                })
                .addOnFailureListener(e -> {
                    if (tvUserName != null) tvUserName.setText(current.getDisplayName() != null ? current.getDisplayName() : getString(R.string.app_name));
                    if (tvUserEmail != null) tvUserEmail.setText(current.getEmail() != null ? current.getEmail() : "");
                });
    }
}