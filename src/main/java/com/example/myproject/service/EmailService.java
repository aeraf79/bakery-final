package com.example.myproject.service;

import com.example.myproject.entity.OrderEntity;
import com.example.myproject.entity.OrderItemEntity;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    // ─────────────────────────────────────────────────────────────────────────
    // WELCOME EMAIL — only takes Strings, always safe to be @Async
    // ─────────────────────────────────────────────────────────────────────────
    @Async
    public void sendWelcomeEmail(String toEmail, String fullName) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Welcome to Maison Dorée Bakery! 🎉");
            helper.setText(buildWelcomeHtml(fullName), true);
            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("Failed to send welcome email: " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ORDER CONFIRMATION — called from inside @Transactional in OrderService
    // and RazorpayService, so the Hibernate session IS open here.
    //
    // KEY FIX: This method is NOT @Async.
    // It extracts all lazy-loaded data synchronously while the session is open,
    // then passes only plain Strings/primitives to the @Async sender below.
    //
    // Previous bug: method was @Async itself, so it ran on a new thread AFTER
    // the transaction closed — order.getUser() and order.getOrderItems() threw
    // LazyInitializationException because the session was already gone.
    // ─────────────────────────────────────────────────────────────────────────
    public void sendOrderConfirmationEmail(OrderEntity order) {
        try {
            // ── Extract lazy fields while session is open ───────────────────
            String toEmail      = order.getUser().getEmail();       // LAZY — must read here
            String customerName = order.getShippingName();
            String orderNumber  = order.getOrderNumber();
            boolean isCod       = order.getPaymentMethod() == OrderEntity.PaymentMethod.COD;

            String orderDate = order.getCreatedAt() != null
                ? order.getCreatedAt().format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a"))
                : "—";

            String totalAmount = fmt(order.getTotalAmount());
            String shippingFee = (order.getShippingFee() == null
                || order.getShippingFee().compareTo(BigDecimal.ZERO) == 0)
                ? "FREE" : fmt(order.getShippingFee());
            String finalAmount = fmt(order.getFinalAmount());

            String address = esc(order.getShippingAddress()) + ", "
                + esc(order.getShippingCity()) + ", "
                + esc(order.getShippingState()) + " – " + esc(order.getShippingPincode());
            String phone = esc(order.getShippingPhone());
            String notes = order.getOrderNotes() != null && !order.getOrderNotes().isBlank()
                ? "📝 " + esc(order.getOrderNotes()) : "";

            // Extract order items into plain POJOs — LAZY collection, read now
            List<EmailOrderItem> items = new ArrayList<>();
            if (order.getOrderItems() != null) {
                for (OrderItemEntity item : order.getOrderItems()) {
                    items.add(new EmailOrderItem(
                        item.getProductName(),
                        item.getQuantity(),
                        item.getSubtotal()
                    ));
                }
            }

            // ── Hand off to @Async sender — only plain data, no entity refs ─
            sendOrderEmailAsync(toEmail, customerName, orderNumber, orderDate,
                isCod, items, totalAmount, shippingFee, finalAmount, address, phone, notes);

        } catch (Exception e) {
            // Never break checkout due to email issues
            System.err.println("Failed to prepare order confirmation email: " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ASYNC SMTP SENDER — receives only plain values, safe on any thread
    // ─────────────────────────────────────────────────────────────────────────
    @Async
    public void sendOrderEmailAsync(
        String toEmail, String customerName, String orderNumber, String orderDate,
        boolean isCod, List<EmailOrderItem> items,
        String totalAmount, String shippingFee, String finalAmount,
        String address, String phone, String notes
    ) {
        try {
            StringBuilder itemRowsHtml = new StringBuilder();
            for (EmailOrderItem item : items) {
                itemRowsHtml.append(buildItemRow(item.productName, item.quantity, item.subtotal));
            }

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject(isCod
                ? "Order Confirmed! 🎊 #" + orderNumber + " – Pay on Delivery"
                : "Payment Successful! ✅ Order #" + orderNumber + " Confirmed");
            helper.setText(buildOrderHtml(customerName, orderNumber, orderDate, isCod,
                itemRowsHtml.toString(), totalAmount, shippingFee, finalAmount,
                address, phone, notes), true);
            mailSender.send(message);
            System.out.println("✅ Order confirmation email sent → " + toEmail);
        } catch (MessagingException e) {
            System.err.println("❌ Failed to send order confirmation email: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("❌ Unexpected error sending email: " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SIMPLE VALUE OBJECT — carries item data safely across the thread boundary
    // ─────────────────────────────────────────────────────────────────────────
    public static class EmailOrderItem {
        final String productName;
        final int quantity;
        final BigDecimal subtotal;

        EmailOrderItem(String productName, int quantity, BigDecimal subtotal) {
            this.productName = productName;
            this.quantity    = quantity;
            this.subtotal    = subtotal;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HTML BUILDERS
    // ─────────────────────────────────────────────────────────────────────────
    private String buildOrderHtml(
        String customerName, String orderNumber, String orderDate, boolean isCod,
        String itemRowsHtml, String totalAmount, String shippingFee, String finalAmount,
        String address, String phone, String notes
    ) {
        String paymentColor = isCod ? "#d97706" : "#059669";
        String paymentBadge = isCod ? "💵 Cash on Delivery" : "✅ Paid Online (Razorpay)";

        String paymentNote = isCod
            ? "<div style='background:#fffbeb;border-left:4px solid #f59e0b;padding:14px 16px;"
              + "border-radius:6px;margin:20px 0;color:#92400e;font-size:14px;'>"
              + "<strong>💡 Reminder:</strong> Please keep exact change of "
              + "<strong>" + finalAmount + "</strong> ready for our delivery partner.</div>"
            : "<div style='background:#f0fdf4;border-left:4px solid #22c55e;padding:14px 16px;"
              + "border-radius:6px;margin:20px 0;color:#166534;font-size:14px;'>"
              + "✅ <strong>Payment received.</strong> Your fresh bakes are being prepared right now!</div>";

        String notesRow = notes.isEmpty() ? "" :
            "<p style='margin:8px 0 0;color:#888;font-size:13px;font-style:italic;'>" + notes + "</p>";

        return "<!DOCTYPE html><html><head><meta charset='UTF-8'>"
            + "<meta name='viewport' content='width=device-width,initial-scale=1'></head>"
            + "<body style='margin:0;padding:0;background:#fdf6ee;font-family:Arial,sans-serif;'>"
            + "<div style='max-width:620px;margin:30px auto;background:#fff;border-radius:14px;"
            + "overflow:hidden;box-shadow:0 4px 24px rgba(139,69,19,0.13);'>"
            + "<div style='background:linear-gradient(135deg,#8B4513,#5c2d0a);padding:42px 30px;text-align:center;'>"
            + "<p style='color:#f8d7a0;margin:0 0 6px;font-size:12px;letter-spacing:3px;text-transform:uppercase;'>Maison Dorée</p>"
            + "<h1 style='color:#fff;margin:0;font-size:26px;'>" + (isCod ? "Order Confirmed! 🎊" : "Payment Successful! 🎉") + "</h1>"
            + "<p style='color:#f8d7a0;margin:10px 0 0;font-size:14px;'>Thank you for choosing our artisan bakery</p>"
            + "</div>"
            + "<div style='padding:28px 30px 0;'>"
            + "<h2 style='color:#3d1f08;margin:0 0 6px;font-size:20px;'>Hi " + esc(customerName) + "! 👋</h2>"
            + "<p style='color:#555;margin:0;line-height:1.6;font-size:15px;'>We've received your order and it's being freshly prepared with love.</p>"
            + paymentNote + "</div>"
            + "<div style='margin:0 30px;border-radius:10px;overflow:hidden;border:1px solid #fde8d0;'>"
            + "<table style='width:100%;border-collapse:collapse;background:#fdf3e7;'><tr>"
            + "<td style='padding:14px 18px;border-right:1px solid #fde8d0;'>"
            + "<p style='margin:0;font-size:11px;color:#aaa;text-transform:uppercase;letter-spacing:1px;'>Order No.</p>"
            + "<p style='margin:4px 0 0;font-weight:700;color:#8B4513;font-size:15px;'>#" + esc(orderNumber) + "</p>"
            + "</td><td style='padding:14px 18px;border-right:1px solid #fde8d0;'>"
            + "<p style='margin:0;font-size:11px;color:#aaa;text-transform:uppercase;letter-spacing:1px;'>Date</p>"
            + "<p style='margin:4px 0 0;font-weight:600;color:#3d1f08;font-size:13px;'>" + orderDate + "</p>"
            + "</td><td style='padding:14px 18px;'>"
            + "<p style='margin:0;font-size:11px;color:#aaa;text-transform:uppercase;letter-spacing:1px;'>Payment</p>"
            + "<p style='margin:4px 0 0;font-weight:600;font-size:13px;color:" + paymentColor + ";'>" + paymentBadge + "</p>"
            + "</td></tr></table></div>"
            + "<div style='padding:22px 30px 0;'>"
            + "<h3 style='color:#3d1f08;margin:0 0 10px;font-size:15px;border-bottom:2px solid #fde8d0;padding-bottom:8px;'>🛒 Items Ordered</h3>"
            + "<table style='width:100%;border-collapse:collapse;'>"
            + "<thead><tr style='background:#fdf3e7;'>"
            + "<th style='padding:9px 8px;text-align:left;color:#8B4513;font-size:11px;text-transform:uppercase;letter-spacing:1px;'>Product</th>"
            + "<th style='padding:9px 8px;text-align:center;color:#8B4513;font-size:11px;text-transform:uppercase;letter-spacing:1px;'>Qty</th>"
            + "<th style='padding:9px 8px;text-align:right;color:#8B4513;font-size:11px;text-transform:uppercase;letter-spacing:1px;'>Amount</th>"
            + "</tr></thead><tbody>" + itemRowsHtml + "</tbody></table></div>"
            + "<div style='padding:14px 30px;'>"
            + "<table style='width:100%;border-collapse:collapse;'>"
            + "<tr><td style='padding:5px 0;color:#666;font-size:14px;'>Subtotal</td>"
            + "<td style='padding:5px 0;text-align:right;color:#333;font-size:14px;'>" + totalAmount + "</td></tr>"
            + "<tr><td style='padding:5px 0;color:#666;font-size:14px;'>Shipping</td>"
            + "<td style='padding:5px 0;text-align:right;font-size:14px;color:#059669;font-weight:600;'>" + shippingFee + "</td></tr>"
            + "<tr style='border-top:2px solid #fde8d0;'>"
            + "<td style='padding:10px 0 4px;font-weight:700;color:#3d1f08;font-size:16px;'>" + (isCod ? "Amount Due" : "Total Paid") + "</td>"
            + "<td style='padding:10px 0 4px;text-align:right;font-weight:700;color:#8B4513;font-size:18px;'>" + finalAmount + "</td>"
            + "</tr></table></div>"
            + "<div style='margin:0 30px 22px;background:#fdf3e7;border-radius:10px;padding:16px 18px;'>"
            + "<h3 style='margin:0 0 10px;color:#3d1f08;font-size:15px;'>📦 Delivery Address</h3>"
            + "<p style='margin:0;color:#555;line-height:1.7;font-size:14px;'>"
            + "<strong>" + esc(customerName) + "</strong><br>📞 " + phone + "<br>" + address
            + "</p>" + notesRow + "</div>"
            + "<div style='background:#3d1f08;padding:24px 30px;text-align:center;'>"
            + "<p style='color:#f8d7a0;margin:0 0 4px;font-size:16px;font-weight:600;'>Maison Dorée Artisan Bakery</p>"
            + "<p style='color:#c8a06c;margin:0 0 10px;font-size:13px;'>Freshly baked with love, every single day 🥖</p>"
            + "<p style='color:#c8a06c;margin:0;font-size:12px;'>© 2025 Maison Dorée. All rights reserved.</p>"
            + "</div></div></body></html>";
    }

    private String buildItemRow(String productName, int qty, BigDecimal subtotal) {
        return "<tr>"
            + "<td style='padding:12px 8px;border-bottom:1px solid #fde8d0;color:#3d1f08;'><strong>" + esc(productName) + "</strong></td>"
            + "<td style='padding:12px 8px;border-bottom:1px solid #fde8d0;text-align:center;color:#666;'>× " + qty + "</td>"
            + "<td style='padding:12px 8px;border-bottom:1px solid #fde8d0;text-align:right;color:#8B4513;font-weight:600;'>" + fmt(subtotal) + "</td>"
            + "</tr>";
    }

    private String buildWelcomeHtml(String fullName) {
        return "<!DOCTYPE html><html><head><meta charset='UTF-8'></head>"
            + "<body style='margin:0;padding:0;background:#fdf6ee;font-family:Arial,sans-serif;'>"
            + "<div style='max-width:600px;margin:30px auto;background:#fff;border-radius:12px;overflow:hidden;box-shadow:0 4px 20px rgba(139,69,19,0.12);'>"
            + "<div style='background:linear-gradient(135deg,#8B4513,#5c2d0a);padding:40px 30px;text-align:center;'>"
            + "<p style='color:#f8d7a0;margin:0 0 6px;font-size:12px;letter-spacing:3px;'>MAISON DORÉE</p>"
            + "<h1 style='color:#fff;margin:0;font-size:26px;'>Welcome to the Bakery! 🥐</h1>"
            + "</div>"
            + "<div style='padding:30px;color:#333;line-height:1.6;'>"
            + "<h2>Hi " + esc(fullName) + "! 👋</h2>"
            + "<p>Your account has been created successfully. Welcome to Maison Dorée!</p>"
            + "<p>Browse our fresh breads, pastries, cakes and more — baked daily with love.</p>"
            + "<p>Happy Shopping! 🛍️</p>"
            + "<p><strong>— The Maison Dorée Team</strong></p>"
            + "</div>"
            + "<div style='background:#3d1f08;text-align:center;padding:16px;font-size:12px;color:#c8a06c;'>"
            + "© 2025 Maison Dorée Bakery. All rights reserved."
            + "</div></div></body></html>";
    }

    private String fmt(BigDecimal amount) {
        if (amount == null) return "₹0.00";
        return "₹" + String.format("%,.2f", amount);
    }

    private String esc(String input) {
        if (input == null) return "";
        return input.replace("&", "&amp;").replace("<", "&lt;")
                    .replace(">", "&gt;").replace("\"", "&quot;");
    }
}
