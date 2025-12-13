import { inject } from 'vue'

export function useSnackbar() {
  const showSuccess = inject<(title: string, message?: string) => void>('showSuccess')
  const showError = inject<(title: string, message?: string) => void>('showError')
  const showWarning = inject<(title: string, message?: string) => void>('showWarning')
  const showInfo = inject<(title: string, message?: string) => void>('showInfo')

  return {
    showSuccess: showSuccess || (() => console.warn('Snackbar not provided')),
    showError: showError || (() => console.warn('Snackbar not provided')),
    showWarning: showWarning || (() => console.warn('Snackbar not provided')),
    showInfo: showInfo || (() => console.warn('Snackbar not provided'))
  }
}