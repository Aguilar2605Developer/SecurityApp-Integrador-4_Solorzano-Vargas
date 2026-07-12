package com.pucetec.securitydev.service

import com.pucetec.securitydev.dto.UserRequest
import com.pucetec.securitydev.dto.UserResponse
import com.pucetec.securitydev.entity.Users
import com.pucetec.securitydev.mappers.UserMapper
import com.pucetec.securitydev.repository.UserRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.security.crypto.password.PasswordEncoder
import java.util.Optional

/**
 * Clases confirmadas contra el código real del proyecto:
 *  - dto.UserRequest (data class): name, email, number, password
 *  - dto.UserResponse (data class): id, name, email, number
 *  - entity.Users: id: Long, name: String, email: String, number: String, password: String,
 *                  hotSpots: MutableList<HotSpot>
 *  - mappers.UserMapper: toEntity(request: UserRequest, id: Long = 0L): Users
 *                        toResponse(user: Users): UserResponse
 *  - repository.UserRepository: JpaRepository<Users, Long> estándar
 *                        (findAll, findById, existsById, save, deleteById heredados)
 */
@ExtendWith(MockitoExtension::class)
class UserServiceTest {

    @Mock
    private lateinit var userRepository: UserRepository

    @Mock
    private lateinit var userMapper: UserMapper

    @Mock
    private lateinit var passwordEncoder: PasswordEncoder

    @InjectMocks
    private lateinit var userService: UserService

    private lateinit var sampleRequest: UserRequest
    private lateinit var sampleEntity: Users
    private lateinit var sampleResponse: UserResponse

    @BeforeEach
    fun setUp() {
        sampleRequest = UserRequest(
            name = "Juan Perez",
            email = "juanperez@example.com",
            number = "0999999999",
            password = "plain-password"
        )

        sampleEntity = Users(
            id = 1L,
            name = "Juan Perez",
            email = "juanperez@example.com",
            number = "0999999999",
            password = "encoded-password"
        )

        sampleResponse = UserResponse(
            id = 1L,
            name = "Juan Perez",
            email = "juanperez@example.com",
            number = "0999999999"
        )
    }

    // ---------------------- registerUser ----------------------

    @Test
    fun `registerUser deberia encriptar el password, guardar el usuario y devolver el response`() {
        val encodedRequest = sampleRequest.copy(password = "encoded-password")

        whenever(passwordEncoder.encode("plain-password")).thenReturn("encoded-password")
        whenever(userMapper.toEntity(encodedRequest)).thenReturn(sampleEntity)
        whenever(userRepository.save(sampleEntity)).thenReturn(sampleEntity)
        whenever(userMapper.toResponse(sampleEntity)).thenReturn(sampleResponse)

        val result = userService.registerUser(sampleRequest)

        assertEquals(sampleResponse.id, result.id)
        assertEquals(sampleResponse.email, result.email)
        verify(passwordEncoder, times(1)).encode("plain-password")
        verify(userRepository, times(1)).save(sampleEntity)
    }

    // ---------------------- getAllUsers ----------------------

    @Test
    fun `getAllUsers deberia devolver la lista de usuarios mapeados`() {
        val secondEntity = Users(
            id = 2L,
            name = "Maria Lopez",
            email = "marialopez@example.com",
            number = "0988888888",
            password = "encoded-password-2"
        )
        val secondResponse = UserResponse(
            id = 2L,
            name = "Maria Lopez",
            email = "marialopez@example.com",
            number = "0988888888"
        )

        whenever(userRepository.findAll()).thenReturn(listOf(sampleEntity, secondEntity))
        whenever(userMapper.toResponse(sampleEntity)).thenReturn(sampleResponse)
        whenever(userMapper.toResponse(secondEntity)).thenReturn(secondResponse)

        val result = userService.getAllUsers()

        assertEquals(2, result.size)
        assertEquals(sampleResponse.email, result[0].email)
        assertEquals(secondResponse.email, result[1].email)
        verify(userRepository, times(1)).findAll()
    }

    @Test
    fun `getAllUsers deberia devolver lista vacia cuando no hay usuarios`() {
        whenever(userRepository.findAll()).thenReturn(emptyList())

        val result = userService.getAllUsers()

        assertTrue(result.isEmpty())
        verify(userMapper, never()).toResponse(any<Users>())
    }

    // ---------------------- getUserById ----------------------

    @Test
    fun `getUserById deberia devolver el usuario cuando existe`() {
        whenever(userRepository.findById(1L)).thenReturn(Optional.of(sampleEntity))
        whenever(userMapper.toResponse(sampleEntity)).thenReturn(sampleResponse)

        val result = userService.getUserById(1L)

        assertNotNull(result)
        assertEquals(sampleResponse.id, result?.id)
        verify(userRepository, times(1)).findById(1L)
    }

    @Test
    fun `getUserById deberia devolver null cuando el usuario no existe`() {
        whenever(userRepository.findById(99L)).thenReturn(Optional.empty())

        val result = userService.getUserById(99L)

        assertNull(result)
        verify(userMapper, never()).toResponse(any<Users>())
    }

    // ---------------------- updateUser ----------------------

    @Test
    fun `updateUser deberia actualizar y devolver el usuario cuando existe`() {
        val encodedRequest = sampleRequest.copy(password = "encoded-password")

        whenever(userRepository.existsById(1L)).thenReturn(true)
        whenever(passwordEncoder.encode("plain-password")).thenReturn("encoded-password")
        whenever(userMapper.toEntity(encodedRequest, 1L)).thenReturn(sampleEntity)
        whenever(userRepository.save(sampleEntity)).thenReturn(sampleEntity)
        whenever(userMapper.toResponse(sampleEntity)).thenReturn(sampleResponse)

        val result = userService.updateUser(1L, sampleRequest)

        assertNotNull(result)
        assertEquals(sampleResponse.id, result?.id)
        verify(userRepository, times(1)).existsById(1L)
        verify(userRepository, times(1)).save(sampleEntity)
    }

    @Test
    fun `updateUser deberia devolver null cuando el usuario no existe`() {
        whenever(userRepository.existsById(99L)).thenReturn(false)

        val result = userService.updateUser(99L, sampleRequest)

        assertNull(result)
        verify(passwordEncoder, never()).encode(any<String>())
        verify(userRepository, never()).save(any<Users>())
    }

    // ---------------------- deleteUser ----------------------

    @Test
    fun `deleteUser deberia eliminar el usuario y devolver true cuando existe`() {
        whenever(userRepository.existsById(1L)).thenReturn(true)

        val result = userService.deleteUser(1L)

        assertTrue(result)
        verify(userRepository, times(1)).deleteById(1L)
    }

    @Test
    fun `deleteUser deberia devolver false cuando el usuario no existe`() {
        whenever(userRepository.existsById(99L)).thenReturn(false)

        val result = userService.deleteUser(99L)

        assertFalse(result)
        verify(userRepository, never()).deleteById(any<Long>())
    }
}