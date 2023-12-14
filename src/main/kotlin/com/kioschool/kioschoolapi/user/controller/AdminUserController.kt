package com.kioschool.kioschoolapi.user.controller

import com.kioschool.kioschoolapi.security.CustomUserDetails
import com.kioschool.kioschoolapi.user.dto.admin.CreateSuperUserRequestBody
import com.kioschool.kioschoolapi.user.dto.admin.RegisterAccountUrlRequestBody
import com.kioschool.kioschoolapi.user.entity.User
import com.kioschool.kioschoolapi.user.service.UserService
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/admin")
class AdminUserController(
    private val userService: UserService
) {
    @GetMapping("/user")
    fun getUser(authentication: Authentication): User {
        val username = (authentication.principal as CustomUserDetails).username
        return userService.getUser(username)
    }

    @PostMapping("/super-user")
    fun createSuperUser(
        authentication: Authentication,
        @RequestBody body: CreateSuperUserRequestBody
    ): User {
        val username = (authentication.principal as CustomUserDetails).username
        return userService.createSuperUser(username, body.id)
    }

    @PostMapping("/user/toss-account")
    fun registerAccountUrl(
        authentication: Authentication,
        @RequestBody body: RegisterAccountUrlRequestBody
    ): User {
        val username = (authentication.principal as CustomUserDetails).username
        return userService.registerAccountUrl(username, body.accountUrl)
    }
}