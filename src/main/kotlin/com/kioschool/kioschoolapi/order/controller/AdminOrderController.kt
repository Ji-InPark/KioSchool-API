package com.kioschool.kioschoolapi.order.controller

import com.kioschool.kioschoolapi.order.dto.CancelOrderRequestBody
import com.kioschool.kioschoolapi.order.entity.Order
import com.kioschool.kioschoolapi.order.service.OrderService
import com.kioschool.kioschoolapi.security.CustomUserDetails
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/admin")
class AdminOrderController(
    private val orderService: OrderService
) {
    @GetMapping("/orders")
    fun getAllOrders(
        authentication: Authentication,
        @RequestParam("workspaceId") workspaceId: Long
    ): List<Order> {
        val username = (authentication.principal as CustomUserDetails).username
        return orderService.getAllOrders(username, workspaceId)
    }

    @GetMapping("/orders")
    fun getOrdersByCondition(
        authentication: Authentication,
        @RequestParam("workspaceId") workspaceId: Long,
        @RequestParam("startDate") startDate: String? = null,
        @RequestParam("endDate") endDate: String? = null,
        @RequestParam("status") status: String? = null
    ): List<Order> {
        val username = (authentication.principal as CustomUserDetails).username
        return orderService.getAllOrdersByCondition(
            username,
            workspaceId,
            startDate,
            endDate,
            status
        )
    }

    @PostMapping("/order/cancel")
    fun cancelOrder(
        authentication: Authentication,
        @RequestBody body: CancelOrderRequestBody
    ): Order {
        val username = (authentication.principal as CustomUserDetails).username
        return orderService.cancelOrder(username, body.workspaceId, body.orderId)
    }

    @PostMapping("/order/serve")
    fun serveOrder(
        authentication: Authentication,
        @RequestBody body: CancelOrderRequestBody
    ): Order {
        val username = (authentication.principal as CustomUserDetails).username
        return orderService.serveOrder(username, body.workspaceId, body.orderId)
    }
}