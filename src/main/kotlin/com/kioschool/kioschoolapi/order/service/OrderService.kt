package com.kioschool.kioschoolapi.order.service

import com.kioschool.kioschoolapi.common.enums.OrderStatus
import com.kioschool.kioschoolapi.order.dto.OrderProductRequestBody
import com.kioschool.kioschoolapi.order.entity.Order
import com.kioschool.kioschoolapi.order.entity.OrderProduct
import com.kioschool.kioschoolapi.order.repository.CustomOrderRepository
import com.kioschool.kioschoolapi.order.repository.OrderRepository
import com.kioschool.kioschoolapi.product.service.ProductService
import com.kioschool.kioschoolapi.websocket.dto.Message
import com.kioschool.kioschoolapi.websocket.service.WebsocketService
import com.kioschool.kioschoolapi.workspace.exception.WorkspaceInaccessibleException
import com.kioschool.kioschoolapi.workspace.service.WorkspaceService
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Service
class OrderService(
    private val orderRepository: OrderRepository,
    private val workspaceService: WorkspaceService,
    private val productService: ProductService,
    private val websocketService: WebsocketService,
    private val customOrderRepository: CustomOrderRepository
) {
    fun getAllOrders(username: String, workspaceId: Long): List<Order> {
        val workspace = workspaceService.getWorkspace(workspaceId)
        if (workspace.owner.loginId != username) throw WorkspaceInaccessibleException()

        return orderRepository.findAllByWorkspaceId(workspaceId)
    }

    fun createOrder(
        workspaceId: Long,
        tableNumber: Int,
        customerName: String,
        rawOrderProducts: List<OrderProductRequestBody>
    ): Order {
        val workspace = workspaceService.getWorkspace(workspaceId)
        val order = orderRepository.save(
            Order(
                workspace = workspace,
                tableNumber = tableNumber,
                customerName = customerName
            )
        )
        val productMap = productService.getAllProductsByCondition(workspaceId).associateBy { it.id }
        val orderProducts = rawOrderProducts.filter { productMap.containsKey(it.productId) }.map {
            val product = productMap[it.productId]!!
            OrderProduct(
                order = order,
                product = product,
                quantity = it.quantity,
                totalPrice = product.price * it.quantity
            )
        }

        order.orderProducts.addAll(orderProducts)
        order.totalPrice = orderProducts.sumOf { it.totalPrice }
        return saveOrderAndSendWebsocketMessage(order)
    }

    fun cancelOrder(username: String, workspaceId: Long, orderId: Long): Order {
        val workspace = workspaceService.getWorkspace(workspaceId)
        if (workspace.owner.loginId != username) throw WorkspaceInaccessibleException()

        val order = orderRepository.findById(orderId).get()
        order.status = OrderStatus.CANCELLED
        return orderRepository.save(order)
    }

    fun serveOrder(username: String, workspaceId: Long, orderId: Long): Order {
        val workspace = workspaceService.getWorkspace(workspaceId)
        if (workspace.owner.loginId != username) throw WorkspaceInaccessibleException()

        val order = orderRepository.findById(orderId).get()
        order.status = OrderStatus.SERVED
        return orderRepository.save(order)
    }

    private fun saveOrderAndSendWebsocketMessage(order: Order): Order {
        val savedOrder = orderRepository.save(order)
        websocketService.sendMessage(
            "/sub/order/${order.workspace.id}",
            Message("CREATE", savedOrder)
        )
        return savedOrder
    }

    fun getAllOrdersByCondition(
        username: String,
        workspaceId: Long,
        startDate: String?,
        endDate: String?,
        status: String?
    ): List<Order> {
        val workspace = workspaceService.getWorkspace(workspaceId)
        if (workspace.owner.loginId != username) throw WorkspaceInaccessibleException()

        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val parsedStartDate = startDate?.let { LocalDate.parse(it, formatter).atStartOfDay() }
        val parsedEndDate = endDate?.let { LocalDate.parse(it, formatter).atTime(LocalTime.MAX) }
        val parsedStatus = status?.let { OrderStatus.valueOf(it) }

        return customOrderRepository.findAllByCondition(
            workspaceId,
            parsedStartDate,
            parsedEndDate,
            parsedStatus
        )
    }

    fun payOrder(username: String, workspaceId: Long, orderId: Long): Order {
        val workspace = workspaceService.getWorkspace(workspaceId)
        if (workspace.owner.loginId != username) throw WorkspaceInaccessibleException()

        val order = orderRepository.findById(orderId).get()
        order.status = OrderStatus.PAID
        return orderRepository.save(order)
    }

    fun getOrder(orderId: Long): Order {
        return orderRepository.findById(orderId).get()
    }

    fun getRealtimeOrders(username: String, workspaceId: Long): List<Order> {
        val workspace = workspaceService.getWorkspace(workspaceId)
        if (workspace.owner.loginId != username) throw WorkspaceInaccessibleException()

        val startDate = LocalDateTime.now().minusHours(12)
        val endDate = LocalDateTime.now()

        return customOrderRepository.findAllByCondition(workspaceId, startDate, endDate, null)

    }
}