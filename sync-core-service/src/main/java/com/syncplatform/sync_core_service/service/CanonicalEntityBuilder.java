package com.syncplatform.sync_core_service.service;

import com.syncplatform.sync_core_service.entity.Customer;
import com.syncplatform.sync_core_service.entity.Order;
import com.syncplatform.sync_core_service.entity.OrderLineItem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@Slf4j
public class CanonicalEntityBuilder {

    @SuppressWarnings("unchecked")
    public Customer buildCustomerFromShopifyPayload(UUID syncConnectionId, Map<String, Object> payload) {
        // Shopify order payload contains a nested "customer" object
        Map<String, Object> customerData = (Map<String, Object>) payload.get("customer");
        if (customerData == null) {
            return null; // guest checkout
        }

        Customer customer = new Customer();
        customer.setSyncConnectionId(syncConnectionId);
        customer.setExternalCustomerId(String.valueOf(customerData.get("id")));
        customer.setEmail((String) customerData.get("email"));
        customer.setFirstName((String) customerData.get("first_name"));
        customer.setLastName((String) customerData.get("last_name"));
        customer.setPhone((String) customerData.get("phone"));
        customer.setCurrency((String) payload.getOrDefault("currency", "USD"));
        customer.setTaxExempt(Boolean.TRUE.equals(customerData.get("tax_exempt")));

        return customer;
    }

    @SuppressWarnings("unchecked")
    public Order buildOrderFromShopifyPayload(UUID syncConnectionId, Map<String, Object> payload) {
        Order order = new Order();
        order.setSyncConnectionId(syncConnectionId);
        order.setExternalOrderId(String.valueOf(payload.get("id")));
        order.setOrderNumber(String.valueOf(payload.getOrDefault("name", payload.get("order_number"))));
        order.setStatus(mapShopifyStatus(payload));
        order.setCurrency((String) payload.getOrDefault("currency", "USD"));

        order.setSubtotal(toDecimal(payload.get("subtotal_price")));
        order.setTotalDiscount(toDecimal(payload.get("total_discounts")));
        order.setTotalTax(toDecimal(payload.get("total_tax")));
        order.setTotalShipping(toDecimal(extractShippingTotal(payload)));
        order.setTotalAmount(toDecimal(payload.get("total_price")));

        order.setPlacedAt(parseShopifyDate((String) payload.get("created_at")));
        order.setPaidAt(parseShopifyDate((String) payload.get("processed_at")));

        Map<String, Object> paymentDetails = (Map<String, Object>) payload.get("payment_details");
        if (paymentDetails != null) {
            order.setPaymentMethod((String) paymentDetails.get("credit_card_company"));
        }

        order.setNotes((String) payload.get("note"));

        return order;
    }

    @SuppressWarnings("unchecked")
    public List<OrderLineItem> buildLineItemsFromShopifyPayload(Map<String, Object> payload) {
        List<OrderLineItem> lineItems = new ArrayList<>();
        List<Map<String, Object>> items = (List<Map<String, Object>>) payload.get("line_items");

        if (items == null)
            return lineItems;

        int position = 0;
        for (Map<String, Object> item : items) {
            OrderLineItem li = new OrderLineItem();
            li.setDescription((String) item.getOrDefault("title", "Unknown item"));
            li.setQuantity(((Number) item.getOrDefault("quantity", 1)).intValue());
            li.setUnitPrice(toDecimal(item.get("price")));
            li.setDiscountAmount(extractLineDiscount(item));
            li.setTaxAmount(extractLineTax(item));

            BigDecimal lineTotal = li.getUnitPrice()
                    .multiply(BigDecimal.valueOf(li.getQuantity()))
                    .subtract(li.getDiscountAmount())
                    .add(li.getTaxAmount());
            li.setTotal(lineTotal);
            li.setLinePosition(position++);

            lineItems.add(li);
        }

        return lineItems;
    }

    private String mapShopifyStatus(Map<String, Object> payload) {
        String financialStatus = (String) payload.get("financial_status");
        if (financialStatus == null)
            return "pending";

        return switch (financialStatus) {
            case "paid" -> "paid";
            case "refunded" -> "refunded";
            case "partially_refunded" -> "partially_refunded";
            case "voided" -> "canceled";
            default -> "pending";
        };
    }

    @SuppressWarnings("unchecked")
    private Object extractShippingTotal(Map<String, Object> payload) {
        List<Map<String, Object>> shippingLines = (List<Map<String, Object>>) payload.get("shipping_lines");
        if (shippingLines == null || shippingLines.isEmpty())
            return "0";

        BigDecimal total = BigDecimal.ZERO;
        for (Map<String, Object> line : shippingLines) {
            total = total.add(toDecimal(line.get("price")));
        }
        return total.toString();
    }

    @SuppressWarnings("unchecked")
    private BigDecimal extractLineDiscount(Map<String, Object> item) {
        List<Map<String, Object>> allocations = (List<Map<String, Object>>) item.get("discount_allocations");
        if (allocations == null)
            return BigDecimal.ZERO;

        BigDecimal total = BigDecimal.ZERO;
        for (Map<String, Object> alloc : allocations) {
            total = total.add(toDecimal(alloc.get("amount")));
        }
        return total;
    }

    @SuppressWarnings("unchecked")
    private BigDecimal extractLineTax(Map<String, Object> item) {
        List<Map<String, Object>> taxLines = (List<Map<String, Object>>) item.get("tax_lines");
        if (taxLines == null)
            return BigDecimal.ZERO;

        BigDecimal total = BigDecimal.ZERO;
        for (Map<String, Object> tax : taxLines) {
            total = total.add(toDecimal(tax.get("price")));
        }
        return total;
    }

    private BigDecimal toDecimal(Object value) {
        if (value == null)
            return BigDecimal.ZERO;
        try {
            return new BigDecimal(value.toString());
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    private OffsetDateTime parseShopifyDate(String dateStr) {
        if (dateStr == null)
            return null;
        try {
            return OffsetDateTime.parse(dateStr, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        } catch (Exception e) {
            log.warn("Could not parse date: {}", dateStr);
            return null;
        }
    }
}