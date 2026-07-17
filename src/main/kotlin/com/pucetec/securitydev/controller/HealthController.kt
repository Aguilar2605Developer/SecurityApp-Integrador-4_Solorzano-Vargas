package com.pucetec.securitydev.controller

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/health")
@CrossOrigin(origins = ["*"])
class HealthController {

    @GetMapping
    fun health(): ResponseEntity<Map<String, String>> {
        return ResponseEntity.ok(
            mapOf(
                "status" to "UP",
                "service" to "SecurityApp Backend",
                "timestamp" to System.currentTimeMillis().toString()
            )
        )
    }

    @GetMapping("/cognito")
    fun cognito(): ResponseEntity<Map<String, Any>> {
        val userPoolId = System.getenv("COGNITO_USER_POOL_ID") ?: "NOT_SET"
        return ResponseEntity.ok(
            mapOf(
                "status" to "OK",
                "cognitoPoolId" to userPoolId,
                "hasAwsCredentials" to (System.getenv("AWS_ACCESS_KEY_ID") != null)
            )
        )
    }
}
