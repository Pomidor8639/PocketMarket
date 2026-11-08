package com.pocketmarket.client;

import android.accounts.AccountManager;
import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.pocketmarket.client.api.PocketMarketApi;
import com.pocketmarket.client.utils.AuthManager;
import com.pocketmarket.client.utils.FontUtils;

public class AuthActivity extends AppCompatActivity {

    private static final int RC_GOOGLE_PICKER = 1001;
    private String selectedGoogleEmail = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);
        FontUtils.applyMinecraftFont(findViewById(android.R.id.content));

        View btnGoogleAuth = findViewById(R.id.btnGoogleAuth);
        View btnContinueGuest = findViewById(R.id.btnContinueGuest);

        btnGoogleAuth.setOnClickListener(v -> launchGooglePickerOrDialog());
        btnContinueGuest.setOnClickListener(v -> showGuestWarningDialog());
    }

    private void launchGooglePickerOrDialog() {
        try {
            Intent intent = AccountManager.newChooseAccountIntent(
                    null,
                    null,
                    new String[]{"com.google"},
                    null,
                    null,
                    null,
                    null
            );
            startActivityForResult(intent, RC_GOOGLE_PICKER);
        } catch (Exception e) {
            // Fallback directly to dialog if AccountManager intent is unavailable
            showAccountSetupDialog("");
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_GOOGLE_PICKER) {
            String email = "";
            if (resultCode == Activity.RESULT_OK && data != null) {
                email = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
            }
            showAccountSetupDialog(email != null ? email : "");
        }
    }

    private void showAccountSetupDialog(String prefilledEmail) {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_account_setup);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dialog.getWindow().setLayout(
                    (int) (getResources().getDisplayMetrics().widthPixels * 0.90),
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
        }
        FontUtils.applyMinecraftFont(dialog.findViewById(android.R.id.content));

        EditText etEmail = dialog.findViewById(R.id.etAuthEmail);
        EditText etNickname = dialog.findViewById(R.id.etAuthNickname);
        EditText etPassword = dialog.findViewById(R.id.etAuthPassword);
        Button btnCancel = dialog.findViewById(R.id.btnCancelAuth);
        Button btnSubmit = dialog.findViewById(R.id.btnSubmitAuth);

        if (prefilledEmail != null && !prefilledEmail.isEmpty()) {
            etEmail.setText(prefilledEmail);
            String candidateNick = prefilledEmail.split("@")[0];
            etNickname.setText(candidateNick);
        }

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSubmit.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String nickname = etNickname.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (!email.contains("@")) {
                Toast.makeText(AuthActivity.this, "Введите корректный адрес почты", Toast.LENGTH_SHORT).show();
                return;
            }
            if (nickname.length() < 2) {
                Toast.makeText(AuthActivity.this, "Никнейм должен быть не короче 2 символов", Toast.LENGTH_SHORT).show();
                return;
            }
            if (password.length() < 4) {
                Toast.makeText(AuthActivity.this, "Пароль должен быть не короче 4 символов", Toast.LENGTH_SHORT).show();
                return;
            }

            btnSubmit.setEnabled(false);
            btnSubmit.setText("ВХОД...");

            PocketMarketApi.getInstance().accountAuth(email, nickname, password, new PocketMarketApi.ApiCallback<PocketMarketApi.AuthResult>() {
                @Override
                public void onSuccess(PocketMarketApi.AuthResult result) {
                    dialog.dismiss();
                    AuthManager.getInstance(AuthActivity.this).saveSession(
                            result.token,
                            result.id,
                            result.email,
                            result.nickname
                    );
                    Toast.makeText(AuthActivity.this, "Добро пожаловать, " + result.nickname + "!", Toast.LENGTH_SHORT).show();
                    startMainActivity();
                }

                @Override
                public void onError(Exception e) {
                    btnSubmit.setEnabled(true);
                    btnSubmit.setText("ВОЙТИ");
                    Toast.makeText(AuthActivity.this, "Ошибка: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            });
        });

        dialog.show();
    }

    private void showGuestWarningDialog() {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_guest_warning);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dialog.getWindow().setLayout(
                    (int) (getResources().getDisplayMetrics().widthPixels * 0.90),
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
        }
        FontUtils.applyMinecraftFont(dialog.findViewById(android.R.id.content));

        Button btnCancel = dialog.findViewById(R.id.btnCancelGuest);
        Button btnConfirm = dialog.findViewById(R.id.btnConfirmGuest);

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnConfirm.setOnClickListener(v -> {
            dialog.dismiss();
            AuthManager.getInstance(AuthActivity.this).setGuestMode(true);
            Toast.makeText(AuthActivity.this, "Вход выполнен в гостевом режиме", Toast.LENGTH_SHORT).show();
            startMainActivity();
        });

        dialog.show();
    }

    private void startMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
}
