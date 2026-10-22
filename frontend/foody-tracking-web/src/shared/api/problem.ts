import { isAxiosError } from 'axios'

const API_CODES = [
  'VALIDATION_ERROR',
  'AUTH_INVALID_CREDENTIALS',
  'AUTH_TOKEN_INVALID',
  'ACCESS_DENIED',
  'ORDER_NOT_FOUND',
  'USER_EMAIL_ALREADY_EXISTS',
  'ORDER_INVALID_TRANSITION',
  'CONCURRENT_MODIFICATION',
  'INTERNAL_ERROR',
  'NOT_FOUND',
  'METHOD_NOT_ALLOWED',
  'NOT_ACCEPTABLE',
  'UNSUPPORTED_MEDIA_TYPE',
] as const

export type ApiErrorCode = (typeof API_CODES)[number] | 'NETWORK_ERROR' | 'TIMEOUT' | 'UNKNOWN'

export interface ApiError {
  /** Null when no response arrived. */
  status: number | null
  code: ApiErrorCode
  message: string
  fieldErrors: Record<string, string>
}

interface ProblemDetail {
  status: number
  detail: string
  code?: string
  errors?: { field: string; message: string }[]
}

// Older or framework-built responses may lack `code`; the status still says what happened.
const CODE_BY_STATUS: Partial<Record<number, ApiErrorCode>> = {
  400: 'VALIDATION_ERROR',
  401: 'AUTH_TOKEN_INVALID',
  403: 'ACCESS_DENIED',
  404: 'NOT_FOUND',
  405: 'METHOD_NOT_ALLOWED',
  406: 'NOT_ACCEPTABLE',
  415: 'UNSUPPORTED_MEDIA_TYPE',
}

const FALLBACK_MESSAGE = {
  NETWORK_ERROR: 'Não foi possível conectar ao servidor. Verifique sua conexão e tente novamente.',
  TIMEOUT: 'O servidor demorou para responder. Tente novamente.',
  SERVER: 'Erro interno. Tente novamente mais tarde.',
  UNKNOWN: 'Algo deu errado. Tente novamente.',
}

function isApiCode(code: unknown): code is ApiErrorCode {
  return API_CODES.some((known) => known === code)
}

function isProblemDetail(data: unknown): data is ProblemDetail {
  return (
    typeof data === 'object' && data !== null && typeof Reflect.get(data, 'detail') === 'string'
  )
}

// API paths use brackets (`items[0].unitPrice`), react-hook-form dots; several messages are joined.
function toFieldErrors(errors: ProblemDetail['errors'] = []): Record<string, string> {
  const result: Record<string, string> = {}
  for (const { field, message } of errors) {
    const path = field.replace(/\[(\d+)\]/g, '.$1')
    result[path] = result[path] ? `${result[path]}; ${message}` : message
  }
  return result
}

export function toApiError(error: unknown): ApiError {
  if (!isAxiosError(error)) {
    return { status: null, code: 'UNKNOWN', message: FALLBACK_MESSAGE.UNKNOWN, fieldErrors: {} }
  }
  if (!error.response) {
    const code =
      error.code === 'ECONNABORTED' || error.code === 'ETIMEDOUT' ? 'TIMEOUT' : 'NETWORK_ERROR'
    return { status: null, code, message: FALLBACK_MESSAGE[code], fieldErrors: {} }
  }

  const { status, data } = error.response
  const problem = isProblemDetail(data) ? data : undefined
  const serverError = status >= 500
  const code: ApiErrorCode = isApiCode(problem?.code)
    ? problem.code
    : (CODE_BY_STATUS[status] ?? (serverError ? 'INTERNAL_ERROR' : 'UNKNOWN'))

  return {
    status,
    code,
    message: problem?.detail || (serverError ? FALLBACK_MESSAGE.SERVER : FALLBACK_MESSAGE.UNKNOWN),
    fieldErrors: toFieldErrors(problem?.errors),
  }
}
