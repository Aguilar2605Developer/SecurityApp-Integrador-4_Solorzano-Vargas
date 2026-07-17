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
import org.mockito.kotlin.*
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class UserServiceTest {

    @Mock
    private lateinit var userRepository: UserRepository

    @Mock
    private lateinit var userMapper: UserMapper

    @InjectMocks
    private lateinit var userService: UserService

    private lateinit var sampleEntity: Users
    private lateinit var sampleResponse: UserResponse

    @BeforeEach
    fun setUp() {
        sampleEntity = Users(
            id = 1L,
            cognitoSub = "sub-abc-123",
            name = "Juan Perez",
            email = "juanperez@example.com",
            number = "0999999999"
        )

        sampleResponse = UserResponse(
            id = 1L,
            name = "Juan Perez",
            email = "juanperez@example.com",
            number = "0999999999"
        )
    }

    // ---------------------- findOrCreateByCognitoSub ----------------------

    @Test
    fun `findOrCreateByCognitoSub deberia devolver el usuario existente si el sub ya esta registrado`() {
        whenever(userRepository.findByCognitoSub("sub-abc-123")).thenReturn(sampleEntity)
        whenever(userMapper.toResponse(sampleEntity)).thenReturn(sampleResponse)

        val result = userService.findOrCreateByCognitoSub("sub-abc-123", "juanperez@example.com", "Juan Perez")

        assertEquals(sampleResponse.id, result.id)
        verify(userRepository, never()).save(any<Users>())
    }

    @Test
    fun `findOrCreateByCognitoSub deberia crear el usuario si el sub no existe`() {
        whenever(userRepository.findByCognitoSub("sub-nuevo")).thenReturn(null)
        whenever(userRepository.save(any<Users>())).thenAnswer { it.arguments[0] as Users }
        whenever(userMapper.toResponse(any<Users>())).thenReturn(sampleResponse)

        val result = userService.findOrCreateByCognitoSub("sub-nuevo", "nuevo@example.com", "Nuevo Usuario")

        assertNotNull(result)
        verify(userRepository, times(1)).save(
            argThat { user -> user.cognitoSub == "sub-nuevo" && user.email == "nuevo@example.com" }
        )
    }

    // ---------------------- getAllUsers ----------------------

    @Test
    fun `getAllUsers deberia devolver la lista de usuarios mapeados`() {
        val secondEntity = Users(id = 2L, cognitoSub = "sub-2", name = "Maria Lopez", email = "marialopez@example.com", number = "0988888888")
        val secondResponse = UserResponse(id = 2L, name = "Maria Lopez", email = "marialopez@example.com", number = "0988888888")

        whenever(userRepository.findAll()).thenReturn(listOf(sampleEntity, secondEntity))
        whenever(userMapper.toResponse(sampleEntity)).thenReturn(sampleResponse)
        whenever(userMapper.toResponse(secondEntity)).thenReturn(secondResponse)

        val result = userService.getAllUsers()

        assertEquals(2, result.size)
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
    fun `updateUser deberia actualizar nombre y numero preservando cognitoSub y email`() {
        val request = UserRequest(name = "Juan Actualizado", number = "0977777777")

        whenever(userRepository.findById(1L)).thenReturn(Optional.of(sampleEntity))
        whenever(userRepository.save(any<Users>())).thenAnswer { it.arguments[0] as Users }
        whenever(userMapper.toResponse(any<Users>())).thenReturn(sampleResponse)

        val result = userService.updateUser(1L, request)

        assertNotNull(result)
        verify(userRepository).save(
            argThat { user -> user.cognitoSub == sampleEntity.cognitoSub && user.email == sampleEntity.email }
        )
    }

    @Test
    fun `updateUser deberia devolver null cuando el usuario no existe`() {
        whenever(userRepository.findById(99L)).thenReturn(Optional.empty())

        val result = userService.updateUser(99L, UserRequest(name = "X", number = "000"))

        assertNull(result)
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

    // ---------------------- resolveLocalId ----------------------

    @Test
    fun `resolveLocalId deberia devolver el id local cuando el sub existe`() {
        whenever(userRepository.findByCognitoSub("sub-abc-123")).thenReturn(sampleEntity)

        val result = userService.resolveLocalId("sub-abc-123")

        assertEquals(1L, result)
    }

    @Test
    fun `resolveLocalId deberia devolver null cuando el sub es null`() {
        val result = userService.resolveLocalId(null)

        assertNull(result)
    }
}