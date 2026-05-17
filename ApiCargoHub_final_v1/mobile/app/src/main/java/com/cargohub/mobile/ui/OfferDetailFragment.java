package com.cargohub.mobile.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
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
    private TextView routeText;
    private TextView pickupText;
    private TextView distanceText;
    private TextView cargoText;
    private TextView priceText;
    private TextView weightText;
    private TextView volumeText;
    private TextView dimensionsText;
    private TextView vehicleText;
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
        Bundle args = getArguments();
        swipeRefreshLayout = view.findViewById(R.id.offerDetailSwipeRefresh);
        loadingContainer = view.findViewById(R.id.offerDetailLoadingContainer);
        emptyContainer = view.findViewById(R.id.offerDetailEmptyContainer);
        errorContainer = view.findViewById(R.id.offerDetailErrorContainer);
        contentContainer = view.findViewById(R.id.offerDetailContentContainer);
        titleText = view.findViewById(R.id.offerDetailTitleText);
        routeText = view.findViewById(R.id.offerDetailRouteText);
        pickupText = view.findViewById(R.id.offerDetailPickupText);
        distanceText = view.findViewById(R.id.offerDetailDistanceText);
        cargoText = view.findViewById(R.id.offerDetailCargoText);
        priceText = view.findViewById(R.id.offerDetailPriceText);
        weightText = view.findViewById(R.id.offerDetailWeightText);
        volumeText = view.findViewById(R.id.offerDetailVolumeText);
        dimensionsText = view.findViewById(R.id.offerDetailDimensionsText);
        vehicleText = view.findViewById(R.id.offerDetailVehicleText);
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

        if (args == null || !args.containsKey(ARG_PORTE_ID)) {
            showError(getString(R.string.offer_missing_id));
            return;
        }
        porteId = args.getLong(ARG_PORTE_ID);
        if (porteId <= 0L) {
            showError(getString(R.string.offer_missing_id));
            return;
        }
        loadOffer();
    }

    private void loadOffer() {
        Long conductorId = SessionManager.resolveConductorId();
        if (conductorId == null || conductorId <= 0) {
            showError(getString(R.string.incidencia_error_sesion));
            swipeRefreshLayout.setRefreshing(false);
            return;
        }
        showLoading();
        porteRepository.getOfferDetailForConductor(porteId, conductorId, result -> {
            if (!isAdded()) {
                return;
            }
            swipeRefreshLayout.setRefreshing(false);
            if (!result.isSuccessful() && result.getCode() == 404) {
                showEmpty(getString(R.string.offer_detail_state_empty_body));
                return;
            }
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
        titleText.setText(UiFormatters.formatPorteShortTitle(porte));
        routeText.setText(getString(R.string.offer_detail_route_value, UiFormatters.formatPorteRoute(porte)));
        pickupText.setText(getString(R.string.offer_detail_pickup_value, UiFormatters.formatDateTime(porte.getFechaRecogida())));
        distanceText.setText(getString(R.string.offer_detail_distance_value, UiFormatters.formatPorteDistance(porte)));
        priceText.setText(getString(R.string.offer_detail_price_value, UiFormatters.formatPortePrice(porte)));
        cargoText.setText(getString(R.string.offer_detail_cargo_value,
                UiFormatters.valueOrFallback(porte.getDescripcionMercancia(), getString(R.string.offer_detail_not_available))));
        weightText.setText(getString(R.string.offer_detail_weight_value, UiFormatters.formatPorteWeight(porte)));
        volumeText.setText(getString(R.string.offer_detail_volume_value, UiFormatters.formatPorteVolume(porte)));
        dimensionsText.setText(getString(R.string.offer_detail_dimensions_value, UiFormatters.formatPorteDimensions(porte)));
        vehicleText.setText(getString(R.string.offer_detail_vehicle_value, UiFormatters.formatPorteVehicleRequirement(porte)));
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
            showSnackbar(R.string.incidencia_error_sesion, Snackbar.LENGTH_LONG);
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
            showSnackbar(R.string.incidencia_error_sesion, Snackbar.LENGTH_LONG);
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
            showSnackbar(result.getMessage(), R.string.offer_detail_state_error_body, Snackbar.LENGTH_LONG);
            return;
        }
        currentPorte = result.getData();
        renderOffer(currentPorte);
        View view = getView();
        if (view != null) {
            Snackbar.make(view, R.string.offer_accept_success, Snackbar.LENGTH_LONG)
                    .setAction(R.string.offer_accept_open_trip, v -> openTripDetail())
                    .show();
        }
    }

    private void handleRejectResult(@NonNull RepositoryResult<Void> result) {
        if (!isAdded()) {
            return;
        }
        setActionsEnabled(true);
        if (!result.isSuccessful()) {
            showSnackbar(result.getMessage(), R.string.offer_detail_state_error_body, Snackbar.LENGTH_LONG);
            return;
        }
        Bundle refresh = new Bundle();
        refresh.putBoolean(OfferInboxFragment.RESULT_KEY_REFRESH, true);
        getParentFragmentManager().setFragmentResult(OfferInboxFragment.REQUEST_KEY_REFRESH, refresh);
        showSnackbar(R.string.offer_reject_success, Snackbar.LENGTH_SHORT);
        getParentFragmentManager().popBackStack();
    }

    private void showSnackbar(@StringRes int messageRes, int duration) {
        View view = getView();
        if (isAdded() && view != null) {
            Snackbar.make(view, messageRes, duration).show();
        }
    }

    private void showSnackbar(@Nullable String message, @StringRes int fallbackRes, int duration) {
        String resolvedMessage = message != null ? message.trim() : "";
        if (resolvedMessage.isEmpty()) {
            showSnackbar(fallbackRes, duration);
            return;
        }
        View view = getView();
        if (isAdded() && view != null) {
            Snackbar.make(view, resolvedMessage, duration).show();
        }
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
            errorMessage.setText(R.string.offer_detail_state_error_body);
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

}
