import { Alert } from 'antd'
import type { AxiosError } from 'axios'

interface Props {
  error: unknown
}

export function QueryErrorAlert({ error }: Props) {
  const message =
    (error as AxiosError<{ message?: string }>)?.response?.data?.message ??
    (error as Error)?.message ??
    'Ocorreu um erro inesperado.'

  return <Alert type="error" message="Erro" description={message} showIcon />
}
