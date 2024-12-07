package com.ken.infinity.controllers;

import com.ken.infinity.models.Artwork;
import com.ken.infinity.models.Orders;
import com.ken.infinity.models.User;
import com.ken.infinity.services.ArtworkService;
import com.ken.infinity.services.OrdersService;
import com.ken.infinity.services.SecurityService;
import com.ken.infinity.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class OrderController {
    private final UserService userService;
    private final SecurityService securityService;
    private final OrdersService ordersService;
    private final ArtworkService artworkService;

    @Autowired
    private JavaMailSender javaMailSender;

    @Autowired
    public OrderController(UserService userService, SecurityService securityService, OrdersService ordersService, ArtworkService artworkService) {
        this.userService = userService;
        this.securityService = securityService;
        this.ordersService = ordersService;
        this.artworkService = artworkService;
    }

    @PostMapping("/order")
    public String addOrder(@ModelAttribute("orders") Orders orders, 
                           @RequestParam("artwork_id") int artworkId, 
                           Model model) {
        model.addAttribute("loggedIn", securityService.isLoggedIn());
        
        int currentUserId = userService.findByUsername(securityService.findLoggedInUsername()).getId();
        User user = userService.findByUserId(currentUserId);
        Artwork artwork = artworkService.findArtworkById(artworkId);
        int price = artwork.getPrice();
        
        orders.setPrice(price);
        orders.setOrdered_at(new java.sql.Timestamp(System.currentTimeMillis()));
        
        artworkService.updateArtwork(artworkId);
        ordersService.save(orders, user, artwork);

        // Start sending mail
        try {
            String from = "sai7997242043@gmail.com"; // Your Gmail address
            String to = user.getEmail();

            if (to == null || to.isEmpty()) {
                System.err.println("User email is empty or null. Skipping email sending.");
                return "redirect:/homepage";
            }

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(to);
            message.setSubject("Your order for Artwork from Infinity Art Gallery");
            message.setText(String.format(
                "Hello %s! \n\n" +
                "Thanks for your order #%d placed on %s with Infinity Art Gallery.\n" +
                "Your Order total is %d$. We accept payment via cheque/debit/credit card.\n" +
                "Simply reply to this mail to let us know how you wish to pay. We will send you further instructions.\n" +
                "If you wish to cancel the order, reply to this mail. The due date for payment is up to 15 days.\n" +
                "After that, we may have to cancel your order.\n\n" +
                "Sincerely, \nInfinity Art Gallery",
                user.getFirstName(), orders.getId(), orders.getOrdered_at(), orders.getPrice()
            ));

            javaMailSender.send(message);
            System.out.println("Email sent successfully.");
        } catch (Exception e) {
            System.err.println("Error while sending email: " + e.getMessage());
            e.printStackTrace();
        }

        return "redirect:/homepage";
    }
}
