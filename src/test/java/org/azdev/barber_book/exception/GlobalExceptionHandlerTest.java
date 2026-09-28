package org.azdev.barber_book.exception;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.azdev.barber_book.dtos.ApiErrorResponse;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    private HttpServletRequest request(String uri) {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn(uri);
        return request;
    }

    @Test
    void handleBadRequestReturns400() {
        ResponseEntity<ApiErrorResponse> response = handler.handleBadRequest(
                new BadRequestException("Requisição inválida."), request("/api/v1/services"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().message()).isEqualTo("Requisição inválida.");
    }

    @Test
    void handleMissingParamReturns400WithParameterName() {
        MissingServletRequestParameterException ex =
                new MissingServletRequestParameterException("date", "LocalDate");

        ResponseEntity<ApiErrorResponse> response = handler.handleMissingParam(ex, request("/api/v1/services"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().message()).contains("date");
    }

    @Test
    void handleMethodArgumentNotValidReturns400WithFieldErrors() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "target");
        bindingResult.addError(new FieldError("target", "name", "não pode ficar em branco"));
        when(ex.getBindingResult()).thenReturn(bindingResult);

        ResponseEntity<ApiErrorResponse> response = handler.handleMethodArgumentNotValid(ex, request("/api/v1/services"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().message()).contains("name");
    }

    @Test
    void handleConstraintViolationReturns400() {
        ConstraintViolationException ex = new ConstraintViolationException("inválido", Set.of());

        ResponseEntity<ApiErrorResponse> response = handler.handleConstraintViolation(ex, request("/api/v1/services"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void handleNotFoundReturns404ForCustomException() {
        ResponseEntity<ApiErrorResponse> response = handler.handleNotFound(
                new NotFoundException("Não encontrado."), request("/api/v1/services/1"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void handleNotFoundReturns404ForEntityNotFoundException() {
        ResponseEntity<ApiErrorResponse> response = handler.handleNotFound(
                new EntityNotFoundException("Não encontrado."), request("/api/v1/services/1"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void handleConflictReturns409() {
        ResponseEntity<ApiErrorResponse> response = handler.handleConflict(
                new ConflictException("Conflito."), request("/api/v1/appointments"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void handleDataIntegrityViolationReturnsGenericMessageByDefault() {
        DataIntegrityViolationException ex = new DataIntegrityViolationException("violation", new RuntimeException("other"));

        ResponseEntity<ApiErrorResponse> response = handler.handleDataIntegrityViolation(ex, request("/api/v1/appointments"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().message()).isEqualTo("Violação de integridade de dados.");
    }

    @Test
    void handleDataIntegrityViolationReturnsFriendlyMessageForOverlapConstraint() {
        DataIntegrityViolationException ex = new DataIntegrityViolationException(
                "violation", new RuntimeException("appointments_no_overlap constraint violated"));

        ResponseEntity<ApiErrorResponse> response = handler.handleDataIntegrityViolation(ex, request("/api/v1/appointments"));

        assertThat(response.getBody().message()).contains("Ops! Este horário");
    }

    @Test
    void handleSecurityReturns403ForUnauthorizedException() {
        ResponseEntity<ApiErrorResponse> response = handler.handleSecurity(
                new UnauthorizedException("Acesso negado."), request("/api/v1/services"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void handleSecurityReturns403ForSecurityException() {
        ResponseEntity<ApiErrorResponse> response = handler.handleSecurity(
                new SecurityException("Acesso negado."), request("/api/v1/services"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void handleSecurityReturns403ForAccessDeniedException() {
        ResponseEntity<ApiErrorResponse> response = handler.handleSecurity(
                new AccessDeniedException("Acesso negado."), request("/api/v1/services"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void handleMalformedRequestBodyReturns400() {
        HttpMessageNotReadableException ex = mock(HttpMessageNotReadableException.class);

        ResponseEntity<ApiErrorResponse> response = handler.handleMalformedRequestBody(ex, request("/api/v1/services"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void handleTypeMismatchReturns400WithParameterName() {
        MethodArgumentTypeMismatchException ex = mock(MethodArgumentTypeMismatchException.class);
        when(ex.getName()).thenReturn("id");

        ResponseEntity<ApiErrorResponse> response = handler.handleTypeMismatch(ex, request("/api/v1/services/abc"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().message()).contains("id");
    }

    @Test
    void handleGenericExceptionReturns500() {
        HttpServletRequest req = request("/api/v1/services");
        when(req.getMethod()).thenReturn("GET");

        ResponseEntity<ApiErrorResponse> response = handler.handleGenericException(new RuntimeException("boom"), req);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    void projectBadCredentialsExceptionMapsToUnauthorized() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/v1/auth/login");

        ResponseEntity<ApiErrorResponse> response = handler.handleBadCredentials(
                new BadCredentialsException("Usuário não encontrado."), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("Credenciais inválidas.");
    }

    @Test
    void springBadCredentialsExceptionMapsToUnauthorized() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/v1/auth/login");

        ResponseEntity<ApiErrorResponse> response = handler.handleBadCredentials(
                new org.springframework.security.authentication.BadCredentialsException("bad creds"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
