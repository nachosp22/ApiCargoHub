package com.cargohub.mobile.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.cargohub.mobile.data.model.EstadoPorte;
import com.cargohub.mobile.data.model.Porte;

import org.junit.Test;

import java.util.List;

public class PorteRepositoryTest {

    private final PorteRepository repository = new PorteRepository();

    @Test
    public void canAcceptOffer_onlyWhenPorteIsPending() {
        Porte porte = new Porte();
        porte.setEstado("PENDIENTE");

        assertTrue(repository.canAcceptOffer(porte));

        porte.setEstado("ASIGNADO");
        assertFalse(repository.canAcceptOffer(porte));
    }

    @Test
    public void getAvailableActions_matchesOperationalState() {
        Porte porte = new Porte();
        porte.setEstado("EN_TRANSITO");

        List<PorteRepository.PorteAction> actions = repository.getAvailableActions(porte);

        assertEquals(1, actions.size());
        assertEquals(PorteRepository.PorteAction.COMPLETE_TRIP, actions.get(0));
    }

    @Test
    public void isTransitionAllowed_onlyForForwardDriverFlow() {
        assertTrue(repository.isTransitionAllowed(EstadoPorte.ASIGNADO, EstadoPorte.EN_TRANSITO));
        assertTrue(repository.isTransitionAllowed(EstadoPorte.EN_TRANSITO, EstadoPorte.ENTREGADO));
        assertFalse(repository.isTransitionAllowed(EstadoPorte.PENDIENTE, EstadoPorte.ENTREGADO));
        assertFalse(repository.isTransitionAllowed(EstadoPorte.ENTREGADO, EstadoPorte.EN_TRANSITO));
    }

    @Test
    public void supportsOfferRejection_isTrueWhenBackendProvidesEndpoint() {
        assertTrue(repository.supportsOfferRejection());
    }

    @Test
    public void getAvailableActions_includesRejectForPendingOffers() {
        Porte porte = new Porte();
        porte.setEstado("PENDIENTE");

        List<PorteRepository.PorteAction> actions = repository.getAvailableActions(porte);

        assertEquals(2, actions.size());
        assertTrue(actions.contains(PorteRepository.PorteAction.ACCEPT_OFFER));
        assertTrue(actions.contains(PorteRepository.PorteAction.REJECT_OFFER));
    }
}
