import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { ToastService } from '../services/toast.service';
import { ProblemDetail } from '../models/problem-detail.model';

export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const toastService = inject(ToastService);

  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      let title = 'Error en el Servidor';
      let detail = error.message || 'Ha ocurrido un error inesperado';
      let validationErrors: Record<string, string> | undefined;

      if (error.error && typeof error.error === 'object') {
        const problem = error.error as ProblemDetail;
        if (problem.title) {
          title = problem.title;
        } else if (error.status === 422) {
          title = 'Violación de Invariante de Dominio';
        } else if (error.status === 400) {
          title = 'Solicitud Inválida';
        } else if (error.status === 404) {
          title = 'Recurso No Encontrado';
        }

        if (problem.detail) {
          detail = problem.detail;
        }

        if (problem.errors && typeof problem.errors === 'object') {
          validationErrors = problem.errors;
        }
      } else if (error.status === 0) {
        title = 'Sin Conexión con Backend';
        detail = 'No se pudo contactar el servidor en http://localhost:8080. Verifica que el backend de Spring Boot esté activo.';
      }

      toastService.error(title, detail, validationErrors);
      return throwError(() => error);
    })
  );
};
