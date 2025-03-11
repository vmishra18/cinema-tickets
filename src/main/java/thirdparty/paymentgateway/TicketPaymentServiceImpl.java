package uk.gov.dwp.uc.pairtest;

import thirdparty.paymentgateway.TicketPaymentService;
import thirdparty.seatbooking.SeatReservationService;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest;
import uk.gov.dwp.uc.pairtest.exception.InvalidPurchaseException;

/**
 * TicketServiceImpl handles the logic for purchasing tickets, applying business rules 
 * for validation, payment processing, and seat reservations.
 */
public class TicketServiceImpl implements TicketService {

    private static final int MAX_TICKETS = 25;
    private static final int INFANT_PRICE = 0;
    private static final int CHILD_PRICE = 15;
    private static final int ADULT_PRICE = 25;
    
    private final TicketPaymentService ticketPaymentService;
    private final SeatReservationService seatReservationService;

    /**
     * Constructor to initialize the services needed for ticket purchase.
     * 
     * @param ticketPaymentService Service to handle payment transactions
     * @param seatReservationService Service to handle seat reservations
     */
    public TicketServiceImpl(TicketPaymentService ticketPaymentService, 
                             SeatReservationService seatReservationService) {
        this.ticketPaymentService = ticketPaymentService;
        this.seatReservationService = seatReservationService;
    }

    /**
     * Purchases tickets for the given account, applying business rule validations.
     * 
     * @param accountId The account ID making the purchase
     * @param ticketTypeRequests Array of ticket requests for different ticket types
     * @throws InvalidPurchaseException If the purchase request is invalid based on business rules
     */
    @Override
    public void purchaseTickets(Long accountId, TicketTypeRequest... ticketTypeRequests) 
            throws InvalidPurchaseException {
        
        // Validate account and ticket requests
        validateAccount(accountId);
        validateTicketRequests(ticketTypeRequests);
        
        int totalTickets = 0;
        int infantTickets = 0;
        int childTickets = 0;
        int adultTickets = 0;
        int totalAmount = 0;
        
        // Loop through the ticket requests and accumulate the relevant totals
        for (TicketTypeRequest request : ticketTypeRequests) {
            int numTickets = request.getNoOfTickets();
            totalTickets += numTickets;
            
            switch (request.getTicketType()) {
                case INFANT:
                    infantTickets += numTickets;
                    // Infants are free, so no charge added
                    break;
                case CHILD:
                    childTickets += numTickets;
                    totalAmount += numTickets * CHILD_PRICE;
                    break;
                case ADULT:
                    adultTickets += numTickets;
                    totalAmount += numTickets * ADULT_PRICE;
                    break;
            }
        }
        
        // Apply business rule checks
        validateBusinessRules(totalTickets, infantTickets, childTickets, adultTickets);
        
        // Calculate the number of seats to reserve (only for adults and children, not infants)
        int seatsToReserve = adultTickets + childTickets;
        
        // Proceed with payment and seat reservation
        ticketPaymentService.makePayment(accountId, totalAmount);
        seatReservationService.reserveSeat(accountId, seatsToReserve);
    }
    
    /**
     * Validates the account ID.
     * 
     * @param accountId The account ID to validate
     * @throws InvalidPurchaseException If the account ID is invalid
     */
    private void validateAccount(Long accountId) throws InvalidPurchaseException {
        if (accountId == null || accountId <= 0) {
            throw new InvalidPurchaseException("Invalid account ID");
        }
    }
    
    /**
     * Validates the ticket requests.
     * 
     * @param ticketTypeRequests Array of ticket requests to validate
     * @throws InvalidPurchaseException If any request is invalid
     */
    private void validateTicketRequests(TicketTypeRequest... ticketTypeRequests) throws InvalidPurchaseException {
        if (ticketTypeRequests == null || ticketTypeRequests.length == 0) {
            throw new InvalidPurchaseException("No tickets requested");
        }
        
        for (TicketTypeRequest request : ticketTypeRequests) {
            if (request == null) {
                throw new InvalidPurchaseException("Ticket request cannot be null");
            }
            
            if (request.getNoOfTickets() <= 0) {
                throw new InvalidPurchaseException("Number of tickets must be positive");
            }
        }
    }
    
    /**
     * Validates the business rules for the ticket purchase.
     * 
     * @param totalTickets The total number of tickets requested
     * @param infantTickets The number of infant tickets
     * @param childTickets The number of child tickets
     * @param adultTickets The number of adult tickets
     * @throws InvalidPurchaseException If any business rule is violated
     */
    private void validateBusinessRules(int totalTickets, int infantTickets, int childTickets, int adultTickets) 
            throws InvalidPurchaseException {
        
        // Rule: Maximum of 25 tickets allowed per purchase
        if (totalTickets > MAX_TICKETS) {
            throw new InvalidPurchaseException("Cannot purchase more than 25 tickets");
        }
        
        // Rule: At least one adult ticket is required if child or infant tickets are purchased
        if ((infantTickets > 0 || childTickets > 0) && adultTickets == 0) {
            throw new InvalidPurchaseException("Child or Infant tickets cannot be purchased without an Adult ticket");
        }
        
        // Rule: The number of infant tickets cannot exceed the number of adult tickets
        if (infantTickets > adultTickets) {
            throw new InvalidPurchaseException("Number of Infant tickets cannot exceed number of Adult tickets");
        }
    }
}
