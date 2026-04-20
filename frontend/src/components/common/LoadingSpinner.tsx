import { Flex, Spin } from 'antd'

export function LoadingSpinner() {
  return (
    <Flex justify="center" align="center" style={{ minHeight: 200 }}>
      <Spin size="large" />
    </Flex>
  )
}
