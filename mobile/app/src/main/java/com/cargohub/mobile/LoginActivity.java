package com.cargohub.mobile;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.inputmethod.EditorInfo;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.cargohub.mobile.data.AuthRepository;
import com.cargohub.mobile.data.model.LoginResponse;
import com.cargohub.mobile.databinding.ActivityLoginBinding;
import com.cargohub.mobile.session.SessionManager;

public class LoginActivity extends AppCompatActivity {

    private static final String DRIVER_ROLE = "CONDUCTOR";

    private ActivityLoginBinding binding;
    private final AuthRepository authRepository = new AuthRepository();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (SessionManager.hasSession()) {
            openMainAndFinish();
            return;
        }

        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.loginButton.setOnClickListener(v -> submitLogin());
        binding.passwordInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                submitLogin();
                return true;
            }
            return false;
        });
    }

    private void submitLogin() {
        clearErrors();

        String email = stringValue(binding.emailInput.getText());
        String password = stringValue(binding.passwordInput.getText());

        if (!validate(email, password)) {
            return;
        }

        setLoading(true);
        authRepository.login(email, password, new AuthRepository.LoginCallback() {
            @Override
            public void onSuccess(@NonNull LoginResponse loginResponse) {
                runOnUiThread(() -> {
                    setLoading(false);
                    String token = loginResponse.getToken();
                    String role = loginResponse.getRol();
                    if (role == null || !DRIVER_ROLE.equalsIgnoreCase(role.trim())) {
                        SessionManager.clearSession();
                        showFormError(getString(R.string.login_error_role_not_allowed));
                        return;
                    }
                    if (token == null || token.trim().isEmpty()) {
                        showFormError(getString(R.string.login_error_missing_token));
                        return;
                    }
                    SessionManager.saveAccessToken(token);
                    Long conductorId = loginResponse.getConductorId();
                    if (conductorId == null) {
                        conductorId = loginResponse.getId();
                    }
                    if (conductorId != null && conductorId > 0) {
                        SessionManager.saveConductorId(conductorId);
                    }
                    openMainAndFinish();
                });
            }

            @Override
            public void onError(@NonNull String message) {
                runOnUiThread(() -> {
                    setLoading(false);
                    showFormError(message);
                });
            }
        });
    }

    private boolean validate(@NonNull String email, @NonNull String password) {
        boolean valid = true;

        if (email.isEmpty()) {
            binding.emailInputLayout.setError(getString(R.string.login_error_required_email));
            valid = false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.emailInputLayout.setError(getString(R.string.login_error_invalid_email));
            valid = false;
        }

        if (password.isEmpty()) {
            binding.passwordInputLayout.setError(getString(R.string.login_error_required_password));
            valid = false;
        }

        return valid;
    }

    private void clearErrors() {
        binding.emailInputLayout.setError(null);
        binding.passwordInputLayout.setError(null);
        binding.loginErrorText.setVisibility(android.view.View.GONE);
    }

    private void showFormError(@NonNull String message) {
        binding.loginErrorText.setText(message);
        binding.loginErrorText.setVisibility(android.view.View.VISIBLE);
    }

    private void setLoading(boolean loading) {
        binding.loginButton.setEnabled(!loading);
        binding.loginButton.setText(loading ? R.string.login_cta_loading : R.string.login_cta);
        binding.emailInput.setEnabled(!loading);
        binding.passwordInput.setEnabled(!loading);
        binding.loginProgress.setVisibility(loading ? android.view.View.VISIBLE : android.view.View.GONE);
        binding.loginLoadingText.setVisibility(loading ? android.view.View.VISIBLE : android.view.View.GONE);
    }

    @NonNull
    private String stringValue(CharSequence value) {
        return value == null ? "" : value.toString().trim();
    }

    private void openMainAndFinish() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
