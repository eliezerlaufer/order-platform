import { Layout, Flex, Button, Typography } from 'antd'
import { LogoutOutlined } from '@ant-design/icons'
import { useAuth } from '@/auth/useAuth'

const { Header } = Layout

export function Topbar() {
  const { username, logout } = useAuth()

  return (
    <Header style={{ background: '#fff', padding: '0 24px', borderBottom: '1px solid #f0f0f0' }}>
      <Flex justify="space-between" align="center" style={{ height: '100%' }}>
        <Typography.Text strong>Order Platform</Typography.Text>
        <Flex align="center" gap={12}>
          <Typography.Text type="secondary">{username}</Typography.Text>
          <Button icon={<LogoutOutlined />} onClick={logout} size="small">
            Sair
          </Button>
        </Flex>
      </Flex>
    </Header>
  )
}
