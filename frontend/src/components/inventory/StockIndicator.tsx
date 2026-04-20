import { Progress, Tooltip } from 'antd'

interface Props {
  available: number
  reserved: number
}

export function StockIndicator({ available, reserved }: Props) {
  const total = available + reserved
  const percent = total === 0 ? 0 : Math.round((available / total) * 100)
  const strokeColor = available === 0 ? '#ff4d4f' : available <= 10 ? '#faad14' : '#52c41a'

  return (
    <Tooltip title={`${available} disponível / ${reserved} reservado`}>
      <Progress percent={percent} strokeColor={strokeColor} size="small" showInfo={false} />
    </Tooltip>
  )
}
