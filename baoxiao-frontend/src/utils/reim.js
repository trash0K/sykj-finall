import { cities } from '../mock/data'

export const STATUS_MAP = { '0': '草稿', '1': '已完成', '2': '已作废' }

export function buildTree(list) {
  const map = new Map()
  const roots = []
  list.forEach(item => map.set(item.businessTypeId, { ...item, children: [] }))
  list.forEach(item => {
    const node = map.get(item.businessTypeId)
    if (item.superiorId === 'none') roots.push(node)
    else {
      const p = map.get(item.superiorId)
      if (p) p.children.push(node)
    }
  })
  return roots
}

export function findTreeNode(nodes, id) {
  for (const n of nodes) {
    if (n.businessTypeId === id) return n
    if (n.children?.length) {
      const r = findTreeNode(n.children, id)
      if (r) return r
    }
  }
  return null
}

export function getWeekDay(d) {
  return ['星期日', '星期一', '星期二', '星期三', '星期四', '星期五', '星期六'][new Date(d).getDay()]
}

export function daysBetween(start, end) {
  return Math.floor((new Date(end) - new Date(start)) / 86400000) + 1
}

export function dateRange(start, end) {
  const arr = []
  const cur = new Date(start)
  const last = new Date(end)
  while (cur <= last) {
    arr.push(cur.toISOString().split('T')[0])
    cur.setDate(cur.getDate() + 1)
  }
  return arr
}

/** 餐补标准：一线100/二线80/三线50；交通、通讯各40 */
export function subsidyRates(cityNo) {
  const c = cities.find(x => x.cityNo === cityNo)
  const t = c?.cityType || '3'
  const meal = { '1': 100, '2': 80, '3': 50 }[t] || 50
  return { meal, transport: 40, communication: 40 }
}

export function formatAmount(v) {
  const n = Number(v)
  return Number.isFinite(n) ? n.toFixed(2) : '0.00'
}

export function toPercent(ratio) {
  return ratio ? (Number(ratio) * 100).toFixed(2) : '0.00'
}

export function todayStr() {
  return new Date().toISOString().split('T')[0]
}
