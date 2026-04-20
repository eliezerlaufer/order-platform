import { Menu } from 'antd'
import { useNavigate, useLocation } from 'react-router-dom'
import { DashboardOutlined, PlusOutlined, AppstoreOutlined } from '@ant-design/icons'

const items = [
  { key: '/', icon: <DashboardOutlined />, label: 'Dashboard' },
  { key: '/orders/new', icon: <PlusOutlined />, label: 'Novo pedido' },
  { key: '/inventory', icon: <AppstoreOutlined />, label: 'Inventário' },
]

export function Sidebar() {
  const navigate = useNavigate()
  const { pathname } = useLocation()

  return (
    <Menu
      mode="inline"
      selectedKeys={[pathname]}
      items={items}
      onClick={({ key }) => navigate(key)}
      style={{ height: '100%', borderRight: 0 }}
    />
  )
}
