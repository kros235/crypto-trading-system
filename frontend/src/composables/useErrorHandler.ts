import { ref } from 'vue'
import { getErrorMessage } from '@/types/error'

export interface AppError {
  code: string
  message: string
  detail?: string
  fieldErrors?: Record<string, string>
  status?: number
}

export function useErrorHandler() {
  const error = ref<AppError | null>(null)
  const isError = ref(false)

  // 에러 설정
  const setError = (err: AppError | unknown) => {
    if (err && typeof err === 'object' && 'code' in err) {
      error.value = err as AppError
    } else if (err instanceof Error) {
      error.value = {
        code: 'UNKNOWN',
        message: err.message
      }
    } else {
      error.value = {
        code: 'UNKNOWN',
        message: '알 수 없는 오류가 발생했습니다.'
      }
    }
    isError.value = true
  }

  // 에러 초기화
  const clearError = () => {
    error.value = null
    isError.value = false
  }

  // API 호출 래퍼 (에러 자동 처리)
  const handleAsync = async <T>(
    asyncFn: () => Promise<T>,
    options?: {
      onError?: (err: AppError) => void
      showFieldErrors?: boolean
    }
  ): Promise<T | null> => {
    clearError()
    try {
      return await asyncFn()
    } catch (err) {
      setError(err)
      if (options?.onError && error.value) {
        options.onError(error.value)
      }
      return null
    }
  }

  // 필드 에러 메시지 조회
  const getFieldError = (field: string): string | undefined => {
    return error.value?.fieldErrors?.[field]
  }

  // 에러 메시지 조회
  const getErrorText = (): string => {
    if (!error.value) return ''
    return error.value.detail 
      ? `${error.value.message} - ${error.value.detail}`
      : error.value.message
  }

  return {
    error,
    isError,
    setError,
    clearError,
    handleAsync,
    getFieldError,
    getErrorText
  }
}