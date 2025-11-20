package com.mittimitra;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class ProfileActivity extends AppCompatActivity {

    private EditText etName, etPhone, etLocation;
    private Button btnSave, btnChangePass;
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile); // Make sure you create this layout

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        String uid = auth.getUid();

        etName = findViewById(R.id.et_profile_name);
        etPhone = findViewById(R.id.et_profile_phone);
        etLocation = findViewById(R.id.et_profile_location);
        btnSave = findViewById(R.id.btn_save_profile);
        btnChangePass = findViewById(R.id.btn_change_password);

        // Load Data
        if (uid != null) {
            db.collection("farmers").document(uid).get().addOnSuccessListener(doc -> {
                if (doc.exists()) {
                    etName.setText(doc.getString("firstName"));
                    etPhone.setText(doc.getString("phone"));
                    etLocation.setText(doc.getString("place"));
                }
            });
        }

        // Save Data
        btnSave.setOnClickListener(v -> {
            Map<String, Object> updates = new HashMap<>();
            updates.put("firstName", etName.getText().toString());
            updates.put("place", etLocation.getText().toString());

            db.collection("farmers").document(uid).update(updates)
                    .addOnSuccessListener(a -> Toast.makeText(this, "Updated", Toast.LENGTH_SHORT).show());
        });

        // Change Password Logic
        FirebaseUser user = auth.getCurrentUser();
        if (user != null && user.getProviderData().size() > 0) {
            String provider = user.getProviderData().get(1).getProviderId();
            // Hide button if using Phone or Google (they don't have app-passwords)
            if (provider.equals("phone") || provider.equals("google.com")) {
                btnChangePass.setVisibility(View.GONE);
            }
        }

        btnChangePass.setOnClickListener(v -> {
            if (user.getEmail() != null) {
                auth.sendPasswordResetEmail(user.getEmail())
                        .addOnSuccessListener(a -> Toast.makeText(this, "Reset email sent", Toast.LENGTH_SHORT).show());
            } else {
                Toast.makeText(this, "Not available for this account type", Toast.LENGTH_SHORT).show();
            }
        });
    }
}