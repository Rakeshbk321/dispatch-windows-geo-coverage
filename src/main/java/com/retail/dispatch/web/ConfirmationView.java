package com.retail.dispatch.web;

import java.util.UUID;

public record ConfirmationView(UUID id, String orderId, String address, String date, String time, String price) {
}
