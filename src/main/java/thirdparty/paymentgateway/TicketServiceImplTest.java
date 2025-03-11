package uk.gov.dwp.uc.pairtest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import thirdparty.paymentgateway.TicketPaymentService;
import thirdparty.seatbooking.SeatReservationService;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest;
import uk.gov.dwp.uc.pairtest.exception.InvalidPurchaseException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TicketServiceImplTest {

    @Mock
    private TicketPaymentService ticketPaymentService; // Mocked service for payment processing.

    @Mock
    private SeatReservationService seatReservationService; // Mocked service for seat reservation.

    private TicketServiceImpl ticketService; // Instance of the class being tested.

    @BeforeEach
    void setUp() {
        // Initialize Mockito annotations to create mocks.
        MockitoAnnotations.openMocks(this);
        // Create an instance of TicketServiceImpl with the mocked services.
        ticketService = new TicketServiceImpl(ticketPaymentService, seatReservationService);
    }

    @Test
    void testValidPurchase() throws InvalidPurchaseException {
        // Test a valid purchase scenario with adult and child tickets.
        TicketTypeRequest adult = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 2);
        TicketTypeRequest child = new TicketTypeRequest(TicketTypeRequest.Type.CHILD, 1);
        ticketService.purchaseTickets(1L, adult, child);
        // Verify that the payment service was called with the correct amount.
        verify(ticketPaymentService).makePayment(1L, 65);
        // Verify that the seat reservation service was called with the correct number of seats.
        verify(seatReservationService).reserveSeat(1L, 3);
    }

    @Test
    void testInvalidAccountId() {
        // Test that an exception is thrown for invalid account IDs (0 or null).
        assertThrows(InvalidPurchaseException.class, () -> ticketService.purchaseTickets(0L, new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 1)));
        assertThrows(InvalidPurchaseException.class, () -> ticketService.purchaseTickets(null, new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 1)));
    }

    @Test
    void testTooManyTickets() {
        // Test that an exception is thrown when attempting to purchase more than 25 tickets.
        TicketTypeRequest[] requests = new TicketTypeRequest[26];
        for (int i = 0; i < 26; i++) {
            requests[i] = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 1);
        }
        assertThrows(InvalidPurchaseException.class, () -> ticketService.purchaseTickets(1L, requests));
    }

    @Test
    void testChildOrInfantWithoutAdult() {
        // Test that an exception is thrown when attempting to purchase child or infant tickets without an adult ticket.
        TicketTypeRequest child = new TicketTypeRequest(TicketTypeRequest.Type.CHILD, 1);
        assertThrows(InvalidPurchaseException.class, () -> ticketService.purchaseTickets(1L, child));
    }

    @Test
    void testInfantOnlyWithAdult() throws InvalidPurchaseException {
        // Test that infant tickets can be purchased when accompanied by an adult ticket.
        TicketTypeRequest infant = new TicketTypeRequest(TicketTypeRequest.Type.INFANT, 3);
        TicketTypeRequest adult = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 1);
        ticketService.purchaseTickets(1L, infant, adult);
        verify(ticketPaymentService).makePayment(1L, 25);
        verify(seatReservationService).reserveSeat(1L, 1);
    }

    @Test
    void testNullTicketRequests() {
        // Test that an exception is thrown when the ticket requests array is null.
        assertThrows(InvalidPurchaseException.class, () -> ticketService.purchaseTickets(1L, (TicketTypeRequest[]) null));
    }

    @Test
    void testEmptyTicketRequests() {
        // Test that an exception is thrown when no ticket requests are provided.
        assertThrows(InvalidPurchaseException.class, () -> ticketService.purchaseTickets(1L));
    }

    @Test
    void testNullTicketRequest() {
        // Test that an exception is thrown when a null ticket request is included in the array.
        TicketTypeRequest adult = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 1);
        TicketTypeRequest nullRequest = null;
        assertThrows(InvalidPurchaseException.class, () -> ticketService.purchaseTickets(1L, adult, nullRequest));
    }

    @Test
    void testZeroTickets() {
        // Test that an exception is thrown when a ticket request has zero tickets.
        TicketTypeRequest adult = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 0);
        assertThrows(InvalidPurchaseException.class, () -> ticketService.purchaseTickets(1L, adult));
    }

    @Test
    void testInfantsMoreThanAdults() {
        // Test that an exception is thrown when there are more infant tickets than adult tickets.
        TicketTypeRequest infant = new TicketTypeRequest(TicketTypeRequest.Type.INFANT, 2);
        TicketTypeRequest adult = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 1);
        assertThrows(InvalidPurchaseException.class, () -> ticketService.purchaseTickets(1L, infant, adult));
    }
}