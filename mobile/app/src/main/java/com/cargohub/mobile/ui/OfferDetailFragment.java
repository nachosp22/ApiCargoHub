package com.cargohub.mobile.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.cargohub.mobile.R;
import com.cargohub.mobile.data.PorteRepository;
import com.cargohub.mobile.data.RepositoryResult;
import com.cargohub.mobile.data.model.EstadoPorte;
import com.cargohub.mobile.data.model.Porte;
import com.cargohub.mobile.session.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;

public class OfferDetailFragment extends Fragment {

    private static final String ARG_PORTE_ID = "porte_id";

    private final PorteRepository porteRepository = new PorteRepository();

    private SwipeRefreshLayout swipeRefreshLayout;
    private LinearLayout loadingContainer;
    private LinearLayout emptyContainer;
    private LinearLayout errorContainer;
    private LinearLayout contentContainer;
    private TextView titleText;
    private TextView stateText;
    private TextView scheduleText;
    private TextView cargoText;
    private TextView priceText;
    private TextView helperText;
    private TextView emptyMessage;
    private TextView errorMessage;
    private MaterialButton acceptButton;
    private MaterialButton rejectButton;

    private long porteId;
    private Porte currentPorte;

    public static OfferDetailFragment newInstance(long porteId) {
        OfferDetailFragment fragment = new OfferDetailFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_PORTE_ID, porteId);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_offer_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        porteId = requireArguments().getLong(ARG_PORTE_ID);
        swipeRefreshLayout = view.findViewById(R.id.offerDetailSwipeRefresh);
        loadingContainer = view.findViewById(R.id.offerDetailLoadingContainer);
        emptyContainer = view.findViewById(R.id.offerDetailEmptyContainer);
        errorContainer = view.findViewById(R.id.offerDetailErrorContainer);
        contentContainer = view.findViewById(R.id.offerDetailContentContainer);
        titleText = view.findViewById(R.id.offerDetailTitleText);
        stateText = view.findViewById(R.id.offerDetailStateText);
        scheduleText = view.findViewById(R.id.offerDetailScheduleText);
        cargoText = view.findViewById(R.id.offerDetailCargoText);
        priceText = view.findViewById(R.id.offerDetailPriceText);
        helperText = view.findViewById(R.id.offerDetailHelperText);
        emptyMessage = view.findViewById(R.id.offerDetailEmptyText);
        errorMessage = view.findViewById(R.id.offerDetailErrorMessage);
        acceptButton = view.findViewById(R.id.offerAcceptButton);
        rejectButton = view.findViewById(R.id.offerRejectButton);
        MaterialButton retryButton = view.findViewById(R.id.offerDetailRetryButton);

        swipeRefreshLayout.setColorSchemeResources(R.color.ch_accent_primary);
        swipeRefreshLayout.setOnRefreshListener(this::loadOffer);
        acceptButton.setOnClickListener(v -> acceptOffer());
        rejectButton.setOnClickListener(v -> rejectOffer());
        retryButton.setOnClickListener(v -> loadOffer());
        loadOffer();
    }

    private void loadOffer() {
        showLoading();
        porteRepository.getPorteDetail(porteId, result -> {
            if (!isAdded()) {
                return;
            }
            swipeRefreshLayout.setRefreshing(false);
            if (!result.isSuccessful()) {
                showError(result.getMessage());
                return;
            }
            if (result.getData() == null) {
                showEmpty(getString(R.string.offer_detail_state_empty_body));
                return;
            }
            currentPorte = result.getData();
            renderOffer(currentPorte);
            showContent();
        });
    }

    private void renderOffer(@NonNull Porte porte) {
        titleText.setText(UiFormatters.formatPorteTitle(porte));
        stateText.setText(UiFormatters.formatPorteState(porte));
        scheduleText.setText(UiFormatters.formatPorteSchedule(porte));
        cargoText.setText(UiFormatters.formatPorteCargo(porte));
        priceText.setText(UiFormatters.formatPortePrice(porte));
        applyStateColor(stateText, porte.getEstadoPorte());
        boolean actionable = porteRepository.canAcceptOffer(porte);
        acceptButton.setEnabled(actionable);
        rejectButton.setEnabled(actionable && porteRepository.supportsOfferRejection());
        helperText.setText(actionable
                ? getString(R.string.offer_detail_helper_actionable)
                : getString(R.string.offer_detail_helper_locked));
    }

    private void acceptOffer() {
        if (currentPorte == null || currentPorte.getId() == null) {
            return;
        }
        Long conductorId = SessionManager.resolveConductorId();
        if (conductorId == null || conductorId <= 0) {
            Snackbar.make(requireView(), R.string.incidencia_error_sesion, Snackbar.LENGTH_LONG).show();
            return;
        }
        setActionsEnabled(false);
        porteRepository.acceptOffer(currentPorte.getId(), conductorId, this::handleAcceptResult);
    }

    private void rejectOffer() {
        if (currentPorte == null || currentPorte.getId() == null) {
            return;
        }
        Long conductorId = SessionManager.resolveConductorId();
        if (conductorId == null || conductorId <= 0) {
            Snackbar.make(requireView(), R.string.incidencia_error_sesion, Snackbar.LENGTH_LONG).show();
            return;
        }
        setActionsEnabled(false);
        porteRepository.rejectOffer(currentPorte.getId(), conductorId, this::handleRejectResult);
    }

    private void handleAcceptResult(@NonNull RepositoryResult<Porte> result) {
        if (!isAdded()) {
            return;
        }
        setActionsEnabled(true);
        if (!result.isSuccessful() || result.getData() == null) {
            Snackbar.make(requireView(), result.getMessage(), Snackbar.LENGTH_LONG).show();
            return;
        }
        currentPorte = result.getData();
        renderOffer(currentPorte);
        Snackbar.make(requireView(), R.string.offer_accept_success, Snackbar.LENGTH_LONG)
                .setAction(R.string.offer_accept_open_trip, v -> openTripDetail())
                .show();
    }

    private void handleRejectResult(@NonNull RepositoryResult<Void> result) {
        if (!isAdded()) {
            return;
        }
        setActionsEnabled(true);
        if (!result.isSuccessful()) {
            Snackbar.make(requireView(), result.getMessage(), Snackbar.LENGTH_LONG).show();
            return;
        }
        Bundle refresh = new Bundle();
        refresh.putBoolean(OfferInboxFragment.RESULT_KEY_REFRESH, true);
        getParentFragmentManager().setFragmentResult(OfferInboxFragment.REQUEST_KEY_REFRESH, refresh);
        Snackbar.make(requireView(), R.string.offer_reject_success, Snackbar.LENGTH_SHORT).show();
        getParentFragmentManager().popBackStack();
    }

    private void openTripDetail() {
        if (currentPorte == null || currentPorte.getId() == null) {
            return;
        }
        getParentFragmentManager()
                .beginTransaction()
                .replace(R.id.contentFragmentContainer, TripDetailFragment.newInstance(currentPorte.getId()))
                .addToBackStack(null)
                .commit();
    }

    private void showLoading() {
        loadingContainer.setVisibility(View.VISIBLE);
        contentContainer.setVisibility(View.GONE);
        emptyContainer.setVisibility(View.GONE);
        errorContainer.setVisibility(View.GONE);
    }

    private void showContent() {
        loadingContainer.setVisibility(View.GONE);
        contentContainer.setVisibility(View.VISIBLE);
        emptyContainer.setVisibility(View.GONE);
        errorContainer.setVisibility(View.GONE);
    }

    private void showEmpty(@NonNull String message) {
        loadingContainer.setVisibility(View.GONE);
        contentContainer.setVisibility(View.GONE);
        emptyContainer.setVisibility(View.VISIBLE);
        errorContainer.setVisibility(View.GONE);
        emptyMessage.setText(message);
    }

    private void showError(@NonNull String message) {
        loadingContainer.setVisibility(View.GONE);
        contentContainer.setVisibility(View.GONE);
        emptyContainer.setVisibility(View.GONE);
        errorContainer.setVisibility(View.VISIBLE);
        if (message.trim().isEmpty()) {
            errorMessage.setText(R.string.offer_detail_state_error_title);
        } else {
            errorMessage.setText(message);
        }
    }

    private void setActionsEnabled(boolean enabled) {
        acceptButton.setEnabled(enabled && currentPorte != null && porteRepository.canAcceptOffer(currentPorte));
        rejectButton.setEnabled(enabled
                && currentPorte != null
                && porteRepository.canAcceptOffer(currentPorte)
                && porteRepository.supportsOfferRejection());
    }

    private void applyStateColor(@NonNull TextView stateText, EstadoPorte state) {
        int backgroundRes = R.color.ch_blue_100;
        int textRes = R.color.ch_blue_700;
        if (state == EstadoPorte.PENDIENTE) {
            backgroundRes = R.color.ch_warning_soft;
            textRes = R.color.ch_warning_text;
        } else if (state == EstadoPorte.ASIGNADO || state == EstadoPorte.EN_TRANSITO) {
            backgroundRes = R.color.ch_success_soft;
            textRes = R.color.ch_success_text;
        } else if (state == EstadoPorte.ENTREGADO) {
            backgroundRes = R.color.ch_blue_50;
            textRes = R.color.ch_blue_700;
        }
        stateText.setBackgroundResource(R.drawable.bg_state_chip);
        stateText.setBackgroundTintList(stateText.getContext().getColorStateList(backgroundRes));
        stateText.setTextColor(stateText.getContext().getColor(textRes));
    }
}
