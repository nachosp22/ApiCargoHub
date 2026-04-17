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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.cargohub.mobile.R;
import com.cargohub.mobile.data.PorteRepository;
import com.cargohub.mobile.data.RepositoryResult;
import com.cargohub.mobile.data.model.Porte;
import com.cargohub.mobile.session.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;

import java.util.List;

public class OfferInboxFragment extends Fragment {

    public static final String REQUEST_KEY_REFRESH = "offer_refresh_request";
    public static final String RESULT_KEY_REFRESH = "refresh";

    private final PorteRepository porteRepository = new PorteRepository();

    private SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView recyclerView;
    private LinearLayout loadingContainer;
    private LinearLayout emptyContainer;
    private LinearLayout errorContainer;
    private TextView errorMessage;
    private PorteCardAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_offer_inbox, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        swipeRefreshLayout = view.findViewById(R.id.offerSwipeRefresh);
        recyclerView = view.findViewById(R.id.offerRecyclerView);
        loadingContainer = view.findViewById(R.id.offerLoadingContainer);
        emptyContainer = view.findViewById(R.id.offerEmptyContainer);
        errorContainer = view.findViewById(R.id.offerErrorContainer);
        errorMessage = view.findViewById(R.id.offerErrorMessage);
        MaterialButton retryButton = view.findViewById(R.id.offerRetryButton);
        retryButton.setOnClickListener(v -> loadOffers());
        getParentFragmentManager().setFragmentResultListener(REQUEST_KEY_REFRESH, getViewLifecycleOwner(), (requestKey, result) -> {
            if (result.getBoolean(RESULT_KEY_REFRESH, false)) {
                loadOffers();
            }
        });

        adapter = new PorteCardAdapter(this::openOfferDetail);
        adapter.setCtaLabel(getString(R.string.offer_list_cta));
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        swipeRefreshLayout.setColorSchemeResources(R.color.ch_accent_primary);
        swipeRefreshLayout.setOnRefreshListener(this::loadOffers);
        loadOffers();
    }

    private void loadOffers() {
        Long conductorId = SessionManager.resolveConductorId();
        if (conductorId == null || conductorId <= 0) {
            showError(getString(R.string.incidencia_error_sesion));
            return;
        }
        showLoading();
        porteRepository.getOffers(conductorId, this::handleOffersResult);
    }

    private void handleOffersResult(@NonNull RepositoryResult<List<Porte>> result) {
        if (!isAdded()) {
            return;
        }
        swipeRefreshLayout.setRefreshing(false);
        if (!result.isSuccessful()) {
            showError(result.getMessage());
            return;
        }
        List<Porte> offers = result.getData();
        if (offers == null || offers.isEmpty()) {
            showEmpty();
            return;
        }
        loadingContainer.setVisibility(View.GONE);
        emptyContainer.setVisibility(View.GONE);
        errorContainer.setVisibility(View.GONE);
        recyclerView.setVisibility(View.VISIBLE);
        adapter.setPortes(offers);
    }

    private void openOfferDetail(@NonNull Porte porte) {
        if (porte.getId() == null) {
            Snackbar.make(requireView(), R.string.offer_missing_id, Snackbar.LENGTH_SHORT).show();
            return;
        }
        getParentFragmentManager()
                .beginTransaction()
                .replace(R.id.contentFragmentContainer, OfferDetailFragment.newInstance(porte.getId()))
                .addToBackStack(null)
                .commit();
    }

    private void showLoading() {
        loadingContainer.setVisibility(View.VISIBLE);
        emptyContainer.setVisibility(View.GONE);
        errorContainer.setVisibility(View.GONE);
        recyclerView.setVisibility(View.GONE);
    }

    private void showEmpty() {
        loadingContainer.setVisibility(View.GONE);
        emptyContainer.setVisibility(View.VISIBLE);
        errorContainer.setVisibility(View.GONE);
        recyclerView.setVisibility(View.GONE);
    }

    private void showError(@NonNull String message) {
        loadingContainer.setVisibility(View.GONE);
        emptyContainer.setVisibility(View.GONE);
        errorContainer.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
        if (message.trim().isEmpty()) {
            errorMessage.setText(R.string.offer_list_state_error_title);
        } else {
            errorMessage.setText(message);
        }
    }
}
